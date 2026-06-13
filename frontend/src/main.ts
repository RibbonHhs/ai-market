import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import 'ant-design-vue/dist/reset.css'
import 'highlight.js/styles/github.css'
import 'nprogress/nprogress.css'
import './style/global.scss'

import App from './App.vue'
import router from './router'
import { setupInterceptors } from './axios/interceptor'
import { useAuthStore } from './stores/auth'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()

  app.use(pinia)
  app.use(router)
  app.use(Antd, { locale: zhCN })

  // 注入 axios 拦截器（在 router/pinia 装好之后）
  setupInterceptors()

  // 启动时恢复登录态
  const authStore = useAuthStore()
  await authStore.initFromCache()

  app.mount('#app')
}

bootstrap().catch((err) => {
  // eslint-disable-next-line no-console
  console.error('Bootstrap failed:', err)
})
