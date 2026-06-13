<template>
  <a-layout class="app-layout">
    <AppHeader />
    <a-layout-content class="app-content">
      <HomeHero :top-skills="topSkills" />
      <HomeStats :total-skills="totalSkills" />
      <HomeFeatured
        :featured="featured"
        :soc-categories="socCategories"
        :total-count="featured.length"
      />
      <HomeHot ref="hotRef" :usage-categories="usageCategories" />
    </a-layout-content>
    <a-layout-footer class="app-footer">
      SkillsMap · Built with ❤️ by SkillsMap Team · MIT
    </a-layout-footer>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import HomeStats from '@/components/home/HomeStats.vue'
import HomeFeatured from '@/components/home/HomeFeatured.vue'
import HomeHot from '@/components/home/HomeHot.vue'
import { skillApi, categoryApi } from '@/api/skill'
import type { Skill, Category } from '@/types/skill'

const totalSkills = ref(0)
const topSkills = ref<Skill[]>([])
const featured = ref<Skill[]>([])
const socCategories = ref<Category[]>([])
const usageCategories = ref<Category[]>([])
const hotRef = ref<InstanceType<typeof HomeHot> | null>(null)

onMounted(async () => {
  try {
    const [listPage, featuredRes, socAll, usageAll, top] = await Promise.all([
      skillApi.list({ size: 1 }),
      skillApi.featured(20),
      categoryApi.list(),
      categoryApi.tree('USAGE'),
      skillApi.hot(6, 'hot')
    ])
    totalSkills.value = listPage.total
    featured.value = featuredRes
    topSkills.value = top
    // 兼容：list() 返回全量（含 USAGE），tree() 返回 USAGE 一级 + 二级嵌套
    socCategories.value = (socAll as Category[]).filter(
      (c) => c.type !== 'USAGE' && !c.parentId
    )
    usageCategories.value = (usageAll as Category[]).filter((c) => !c.parentId)
    // 喂 USAGE 分类给 HomeHot tab 排
    hotRef.value?.buildUsageTabs(usageCategories.value)
  } catch (e) {
    console.error('home load error', e)
  }
})
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}
.app-content {
  background: var(--bg-primary);
}
.app-footer {
  text-align: center;
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  font-size: 13px;
  padding: 24px;
  border-top: 1px solid var(--border-color);
}
</style>
