/**
 * LocalPrivateCache — localStorage 的轻量包装，仿 dos-front-vue3 风格
 * 简单 KV 存储 + 命名空间隔离
 */
const PREFIX = 'skillsmap.'

export const LocalPrivateCache = {
  get<T = unknown>(key: string): T | null {
    try {
      const raw = localStorage.getItem(PREFIX + key)
      if (raw == null) return null
      return JSON.parse(raw) as T
    } catch {
      return null
    }
  },
  set(key: string, value: unknown): void {
    try {
      localStorage.setItem(PREFIX + key, JSON.stringify(value))
    } catch {
      /* quota exceeded */
    }
  },
  remove(key: string): void {
    localStorage.removeItem(PREFIX + key)
  },
  clear(): void {
    Object.keys(localStorage)
      .filter((k) => k.startsWith(PREFIX))
      .forEach((k) => localStorage.removeItem(k))
  }
}

export const SessionPrivateCache = {
  get<T = unknown>(key: string): T | null {
    try {
      const raw = sessionStorage.getItem(PREFIX + key)
      if (raw == null) return null
      return JSON.parse(raw) as T
    } catch {
      return null
    }
  },
  set(key: string, value: unknown): void {
    try {
      sessionStorage.setItem(PREFIX + key, JSON.stringify(value))
    } catch {
      /* quota exceeded */
    }
  },
  remove(key: string): void {
    sessionStorage.removeItem(PREFIX + key)
  }
}
