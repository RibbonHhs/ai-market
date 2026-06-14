<template>
  <a-layout-header class="app-header">
    <div class="app-header__inner">
      <div class="app-header__left">
        <router-link to="/" class="app-header__logo">
          <img class="logo-mark" src="/logo.png" alt="SkillsMap logo" />
          <span class="logo-text">SkillsMap</span>
        </router-link>
        <a-menu
          mode="horizontal"
          :selected-keys="[currentRoute]"
          class="app-header__menu"
          @click="onMenuClick"
        >
          <a-menu-item key="home">首页</a-menu-item>
          <a-menu-item key="browse">浏览</a-menu-item>
          <a-menu-item key="occupations">职业技能</a-menu-item>
          <a-menu-item key="categories">用途分类</a-menu-item>
        </a-menu>
      </div>

      <div class="app-header__center">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索 Skills..."
          enter-button
          @search="onSearch"
          class="app-header__search"
        />
      </div>

      <div class="app-header__right">
        <a-tooltip :title="`主题: ${themeStore.currentLabel} · ${themeStore.nextLabel}`">
          <a-button
            data-testid="theme-toggle"
            class="app-header__theme"
            type="text"
            :aria-label="`当前主题: ${themeStore.currentLabel}，${themeStore.nextLabel}`"
            @click="themeStore.cycle()"
          >
            <span aria-hidden="true">{{ themeStore.icon }}</span>
          </a-button>
        </a-tooltip>
        <a-tooltip title="搜索 Ctrl+K">
          <a-button class="app-header__kbd" type="text" @click="focusSearch" aria-label="搜索">
            <SearchOutlined />
          </a-button>
        </a-tooltip>
        <template v-if="auth.isLoggedIn">
          <a-dropdown>
            <a-space class="user-trigger">
              <a-avatar :size="28">{{ auth.userInfo?.avatar || '🙂' }}</a-avatar>
              <span>{{ auth.displayName }}</span>
              <CaretDownOutlined />
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item key="upload" @click="openUploadModal" data-testid="menu-upload">
                  <CloudUploadOutlined /> 上传我的 Skill
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item v-if="auth.isAdmin" key="admin" @click="goAdmin">
                  <DashboardOutlined /> 管理后台
                </a-menu-item>
                <a-menu-item key="me" @click="goMe">
                  <UserOutlined /> 个人中心
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="onLogout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <SkillUploadModal v-model:open="uploadModalOpen" @close="uploadModalOpen = false" />
        </template>
        <template v-else>
          <a-button class="auth-btn" @click="goLogin">登录</a-button>
          <a-button class="auth-btn" type="primary" @click="goRegister">注册</a-button>
        </template>
      </div>
    </div>
  </a-layout-header>

  <a-drawer
    v-model:open="drawerOpen"
    placement="left"
    :width="280"
    title="菜单"
    :body-style="{ padding: 0 }"
  >
    <a-menu
      mode="inline"
      :selected-keys="[currentRoute]"
      @click="onDrawerMenu"
    >
      <a-menu-item key="home">首页</a-menu-item>
      <a-menu-item key="browse">浏览</a-menu-item>
      <a-menu-item key="occupations">职业技能</a-menu-item>
      <a-menu-item key="categories">用途分类</a-menu-item>
    </a-menu>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import {
  CaretDownOutlined,
  DashboardOutlined,
  LogoutOutlined,
  UserOutlined,
  MenuOutlined,
  SearchOutlined,
  CloudUploadOutlined
} from '@ant-design/icons-vue'
import SkillUploadModal from '@/components/SkillUploadModal.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const themeStore = useThemeStore()
const searchKeyword = ref('')
const drawerOpen = ref(false)
const uploadModalOpen = ref(false)

const currentRoute = computed(() => {
  const name = route.name?.toString() || ''
  if (name.startsWith('skill-detail')) return ''
  return name
})

function onMenuClick({ key }: { key: string }) {
  router.push({ name: key })
}

function onDrawerMenu({ key }: { key: string }) {
  drawerOpen.value = false
  router.push({ name: key })
}

function focusSearch() {
  // 桌面端聚焦顶部搜索框；移动端无搜索框则跳 /browse
  const el = document.querySelector<HTMLInputElement>('.app-header__search input')
  if (el) {
    el.focus()
  } else {
    router.push({ name: 'browse' })
  }
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    focusSearch()
  }
}

function onSearch() {
  if (!searchKeyword.value.trim()) return
  router.push({ name: 'browse', query: { keyword: searchKeyword.value.trim() } })
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
// 静默使用 nextTick 防止 lint 警告（保留以备未来用）
void nextTick

async function onLogout() {
  await auth.logout()
  router.push('/')
}

function goLogin() {
  router.push('/login')
}

function goRegister() {
  router.push('/register')
}

function goAdmin() {
  router.push('/admin')
}

function goMe() {
  router.push('/me')
}

function openUploadModal() {
  uploadModalOpen.value = true
}
</script>

<style scoped lang="scss">
.app-header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}
.app-header__inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 64px;
}
.app-header__left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
}
.app-header__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  text-decoration: none;
  .logo-mark {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: block;
    object-fit: contain;
  }
}
.app-header__menu {
  border-bottom: none;
  background: transparent;
  min-width: 240px;
  line-height: 32px;
  :deep(.ant-menu-item) {
    line-height: 32px;
    height: 32px;
  }
}
.app-header__center {
  flex: 1;
  max-width: 480px;
  display: flex;
  align-items: center;
}
.app-header__search {
  width: 100%;
  // 强制让 Ant Design 的 search 内部容器与 button 同高 (32px)
  :deep(.ant-input-affix-wrapper) {
    height: 32px;
    padding: 0 11px;
  }
  :deep(.ant-input) {
    height: 32px;
  }
  :deep(.ant-input-search-button) {
    height: 32px;
  }
}
.app-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  height: 32px;
  // 让两个 button 严格垂直居中
  > :deep(.ant-btn) {
    height: 32px;
    padding: 0 16px;
    line-height: 30px;
  }
}
.user-trigger {
  cursor: pointer;
  padding: 0 8px;
  border-radius: 8px;
  color: var(--text-primary);
  &:hover {
    background: var(--bg-tertiary);
  }
}
.auth-btn {
  font-weight: 500;
}
.app-header__menu-toggle {
  display: none;
  font-size: 18px;
}
.app-header__kbd {
  display: none;
  font-size: 16px;
  color: #64748b;
}

@media (max-width: 768px) {
  .app-header__menu-toggle {
    display: inline-flex;
  }
  .app-header__menu,
  .app-header__center {
    display: none;
  }
  .app-header__kbd {
    display: inline-flex;
  }
  .app-header__inner {
    gap: 12px;
  }
}
.app-header__menu-toggle {
  display: none;
  font-size: 18px;
}
.app-header__kbd {
  display: none;
  font-size: 16px;
  color: #64748b;
}

@media (max-width: 768px) {
  .app-header__menu-toggle {
    display: inline-flex;
  }
  .app-header__menu,
  .app-header__center {
    display: none;
  }
  .app-header__kbd {
    display: inline-flex;
  }
  .app-header__inner {
    gap: 12px;
  }
}
</style>
