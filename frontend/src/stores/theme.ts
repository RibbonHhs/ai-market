/**
 * Theme store — 暗色手动切换 (S28)
 * - 3 态：auto / light / dark
 * - localStorage 持久化 (key=skillsmap.theme)
 * - 跨标签同步 (storage 事件)
 * - auto 态跟系统 (matchMedia change)
 */
import { defineStore } from 'pinia'

export type ThemeMode = 'auto' | 'light' | 'dark'

interface ThemeState {
  mode: ThemeMode
}

const STORAGE_KEY = 'skillsmap.theme'

function readSaved(): ThemeMode {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v === 'auto' || v === 'light' || v === 'dark') return v
  } catch {
    /* SSR / 隐私模式无 localStorage */
  }
  return 'auto'
}

function isDarkPreferred(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches
}

export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => ({ mode: 'auto' }),

  getters: {
    resolved(state): 'light' | 'dark' {
      return state.mode === 'auto' ? (isDarkPreferred() ? 'dark' : 'light') : state.mode
    },
    nextLabel(state): string {
      return state.mode === 'auto' ? '点击切换为浅色' : state.mode === 'light' ? '点击切换为暗色' : '点击切换为跟随系统'
    },
    currentLabel(state): string {
      return state.mode === 'auto' ? '跟随系统' : state.mode === 'light' ? '浅色' : '暗色'
    },
    icon(state): string {
      return state.mode === 'auto' ? '🌓' : state.mode === 'light' ? '☀' : '🌙'
    }
  },

  actions: {
    /** 在 App.vue onBeforeMount 调用一次 */
    init() {
      this.mode = readSaved()
      this.apply()
      // 跨标签
      window.addEventListener('storage', (e) => {
        if (e.key === STORAGE_KEY && e.newValue && (e.newValue === 'auto' || e.newValue === 'light' || e.newValue === 'dark')) {
          this.mode = e.newValue
          this.apply()
        }
      })
      // auto 态系统切换
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (this.mode === 'auto') this.apply()
      })
    },

    cycle() {
      this.mode = this.mode === 'auto' ? 'light' : this.mode === 'light' ? 'dark' : 'auto'
      this.apply()
    },

    setMode(m: ThemeMode) {
      this.mode = m
      this.apply()
    },

    apply() {
      const r = this.resolved
      if (this.mode === 'auto') {
        delete document.documentElement.dataset.theme
      } else {
        document.documentElement.dataset.theme = r
      }
      try {
        localStorage.setItem(STORAGE_KEY, this.mode)
      } catch {
        /* 忽略 */
      }
    }
  }
})
