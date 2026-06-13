import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

// Vite 配置 — 与 dos-front-vue3 风格对齐：端口 7777、API 反代 /api 到后端 8767
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
      resolvers: [AntDesignVueResolver({ importStyle: false })]
    }),
    Components({
      dts: 'src/components.d.ts',
      resolvers: [AntDesignVueResolver({ importStyle: false })]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 7777,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8767',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2020',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          antd: ['ant-design-vue', '@ant-design/icons-vue'],
          editor: ['markdown-it', 'highlight.js']
        }
      }
    }
  }
})
