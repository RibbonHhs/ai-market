/**
 * Auth store — 仿 dos-front-vue3 风格
 * - Options API
 * - LocalPrivateCache 持久化
 * - actions: initFromCache / login / logout / fetchUserInfo / clear
 */
import { defineStore } from 'pinia'
import ajax from '@/axios/ajax'
import { LocalPrivateCache } from '@/utils/cache'

export interface UserInfo {
  id: number
  username: string
  displayName?: string
  avatar?: string
  email?: string
  role: 'ADMIN' | 'USER'
}

interface AuthState {
  token: string | null
  userInfo: UserInfo | null
  roles: string[]
  permissions: string[]
}

const CACHE_KEY = 'auth'

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: null,
    userInfo: null,
    roles: [],
    permissions: []
  }),

  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.userInfo?.role === 'ADMIN',
    displayName: (s) => s.userInfo?.displayName || s.userInfo?.username || '游客'
  },

  actions: {
    /** 启动时从缓存恢复 */
    async initFromCache() {
      const cached = LocalPrivateCache.get<AuthState>(CACHE_KEY)
      if (cached) {
        this.token = cached.token
        this.userInfo = cached.userInfo
        this.roles = cached.roles || []
        this.permissions = cached.permissions || []
        // 后台静默拉一次最新用户信息（失败不抛）
        if (this.token) {
          this.fetchUserInfo().catch(() => {
            // token 无效则清空
            this.clear()
          })
        }
      }
    },

    persist() {
      LocalPrivateCache.set(CACHE_KEY, {
        token: this.token,
        userInfo: this.userInfo,
        roles: this.roles,
        permissions: this.permissions
      })
    },

    async loginByUsername(username: string, password: string) {
      const data = await ajax.post<{ token: string; userInfo: UserInfo; roles: string[]; permissions: string[] }>(
        '/auth/login',
        { username, password }
      )
      this.token = data.token
      this.userInfo = data.userInfo
      this.roles = data.roles || []
      this.permissions = data.permissions || []
      this.persist()
      return data
    },

    async register(username: string, password: string, email?: string, displayName?: string) {
      return ajax.post<UserInfo>('/auth/register', { username, password, email, displayName })
    },

    async fetchUserInfo() {
      const data = await ajax.get<UserInfo>('/auth/me')
      this.userInfo = data
      this.roles = data.role ? [data.role] : []
      this.persist()
      return data
    },

    async logout() {
      try {
        await ajax.post('/auth/logout')
      } catch {
        /* 静默 */
      }
      this.clear()
    },

    clear() {
      this.token = null
      this.userInfo = null
      this.roles = []
      this.permissions = []
      LocalPrivateCache.remove(CACHE_KEY)
    }
  }
})
