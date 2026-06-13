<template>
  <section class="home-hot">
    <div class="home-hot__head">
      <h2 class="home-hot__title">
        <span class="leaf">🌿</span> 热门 Skills <span class="leaf">🌿</span>
      </h2>
      <div class="home-hot__tabs">
        <button
          v-for="t in tabs"
          :key="t.key"
          type="button"
          class="home-hot__tab"
          :class="{ 'is-active': activeTab === t.key }"
          @click="onTab(t.key)"
        >
          <span v-if="t.icon" aria-hidden="true">{{ t.icon }}</span>
          <span>{{ t.label }}</span>
        </button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-empty v-if="!list.length && !loading" description="暂无数据" />
      <a-row v-else :gutter="[16, 16]">
        <a-col
          v-for="s in list"
          :key="s.id"
          :xs="24"
          :sm="12"
          :md="8"
        >
          <article class="home-hot__card" @click="goDetail(s)">
            <div class="home-hot__card-head">
              <SkillLogo :name="s.name" :size="48" />
              <div class="home-hot__card-meta">
                <h3 class="home-hot__card-name">{{ s.displayName || s.name }}</h3>
                <span class="home-hot__card-author">👤 {{ s.authorName || 'anonymous' }}</span>
              </div>
            </div>
            <p class="home-hot__card-desc">{{ truncate(s.description || '', 90) }}</p>
            <footer class="home-hot__card-foot">
              <a-rate
                :value="s.ratingAvg || 0"
                disabled
                allow-half
                :count="5"
                style="font-size: 12px"
              />
              <span class="home-hot__card-rate">
                {{ ((s.ratingAvg || 0)).toFixed(1) }}
                <span class="muted">({{ s.ratingCount || 0 }})</span>
              </span>
              <span class="home-hot__card-dl">⬇ {{ formatNum(s.installs || 0) }}</span>
            </footer>
          </article>
        </a-col>
      </a-row>
    </a-spin>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SkillLogo from '../SkillLogo.vue'
import { skillApi } from '@/api/skill'
import type { Skill, Category } from '@/types/skill'

interface Props {
  usageCategories: Category[]
}
defineProps<Props>()

const router = useRouter()
const activeTab = ref<string>('overall')
const list = ref<Skill[]>([])
const loading = ref(false)

interface Tab {
  key: string
  label: string
  icon?: string
}
const tabs = ref<Tab[]>([
  { key: 'overall', label: '总排行', icon: '📊' },
  { key: 'recent', label: '近期最热', icon: '🔥' },
  { key: 'latest', label: '最新上传', icon: '🆕' }
])

// 动态追加 USAGE 一级分类（最多前 7 个）
function buildUsageTabs(cats: Category[]) {
  const top = (cats || []).filter((c) => c.type === 'USAGE' && !c.parentId).slice(0, 7)
  for (const c of top) {
    tabs.value.push({ key: `usage:${c.id}`, label: c.name })
  }
}

async function load() {
  loading.value = true
  try {
    const tab = activeTab.value
    if (tab === 'overall') {
      list.value = await skillApi.hot(12, 'hot')
    } else if (tab === 'recent') {
      list.value = await skillApi.hot(12, 'recent')
    } else if (tab === 'latest') {
      const res = await skillApi.list({ sort: 'latest', size: 12 })
      list.value = res.records
    } else if (tab.startsWith('usage:')) {
      const id = Number(tab.slice(6))
      const res = await skillApi.list({ usageCategoryId: id, size: 12 })
      list.value = res.records
    }
  } finally {
    loading.value = false
  }
}

function onTab(key: string) {
  activeTab.value = key
  load()
}

function goDetail(s: Skill) {
  if (!s.slug) return
  router.push({ name: 'skill-detail', params: { slug: s.slug } })
}

function truncate(s: string, n: number) {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '…' : s
}

function formatNum(n: number) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

onMounted(() => load())

defineExpose({ buildUsageTabs, reload: load })
</script>

<style scoped lang="scss">
.home-hot {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px 64px;
  &__head {
    text-align: center;
    margin-bottom: 24px;
  }
  &__title {
    margin: 0 0 16px;
    font-size: 28px;
    font-weight: 800;
    color: var(--text-primary);
    .leaf {
      margin: 0 8px;
    }
  }
  &__tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
  }
  &__tab {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    border: 1px solid var(--border);
    border-radius: 999px;
    background: var(--bg-secondary);
    color: var(--text-secondary);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 150ms ease-out;
    &:hover {
      border-color: var(--primary);
      color: var(--primary);
    }
    &.is-active {
      background: var(--primary);
      border-color: var(--primary);
      color: var(--text-inverse);
    }
  }

  &__card {
    background: var(--bg-secondary);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 16px;
    cursor: pointer;
    height: 100%;
    display: flex;
    flex-direction: column;
    transition: all 200ms ease-out;
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08);
      border-color: #c4b5fd;
    }
  }
  &__card-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
  }
  &__card-meta {
    flex: 1;
    min-width: 0;
  }
  &__card-name {
    margin: 0 0 2px;
    font-size: 15px;
    font-weight: 700;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__card-author {
    font-size: 12px;
    color: var(--text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }
  &__card-desc {
    margin: 0 0 12px;
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.5;
    flex: 1;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  &__card-foot {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: var(--text-secondary);
    border-top: 1px solid var(--border);
    padding-top: 8px;
  }
  &__card-rate {
    font-weight: 600;
    color: var(--warning);
    .muted {
      color: var(--text-tertiary);
      font-weight: 400;
      margin-left: 2px;
    }
  }
  &__card-dl {
    margin-left: auto;
    color: var(--text-tertiary);
  }
}
</style>
