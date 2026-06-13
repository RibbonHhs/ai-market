<template>
  <a-card
    class="occ-card"
    :body-style="{ padding: '0' }"
    :aria-label="`${category.name}，${category.skillCount ?? 0} 个技能`"
    role="listitem"
    hoverable
    tabindex="0"
    @click="go(category.slug)"
    @keydown.enter="go(category.slug)"
  >
    <div class="occ-card__inner">
      <div class="big-num" aria-hidden="true">{{ padIndex(index) }}</div>
      <div class="code">#{{ padIndex(index) }}</div>
      <h3 class="name">{{ category.name }}</h3>
      <div class="count">
        <span class="count__num">{{ formatCount(category.skillCount ?? 0) }}</span>
        <span class="count__unit">个技能</span>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Category } from '@/types/skill'

const props = defineProps<{
  category: Category
  index?: number
  /** S35: 维度类型（USAGE / SOC），用于跳转时带 query.type 避免 BrowseView 维度误判 */
  dim?: 'USAGE' | 'SOC'
}>()

const router = useRouter()

function padIndex(idx: number): string {
  return String((idx ?? 0) + 1).padStart(2, '0')
}

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + ' 万'
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + ' 千'
  return String(n)
}

function go(slug: string) {
  // S35: 带 query.type 让 BrowseView 显式知道当前是 USAGE 还是 SOC 维度
  router.push({
    name: 'category-browse',
    params: { slug },
    query: props.dim ? { type: props.dim } : {}
  })
}
</script>

<style scoped lang="scss">
.occ-card {
  position: relative;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  cursor: pointer;
  height: 100%;
  transition:
    transform 200ms ease-out,
    box-shadow 200ms ease-out,
    border-color 200ms ease-out;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08);
    border-color: #6366f1;
  }

  &:focus-visible {
    outline: 2px solid #6366f1;
    outline-offset: 2px;
  }

  &__inner {
    position: relative;
    padding: 28px 24px;
    min-height: 168px;
  }

  .big-num {
    position: absolute;
    top: 50%;
    right: 12px;
    transform: translateY(-50%);
    font-size: 88px;
    font-weight: 800;
    color: #f3f4f6;
    line-height: 1;
    letter-spacing: -3px;
    pointer-events: none;
    user-select: none;
    font-variant-numeric: tabular-nums;
  }

  .code {
    position: relative;
    font-size: 12px;
    color: #9ca3af;
    font-weight: 600;
    letter-spacing: 0.5px;
    margin-bottom: 8px;
    font-variant-numeric: tabular-nums;
  }

  .name {
    position: relative;
    font-size: 17px;
    font-weight: 700;
    color: #111827;
    margin: 0 0 20px;
    line-height: 1.3;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    padding-right: 80px;
  }

  .count {
    position: relative;
    display: flex;
    align-items: baseline;
    gap: 6px;

    &__num {
      font-size: 16px;
      color: #374151;
      font-weight: 600;
      font-variant-numeric: tabular-nums;
    }

    &__unit {
      font-size: 13px;
      color: #9ca3af;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .occ-card {
    transition: none;
    &:hover {
      transform: none;
    }
  }
}
</style>