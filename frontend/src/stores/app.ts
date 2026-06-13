/**
 * App 全局 store
 */
import { defineStore } from 'pinia'

interface AppState {
  sidebarCollapsed: boolean
  breadcrumbs: Array<{ title: string; path?: string }>
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    sidebarCollapsed: false,
    breadcrumbs: []
  }),

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    setBreadcrumbs(items: Array<{ title: string; path?: string }>) {
      this.breadcrumbs = items
    }
  }
})
