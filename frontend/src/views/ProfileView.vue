<template>
  <a-layout class="app-layout">
    <AppHeader />
    <a-layout-content class="app-content">
      <h2>👤 个人中心</h2>
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :xs="24" :md="8">
          <a-card>
            <a-avatar :size="64" style="font-size: 32px">
              {{ auth.userInfo?.avatar || '🙂' }}
            </a-avatar>
            <h3 style="margin: 12px 0 4px">{{ auth.userInfo?.displayName || auth.userInfo?.username }}</h3>
            <p style="color: #999; margin: 0">{{ auth.userInfo?.email || '未设置邮箱' }}</p>
            <a-tag :color="auth.isAdmin ? 'gold' : 'blue'" style="margin-top: 8px">
              {{ auth.isAdmin ? '👑 管理员' : '🙂 普通用户' }}
            </a-tag>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="16">
          <a-card title="⭐ 我的收藏">
            <a-spin :spinning="favLoading">
              <a-empty v-if="!favs.length" description="还没有收藏" />
              <a-row v-else :gutter="[12, 12]">
                <a-col v-for="s in favs" :key="s.id" :xs="24" :sm="12">
                  <SkillCard :skill="s" />
                </a-col>
              </a-row>
            </a-spin>
          </a-card>
        </a-col>
      </a-row>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import SkillCard from '@/components/SkillCard.vue'
import { useAuthStore } from '@/stores/auth'
import { favoriteApi } from '@/api/review'
import type { Skill } from '@/types/skill'

const auth = useAuthStore()
const favs = ref<Skill[]>([])
const favLoading = ref(false)

onMounted(async () => {
  favLoading.value = true
  try {
    const data = await favoriteApi.listMine(1, 50)
    favs.value = data.records
  } finally {
    favLoading.value = false
  }
})
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}
.app-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
  color: var(--text-primary);
}
</style>
