/**
 * axios 拦截器
 * - 请求：自动加 Authorization: Bearer xxx
 * - 响应：code === 0 resolve；code === 401xx 清登录态跳 /login
 */
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { BizCode } from './biz-code'
import ajax from './ajax'
import { useAuthStore } from '@/stores/auth'

let initialized = false

export function setupInterceptors() {
  if (initialized) return
  initialized = true

  const instance = ajax.raw

  // 请求拦截
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const auth = useAuthStore()
      if (auth.token) {
        config.headers.set('Authorization', `Bearer ${auth.token}`)
      }
      config.headers.set('X-Req-Id', crypto.randomUUID?.() || String(Date.now()))
      return config
    },
    (err) => Promise.reject(err)
  )

  // 响应拦截
  instance.interceptors.response.use(
    (res: AxiosResponse) => {
      const body = res.data
      if (body && typeof body.code === 'number' && body.code !== 0) {
        // 业务错误
        if (body.code === BizCode.UNAUTHORIZED || body.code === BizCode.TOKEN_INVALID || body.code === BizCode.TOKEN_EXPIRED) {
          const auth = useAuthStore()
          auth.clear()
          if (window.location.pathname !== '/login') {
            message.warning('登录已过期，请重新登录')
            window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
          }
        } else if (body.code === BizCode.FORBIDDEN) {
          message.error('权限不足')
        } else if (body.code !== BizCode.NOT_FOUND) {
          // 404 不弹通用错误
          message.error(body.message || `错误 ${body.code}`)
        }
        return Promise.reject(body)
      }
      return res
    },
    (err) => {
      if (err.response?.status === 401) {
        const auth = useAuthStore()
        auth.clear()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else if (err.response?.status >= 500) {
        message.error('服务器异常，请稍后重试')
      }
      return Promise.reject(err)
    }
  )
}
