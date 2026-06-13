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
      :aria-label="ariaLabelFor(cat)"
      @click="onSelect(cat)"
      @keydown.enter="onSelect(cat)"
      @keydown.space.prevent="onSelect(cat)"
    >
      <!-- S34: icon-tile — 浅色 USAGE bg + fg；暗色态 16% rgba + 鲜亮 fg -->
      <div class="usage-card__tile" aria-hidden="true">
        <AimOutlined class="usage-card__icon" />
      </div>

      <div class="usage-card__body">
        <div class="usage-card__head">
          <h3 class="usage-card__title">{{ cat.name }}</h3>
          <code class="usage-card__code">{{ parentCodeOf(cat) }}</code>
        </div>
        <p v-if="cat.description" class="usage-card__desc">{{ cat.description }}</p>
        <p v-else class="usage-card__desc usage-card__desc--muted">
          {{ subHint(cat) }}
        </p>
        <div class="usage-card__foot">
          <span class="usage-card__count" :aria-label="`${cat.skillCount ?? 0} 个 skill`">
            <span class="usage-card__count-num">{{ cat.skillCount ?? 0 }}</span>
            <span class="usage-card__count-unit">个 skill</span>
          </span>
          <span v-if="cat.slug" class="usage-card__arrow" aria-hidden="true">→</span>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed, CSSProperties } from 'vue'
import { AimOutlined } from '@ant-design/icons-vue'
import { getUsageColor, getUsageDarkColor } from '@/constants/usage-colors'
import type { Category } from '@/types/skill'

const props = withDefaults(defineProps<{
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

/** parentCode 取色（浅色 + 暗色两套；CSS 变量交给样式层切） */
function paletteOf(cat: Category) {
  const code = (cat.code || '').split('-').slice(0, 2).join('-') || null
  return {
    light: getUsageColor(code),
    dark: getUsageDarkColor(code)
  }
}

/** 显示在卡片小字位的父级 code（如 PURPOSE-DEV） */
function parentCodeOf(cat: Category): string {
  const code = cat.code || ''
  return code ? code.split('-').slice(0, 2).join('-') : ''
}

function ariaLabelFor(cat: Category): string {
  const sub = subHint(cat)
  const count = cat.skillCount ?? 0
  return `用途分类：${cat.name}，${sub}，${count} 个 skill`
}

function subHint(cat: Category): string {
  const totalChildren = (props.categories || []).filter((c) => c.parentId === cat.id).length
  if (totalChildren > 0) return `${totalChildren} 个细分用途`
  return '主要用途'
}

/** 把 USAGE 配色注入 CSS 变量；父组件不需要知道怎么配色 */
function cardStyle(cat: Category, idx: number): CSSProperties {
  const { light } = paletteOf(cat)
  return {
    ['--i' as string]: idx,
    ['--tile-bg' as string]: light.bg,
    ['--tile-fg' as string]: light.fg
  }
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

/* S34: 大卡片 — border-radius 16px、阴影静态/hover、min-height 132px */
.usage-card {
  --tile-bg: #f5f5f5;
  --tile-fg: #595959;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  min-height: 132px;
  background: var(--bg-secondary, #ffffff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04), 0 2px 6px rgba(0, 0, 0, 0.03);
  cursor: default;
  outline: none;
  animation: usage-card-in 360ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--i, 0) * 40ms);
  transition:
    transform 200ms cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 200ms ease-out,
    border-color 200ms ease-out,
    background 200ms ease-out;
}

.usage-card--clickable {
  cursor: pointer;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px -6px rgba(0, 0, 0, 0.10);
    border-color: var(--primary, #6366f1);
  }
  &:active {
    transform: translateY(0) scale(0.98);
    transition-duration: 120ms;
  }
  &:focus-visible {
    outline: 2px solid var(--primary, #6366f1);
    outline-offset: 3px;
  }
}

/* S34: icon-tile — 48×48、12 圆角；颜色由每张卡的 inline style 注入 */
.usage-card__tile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--tile-bg);
  color: var(--tile-fg);
  flex-shrink: 0;

  @media (max-width: 480px) {
    width: 44px;
    height: 44px;
  }
}

.usage-card__icon {
  font-size: 22px;
  line-height: 1;
  :deep(svg) {
    width: 1em;
    height: 1em;
    fill: currentColor;
  }
}

.usage-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.usage-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.usage-card__title {
  margin: 0;
  font-size: clamp(15px, 1.4vw, 17px);
  font-weight: 700;
  color: var(--text-primary, #111827);
  line-height: 1.3;
  letter-spacing: -0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.usage-card__code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--text-tertiary, #9ca3af);
  background: transparent;
  padding: 0;
  flex-shrink: 0;
}

.usage-card__desc {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  color: var(--text-secondary, #6b7280);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;

  &--muted {
    color: var(--text-tertiary, #9ca3af);
    font-style: normal;
  }
}

.usage-card__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 4px;
}

.usage-card__count {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;

  &-num {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary, #111827);
    font-variant-numeric: tabular-nums;
  }
  &-unit {
    font-size: 12px;
    color: var(--text-tertiary, #9ca3af);
  }
}

.usage-card__arrow {
  font-size: 18px;
  line-height: 1;
  color: var(--text-tertiary, #9ca3af);
  font-weight: 400;
  transition: transform 200ms ease-out, color 200ms ease-out;
}

.usage-card--clickable:hover .usage-card__arrow {
  transform: translateX(3px);
  color: var(--primary, #6366f1);
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
  .usage-card--clickable:hover .usage-card__arrow {
    transform: none;
  }
}

@media (prefers-color-scheme: dark) {
  .usage-card:not([data-theme='light']) {
    background: #1c1c1f;
    border-color: rgba(255, 255, 255, 0.08);
  }
}
</style>