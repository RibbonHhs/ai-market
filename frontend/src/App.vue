<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, onBeforeMount } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { theme as antTheme } from 'ant-design-vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const themeConfig = computed(() => {
  const isDark = themeStore.resolved === 'dark'
  return isDark
    ? {
        algorithm: antTheme.darkAlgorithm,
        token: {
          colorPrimary: '#a78bfa',
          colorBgBase:  '#15121f',
          colorTextBase: 'rgba(255,255,255,0.92)',
          colorLink:     '#a78bfa'
        }
      }
    : {
        algorithm: antTheme.defaultAlgorithm,
        token: {
          colorPrimary: '#7c3aed',
          colorBgBase:  '#ffffff',
          colorTextBase: '#1a1a1f',
          colorLink:     '#7c3aed'
        }
      }
})

onBeforeMount(() => themeStore.init())
</script>
