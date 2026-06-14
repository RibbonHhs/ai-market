<template>
  <a-layout class="app-layout">
    <AppHeader />
    <a-layout-content class="app-content">
      <section class="occupations-page" :class="`dim-${dim}`">
        <header class="hero">
          <div class="hero__eyebrow">{{ heroEyebrow }}</div>
          <h1>{{ heroTitle }}</h1>
          <p>
            <span class="hero__count">{{ topLevel.length }}</span>
            {{ heroCountUnit1 }} ·
            <span class="hero__count">{{ totalSubGroups }}</span>
            {{ heroCountUnit2 }}
          </p>
        </header>

        <hr class="hero__divider" />

        <a-empty
          v-if="!loading && !topLevel.length"
          :description="emptyText"
          class="empty-block"
        />

        <div v-else class="grid" role="list">
          <OccupationCard
            v-for="(cat, idx) in topLevel"
            :key="cat.id"
            :category="cat"
            :index="idx"
            :dim="dim"
          />
        </div>
      </section>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import OccupationCard from '@/components/OccupationCard.vue'
import { categoryApi } from '@/api/skill'
import type { Category } from '@/types/skill'

interface CategoryNode extends Category {
  parentId?: number | null
  type?: string
}

const route = useRoute()
const dim = computed<string>(() => (route.meta?.dim as string) || 'USAGE')

const loading = ref(true)
const categories = ref<CategoryNode[]>([])

const topLevel = computed(() =>
  categories.value
    .filter((c) => c.parentId == null || c.parentId === 0)
    .filter((c) => (dim.value === 'SOC' ? c.type !== 'USAGE' : c.type === 'USAGE'))
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id)
)

const totalSubGroups = computed(() => {
  const childCount = categories.value.filter(
    (c) => c.parentId != null && c.parentId !== 0
  ).length
  return Math.max(0, childCount)
})

const isSoc = computed(() => dim.value === 'SOC')
const heroEyebrow = computed(() =>
  isSoc.value ? 'OCCUPATIONS · SOC 2018' : 'USAGE · SKILL MARKETPLACE'
)
const heroTitle = computed(() =>
  isSoc.value ? '职业技能（按 SOC 标准）' : '用途分类（按主要用途）'
)
const heroCountUnit1 = computed(() => (isSoc.value ? '个主要职业组' : '个一级分类'))
const heroCountUnit2 = computed(() => (isSoc.value ? '个细分职位' : '个细分用途'))
const emptyText = computed(() => (isSoc.value ? '暂无职业分类' : '暂无用途分类'))

onMounted(async () => {
  try {
    categories.value = (await categoryApi.list()) as CategoryNode[]
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: #fafafa;
}
.app-content {
  width: 100%;
}

.occupations-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 64px 24px 80px;
}

.hero {
  text-align: center;
  margin-bottom: 40px;
  padding: 24px 0 8px;

  &__eyebrow {
    display: inline-block;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 2px;
    text-transform: uppercase;
    color: #f97316;
    margin-bottom: 16px;
  }

  h1 {
    font-size: clamp(32px, 4.5vw, 48px);
    font-weight: 900;
    letter-spacing: -0.03em;
    color: #111827;
    margin: 0;
    line-height: 1.1;
  }

  p {
    color: #6b7280;
    font-size: 15px;
    margin: 14px 0 0;
    font-weight: 400;
  }

  &__count {
    color: #374151;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    font-size: 17px;
  }

  &__divider {
    border: 0;
    border-top: 1px solid #e5e7eb;
    margin: 32px 0 32px;
  }
}

/* SOC hero — indigo/violet gradient backdrop (强调官方标准) */
.occupations-page.dim-SOC {
  .hero {
    border-radius: 20px;
    background:
      radial-gradient(70% 90% at 50% 0%, rgba(99, 102, 241, 0.10) 0%, rgba(99, 102, 241, 0) 70%),
      radial-gradient(40% 60% at 85% 30%, rgba(139, 92, 246, 0.08) 0%, rgba(139, 92, 246, 0) 70%),
      linear-gradient(180deg, #f5f3ff 0%, #fafafa 100%);
    padding: 56px 24px 48px;
    margin: 24px 0 32px;
    border: 1px solid #ede9fe;
  }
  .hero__divider {
    display: none;
  }
}

/* USAGE hero — amber/orange gradient backdrop (强调工作流) */
.occupations-page.dim-USAGE {
  .hero {
    border-radius: 20px;
    background:
      radial-gradient(70% 90% at 50% 0%, rgba(249, 115, 22, 0.10) 0%, rgba(249, 115, 22, 0) 70%),
      radial-gradient(40% 60% at 15% 30%, rgba(245, 158, 11, 0.08) 0%, rgba(245, 158, 11, 0) 70%),
      linear-gradient(180deg, #fff7ed 0%, #fafafa 100%);
    padding: 56px 24px 48px;
    margin: 24px 0 32px;
    border: 1px solid #fed7aa;
  }
  .hero__divider {
    display: none;
  }
}

.empty-block {
  padding: 80px 0;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}
</style>