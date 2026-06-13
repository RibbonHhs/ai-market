<template>
  <a-layout class="admin-layout">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo">
        <span class="logo-mark">S</span>
        <span v-if="!collapsed" class="logo-text">Admin</span>
      </div>
      <a-menu
        mode="inline"
        theme="dark"
        :selected-keys="[route.path]"
        @click="onMenuClick"
      >
        <a-menu-item key="/admin/dashboard">
          <DashboardOutlined />
          <span>Dashboard</span>
        </a-menu-item>
        <a-menu-item key="/admin/skills">
          <AppstoreOutlined />
          <span>Skills</span>
        </a-menu-item>
        <a-menu-item key="/admin/categories">
          <FolderOutlined />
          <span>职业技能</span>
        </a-menu-item>
        <a-menu-item key="/admin/categories/usage">
          <TagsOutlined />
          <span>用途分类</span>
        </a-menu-item>
        <a-menu-item key="/admin/tags">
          <TagsOutlined />
          <span>标签</span>
        </a-menu-item>
        <a-menu-item key="/admin/users">
          <TeamOutlined />
          <span>用户</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="admin-header">
        <a-space>
          <a-button type="text" @click="$router.push('/')">
            <HomeOutlined /> 返回前台
          </a-button>
        </a-space>
        <a-space>
          <a-tag color="gold">👑 {{ auth.displayName }}</a-tag>
          <a-button type="text" @click="onLogout">
            <LogoutOutlined /> 退出
          </a-button>
        </a-space>
      </a-layout-header>
      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  DashboardOutlined,
  AppstoreOutlined,
  FolderOutlined,
  TagsOutlined,
  TeamOutlined,
  HomeOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

function onMenuClick({ key }: { key: string | number }) {
  router.push(String(key))
}

async function onLogout() {
  await auth.logout()
  router.push('/')
}
</script>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  .logo-mark {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: linear-gradient(135deg, #1677ff, #722ed1);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-weight: 800;
  }
}
.admin-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}
.admin-content {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}
</style>
