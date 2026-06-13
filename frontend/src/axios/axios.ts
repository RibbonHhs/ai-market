/**
 * axios 单例 — 与 dos-front-vue3 风格对齐
 * - baseURL: /api（dev 模式由 Vite proxy 反代到 8767，prod 由 nginx 反代）
 * - withCredentials: true（预留 Cookie 鉴权扩展）
 * - timeout: 15000ms
 */
import axios, { type AxiosInstance } from 'axios'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export default instance
