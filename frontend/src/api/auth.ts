/**
 * Auth API
 */
import ajax from '@/axios/ajax'

export interface LoginReq {
  username: string
  password: string
}
export interface LoginResp {
  token: string
  userInfo: {
    id: number
    username: string
    displayName?: string
    avatar?: string
    email?: string
    role: 'ADMIN' | 'USER'
  }
  roles: string[]
  permissions: string[]
}

export const authApi = {
  login: (data: LoginReq) => ajax.post<LoginResp>('/auth/login', data),
  register: (data: { username: string; password: string; email?: string; displayName?: string }) =>
    ajax.post<unknown>('/auth/register', data),
  me: () => ajax.get<LoginResp['userInfo']>('/auth/me'),
  logout: () => ajax.post<void>('/auth/logout')
}
