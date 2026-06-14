<template>
  <a-layout class="admin-layout">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo">
        <img class="logo-mark" src="/logo_dark.png" alt="SkillsMap logo" />
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
        <a-menu-item key="/admin/skills/new">
          <CloudUploadOutlined />
          <span>上传 Skill</span>
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
  CloudUploadOutlined,
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
  // 侧栏 antdv theme="dark" 始终深色，文字保留白色是 antdv 设计语义
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  .logo-mark {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: block;
    object-fit: contain;
  }
}
.admin-header {
  // S37: 去硬编码，跟随主题切换（浅=白，深=深紫黑）
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border);
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  // Header 内的暗色按钮在暗色下需要显式提升对比度（antdv defaultAlgorithm 自动处理，但 type="text" 仍需保底）
  :deep(.ant-btn-text) {
    color: var(--text-primary);
  }
}
.admin-content {
  // S37: 去硬编码，浅=浅灰、深=深紫，建立与 body 的层级差
  padding: 24px;
  background: var(--bg-secondary);
  min-height: calc(100vh - 64px);
  // 内容区内的 page-header / card 标题靠 antdv 算法产出，自动跟随主题
  // 这里只做保底：万一 antdv 算法未覆盖，强制标题层级使用主文字色
  :deep(.ant-page-header-heading-title),
  :deep(.ant-card-head-title) {
    color: var(--text-primary);
  }
}
</style>
