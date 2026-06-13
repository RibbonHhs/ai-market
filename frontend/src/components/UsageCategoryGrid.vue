<template>
  <div class="usage-grid" role="list">
    <a-empty
      v-if="!loading && !categories.length"
      :description="emptyText"
      class="usage-grid__empty"
    />
    <article
      v-for="(cat, idx) in categories"
      v-else
      :key="cat.id"
      class="usage-card"
      :class="{ 'usage-card--clickable': !!cat.slug }"
      :style="cardStyle(cat, idx)"
      role="listitem"
      tabindex="0"
      :aria-label="`${cat.name}，${cat.skillCount ?? 0} 个 skill`"
      @click="onSelect(cat)"
      @keydown.enter="onSelect(cat)"
      @keydown.space.prevent="onSelect(cat)"
    >
      <h3 class="usage-card__name">{{ cat.name }}</h3>
      <div class="usage-card__count" :aria-hidden="true">
        <span class="usage-card__count-num">{{ cat.skillCount ?? 0 }}</span>
        <span class="usage-card__count-unit">个 skill</span>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { CSSProperties } from 'vue'
import { getUsageColor, getUsageDarkColor } from '@/constants/usage-colors'
import type { Category } from '@/types/skill'

withDefaults(defineProps<{
  categories: Category[]
  loading?: boolean
  emptyText?: string
}>(), {
  loading: false,
  emptyText: '暂无用途分类'
})

const emit = defineEmits<{
  (e: 'select', category: Category): void
}>()

/** 按 parentCode 取浅色 + 暗色两套 */
function paletteOf(cat: Category) {
  const code = (cat.code || '').split('-').slice(0, 2).join('-') || null
  return { light: getUsageColor(code), dark: getUsageDarkColor(code) }
}

/** 注入渐变背景：USAGE 浅色主色 + 邻色高光，制造柔和渐变 */
function cardStyle(cat: Category, idx: number): CSSProperties {
  const { light } = paletteOf(cat)
  return {
    ['--i' as string]: idx,
    ['--bg-base' as string]: light.bg,
    ['--bg-tint' as string]: lighten(light.bg, 0.35),
    ['--fg-strong' as string]: light.fg,
    ['--fg-soft' as string]: withAlpha(light.fg, 0.7)
  }
}

/** 把 hex 转 rgba 并提亮；失败回退原值 */
function lighten(hex: string, amount: number): string {
  const rgb = parseHex(hex)
  if (!rgb) return hex
  const r = Math.round(rgb.r + (255 - rgb.r) * amount)
  const g = Math.round(rgb.g + (255 - rgb.g) * amount)
  const b = Math.round(rgb.b + (255 - rgb.b) * amount)
  return `rgb(${r}, ${g}, ${b})`
}

function withAlpha(hex: string, alpha: number): string {
  const rgb = parseHex(hex)
  if (!rgb) return hex
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`
}

function parseHex(hex: string): { r: number; g: number; b: number } | null {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim())
  if (!m) return null
  const n = parseInt(m[1], 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

function onSelect(cat: Category) {
  emit('select', cat)
}
</script>

<style scoped lang="scss">
.usage-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  @media (max-width: 480px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  &__empty {
    grid-column: 1 / -1;
    padding: 80px 0;
  }
}

/* S34-followup: 极简大卡片 — 渐变背景 + 名称 + 计数，无图标/无副标题 */
.usage-card {
  --bg-base: #f5f5f5;
  --bg-tint: #fafafa;
  --fg-strong: #262626;
  --fg-soft: #8c8c8c;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 28px 24px;
  min-height: 168px;
  border-radius: 20px;
  background:
    radial-gradient(120% 100% at 100% 0%, var(--bg-tint) 0%, transparent 60%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-tint) 100%);
  cursor: default;
  outline: none;
  animation: usage-card-in 360ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--i, 0) * 40ms);
  transition:
    transform 240ms cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 240ms ease-out;
}

.usage-card--clickable {
  cursor: pointer;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 28px -10px var(--fg-soft);
  }
  &:active {
    transform: translateY(-1px) scale(0.99);
    transition-duration: 120ms;
  }
  &:focus-visible {
    outline: 2px solid var(--fg-strong);
    outline-offset: 3px;
  }
}

.usage-card__name {
  margin: 0;
  font-size: clamp(20px, 1.8vw, 24px);
  font-weight: 700;
  color: var(--fg-strong);
  letter-spacing: -0.01em;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.usage-card__count {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 16px;

  &-num {
    font-size: 28px;
    font-weight: 700;
    color: var(--fg-strong);
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  &-unit {
    font-size: 13px;
    color: var(--fg-soft);
    font-weight: 500;
  }
}

@keyframes usage-card-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .usage-card {
    animation: none;
    transition: none;
    &:hover {
      transform: none;
    }
  }
}

/* 暗色态：加深底色 + 鲜亮 fg；保持相同渐变结构 */
@media (prefers-color-scheme: dark) {
  .usage-card:not([data-theme='light']) {
    --bg-base: rgba(255, 255, 255, 0.04);
    --bg-tint: rgba(255, 255, 255, 0.08);
    color: var(--fg-strong);
  }
}
</style>