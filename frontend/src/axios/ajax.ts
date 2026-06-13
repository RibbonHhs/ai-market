/**
 * ajax 封装 — 用 path-to-regexp 编译 URL 模板
 * - 业务统一响应 { code, message, data }
 * - code === 0 resolve，否则 reject
 * - 后端分页响应 { records, total, page, size }
 */
import { compile as compilePath } from 'path-to-regexp'
import axios, { type AxiosRequestConfig, type Method } from 'axios'
import instance from './axios'

export interface BizResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageData<T> {
  records: T[]
  total: number
  page: number
  size: number
}

function buildUrl(template: string, params?: Record<string, unknown>): string {
  if (!params) return template
  const toPath = compilePath(template, { encode: encodeURIComponent })
  // 提取 query 参数
  const pathParams: Record<string, string> = {}
  const queryParams: Record<string, unknown> = {}
  const pathKeys = (template.match(/:[a-zA-Z_][a-zA-Z0-9_]*/g) || []).map((k) => k.slice(1))
  for (const [k, v] of Object.entries(params)) {
    if (pathKeys.includes(k)) {
      // path-to-regexp v8 严格类型化：pathParams 必须是 string
      pathParams[k] = v == null ? '' : String(v)
    } else {
      queryParams[k] = v
    }
  }
  let url = toPath(pathParams)
  const qs = new URLSearchParams()
  for (const [k, v] of Object.entries(queryParams)) {
    if (v == null || v === '') continue
    qs.append(k, String(v))
  }
  const s = qs.toString()
  if (s) url += (url.includes('?') ? '&' : '?') + s
  return url
}

async function request<T>(
  method: Method,
  url: string,
  data?: unknown,
  params?: Record<string, unknown>,
  config?: AxiosRequestConfig
): Promise<T> {
  const fullUrl = buildUrl(url, params)
  const res = await instance.request<BizResponse<T>>({
    method,
    url: fullUrl,
    data,
    params: undefined, // 已合并到 url 上
    ...config
  })
  const body = res.data
  if (body.code === 0) {
    return body.data
  }
  // 业务错误抛错，拦截器会处理
  const err: BizError = new Error(body.message || `BizError ${body.code}`)
  ;(err as BizError).code = body.code
  ;(err as BizError).bizResponse = body
  throw err
}

export interface BizError extends Error {
  code?: number
  bizResponse?: BizResponse<unknown>
}

export const ajax = {
  get: <T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) =>
    request<T>('GET', url, undefined, params, config),
  post: <T>(url: string, data?: unknown, params?: Record<string, unknown>, config?: AxiosRequestConfig) =>
    request<T>('POST', url, data, params, config),
  put: <T>(url: string, data?: unknown, params?: Record<string, unknown>, config?: AxiosRequestConfig) =>
    request<T>('PUT', url, data, params, config),
  del: <T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) =>
    request<T>('DELETE', url, undefined, params, config),
  // 上传文件（multipart/form-data）
  upload: <T>(url: string, formData: FormData, config?: AxiosRequestConfig) =>
    instance.post<BizResponse<T>>(buildUrl(url), formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      ...config
    }).then((res) => {
      const body = res.data
      if (body.code === 0) return body.data
      const err: BizError = new Error(body.message || `BizError ${body.code}`)
      ;(err as BizError).code = body.code
      ;(err as BizError).bizResponse = body
      throw err
    }),
  // 暴露 axios 实例，方便拦截器/特殊场景
  raw: instance,
  axios
}

export default ajax
