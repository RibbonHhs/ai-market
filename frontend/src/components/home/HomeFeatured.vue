<template>
  <section class="home-featured">
    <div class="home-featured__head">
      <h2 class="home-featured__title">
        <span class="leaf">🌿</span> 精选 Skills 榜单 <span class="leaf">🌿</span>
      </h2>
      <p class="home-featured__sub">人工筛选全网最实用的 Skills，严格安全认证，国内镜像下载加速</p>
    </div>

    <div class="home-featured__layout">
      <!-- 左侧 dark 侧栏 -->
      <aside class="home-featured__sidebar">
        <button
          type="button"
          class="home-featured__cat"
          :class="{ 'is-active': activeCat === null }"
          @click="activeCat = null"
        >
          精选 Top {{ totalCount }}
        </button>
        <button
          v-for="c in visibleSoc"
          :key="c.id"
          type="button"
          class="home-featured__cat"
          :class="{ 'is-active': activeCat === c.id }"
          @click="onSelectCat(c.id)"
        >
          {{ c.name }}
        </button>
      </aside>

      <!-- 右侧排行表 -->
      <div class="home-featured__table">
        <a-empty v-if="!filtered.length" description="该分类下暂无精选" />
        <ol v-else class="home-featured__list">
          <li
            v-for="(s, i) in filtered"
            :key="s.id"
            class="home-featured__row"
            @click="goDetail(s)"
          >
            <span class="home-featured__rank" :class="{ 'is-top': i < 3 }">{{ i + 1 }}</span>
            <SkillLogo :name="s.name" :size="36" />
            <div class="home-featured__meta">
              <div class="home-featured__name">{{ s.displayName || s.name }}</div>
              <div class="home-featured__desc">{{ truncate(s.description || '', 60) }}</div>
            </div>
            <div class="home-featured__rate">
              <a-rate
                :value="s.ratingAvg || 0"
                disabled
                allow-half
                :count="5"
                style="font-size: 12px"
              />
              <span class="home-featured__rate-num">{{ ((s.ratingAvg || 0)).toFixed(1) }}</span>
            </div>
            <div class="home-featured__downloads">
              ⬇ {{ formatNum(s.installs || 0) }}
            </div>
          </li>
        </ol>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import SkillLogo from '../SkillLogo.vue'
import type { Skill, Category } from '@/types/skill'

interface Props {
  featured: Skill[]
  socCategories: Category[]
  totalCount: number
}
const props = defineProps<Props>()
const router = useRouter()
const activeCat = ref<number | null>(null)

const visibleSoc = computed(() =>
  (props.socCategories || []).slice(0, 8)
)

const filtered = computed(() => props.featured)

function onSelectCat(id: number) {
  activeCat.value = id
  // 当前 featured 不分分类，切换仅 UI 切换（fallback 用同样的列表）
  // 真实分类榜数据留 v1.1
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
</script>

<style scoped lang="scss">
.home-featured {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  &__head {
    text-align: center;
    margin-bottom: 24px;
  }
  &__title {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 800;
    color: var(--text-primary);
    .leaf {
      margin: 0 8px;
    }
  }
  &__sub {
    margin: 0;
    color: var(--text-secondary);
    font-size: 14px;
  }
  &__layout {
    display: grid;
    grid-template-columns: 200px 1fr;
    gap: 16px;
    background: var(--bg-primary);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  }
  &__sidebar {
    background: var(--bg-tertiary);
    border-radius: 10px;
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  &__cat {
    text-align: left;
    padding: 10px 14px;
    border: none;
    background: transparent;
    color: var(--text-tertiary);
    font-size: 13px;
    font-weight: 500;
    border-radius: 6px;
    cursor: pointer;
    transition: all 150ms ease-out;
    &:hover {
      background: var(--bg-elevated);
      color: var(--text-primary);
    }
    &.is-active {
      background: var(--bg-primary);
      color: var(--text-primary);
      font-weight: 700;
    }
  }
  &__list {
    list-style: none;
    margin: 0;
    padding: 0;
  }
  &__row {
    display: grid;
    grid-template-columns: 36px 36px 1fr 140px 80px;
    gap: 16px;
    align-items: center;
    padding: 12px 14px;
    border-bottom: 1px solid var(--border);
    cursor: pointer;
    transition: background 150ms ease-out;
    &:last-child {
      border-bottom: none;
    }
    &:hover {
      background: var(--bg-tertiary);
    }
  }
  &__rank {
    font-size: 18px;
    font-weight: 800;
    color: var(--text-tertiary);
    text-align: center;
    font-variant-numeric: tabular-nums;
    &.is-top {
      color: var(--primary);
    }
  }
  &__meta {
    min-width: 0;
  }
  &__name {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 2px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__desc {
    font-size: 12px;
    color: var(--text-secondary);
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__rate {
    display: flex;
    align-items: center;
    gap: 4px;
  }
  &__rate-num {
    font-size: 12px;
    font-weight: 600;
    color: var(--warning);
  }
  &__downloads {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-secondary);
    text-align: right;
  }
}

@media (max-width: 768px) {
  .home-featured__layout {
    grid-template-columns: 1fr;
  }
  .home-featured__sidebar {
    flex-direction: row;
    overflow-x: auto;
    background: var(--bg-tertiary);
  }
  .home-featured__cat {
    white-space: nowrap;
    &.is-active {
      background: var(--primary);
      color: var(--text-inverse);
    }
  }
  .home-featured__row {
    grid-template-columns: 28px 28px 1fr;
    gap: 8px;
  }
  .home-featured__rate,
  .home-featured__downloads {
    display: none;
  }
}
</style>
