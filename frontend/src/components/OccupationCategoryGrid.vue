<template>
  <div class="occ-grid" role="list">
    <a-empty
      v-if="!loading && !categories.length"
      :description="emptyText"
      class="occ-grid__empty"
    />
    <article
      v-for="(cat, idx) in categories"
      v-else
      :key="cat.id"
      class="occ-card"
      :class="{ 'occ-card--clickable': !!cat.slug }"
      :style="{ '--i': idx }"
      role="listitem"
      tabindex="0"
      :aria-label="ariaLabelFor(cat)"
      @click="onSelect(cat)"
      @keydown.enter="onSelect(cat)"
      @keydown.space.prevent="onSelect(cat)"
    >
      <!-- 序号大字（保留 S32 装饰资产） -->
      <div class="occ-card__index" aria-hidden="true">{{ padIndex(idx) }}</div>

      <div class="occ-card__head">
        <!-- S34: icon-tile — 蓝色统一（S32 chip-occupation 一致） -->
        <div class="occ-card__tile" aria-hidden="true">
          <ToolOutlined class="occ-card__icon" />
        </div>
        <div class="occ-card__title-block">
          <h3 class="occ-card__title">{{ cat.name }}</h3>
          <code class="occ-card__code">{{ cat.code || 'SOC' }}</code>
        </div>
      </div>

      <p v-if="cat.description" class="occ-card__desc">{{ cat.description }}</p>
      <p v-else class="occ-card__desc occ-card__desc--muted">{{ subHint(cat) }}</p>

      <div class="occ-card__foot">
        <span class="occ-card__count" :aria-label="`${cat.skillCount ?? 0} 个技能`">
          <span class="occ-card__count-num">{{ formatCount(cat.skillCount ?? 0) }}</span>
          <span class="occ-card__count-unit">个技能</span>
        </span>
        <span v-if="cat.slug" class="occ-card__arrow" aria-hidden="true">→</span>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { ToolOutlined } from '@ant-design/icons-vue'
import type { Category } from '@/types/skill'

const props = withDefaults(defineProps<{
  categories: Category[]
  loading?: boolean
  emptyText?: string
}>(), {
  loading: false,
  emptyText: '暂无职业分类'
})

const emit = defineEmits<{
  (e: 'select', category: Category): void
}>()

function padIndex(idx: number): string {
  return String((idx ?? 0) + 1).padStart(2, '0')
}

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + ' 万'
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + ' 千'
  return String(n)
}

function ariaLabelFor(cat: Category): string {
  const sub = subHint(cat)
  const count = cat.skillCount ?? 0
  return `职业分类：${cat.name}，${sub}，${formatCount(count)} 个技能`
}

function subHint(cat: Category): string {
  if (!cat.parentId) return '主要职业组'
  return '细分职位'
}

function onSelect(cat: Category) {
  emit('select', cat)
}
</script>

<style scoped lang="scss">
.occ-grid {
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

/* S34: 大卡片 — 与 USAGE 节奏一致，序号大字保留（S32 装饰资产） */
.occ-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 20px 18px;
  min-height: 156px;
  background: var(--bg-secondary, #ffffff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04), 0 2px 6px rgba(0, 0, 0, 0.03);
  cursor: default;
  outline: none;
  animation: occ-card-in 360ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--i, 0) * 40ms);
  transition:
    transform 200ms cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 200ms ease-out,
    border-color 200ms ease-out;
}

.occ-card--clickable {
  cursor: pointer;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px -6px rgba(0, 0, 0, 0.10);
    border-color: #6366f1;
  }
  &:active {
    transform: translateY(0) scale(0.98);
    transition-duration: 120ms;
  }
  &:focus-visible {
    outline: 2px solid #6366f1;
    outline-offset: 3px;
  }
}

/* S32 序号大字保留为右下角装饰 */
.occ-card__index {
  position: absolute;
  top: 50%;
  right: 14px;
  transform: translateY(-50%);
  font-size: 88px;
  font-weight: 800;
  color: #f3f4f6;
  line-height: 1;
  letter-spacing: -3px;
  pointer-events: none;
  user-select: none;
  font-variant-numeric: tabular-nums;
  z-index: 0;
}

.occ-card--clickable:hover .occ-card__index {
  color: #ede9fe;
}

.occ-card__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* S34: 蓝色 icon-tile，与 S32 chip-occupation 一致 */
.occ-card__tile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #e6f4ff;
  color: #0958d9;
  flex-shrink: 0;

  @media (max-width: 480px) {
    width: 44px;
    height: 44px;
  }
}

.occ-card__icon {
  font-size: 22px;
  line-height: 1;
  :deep(svg) {
    width: 1em;
    height: 1em;
    fill: currentColor;
  }
}

.occ-card__title-block {
  flex: 1;
  min-width: 0;
}

.occ-card__title {
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

.occ-card__code {
  display: inline-block;
  margin-top: 2px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--text-tertiary, #9ca3af);
}

.occ-card__desc {
  position: relative;
  z-index: 1;
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
  }
}

.occ-card__foot {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 4px;
}

.occ-card__count {
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

.occ-card__arrow {
  font-size: 18px;
  line-height: 1;
  color: var(--text-tertiary, #9ca3af);
  transition: transform 200ms ease-out, color 200ms ease-out;
}

.occ-card--clickable:hover .occ-card__arrow {
  transform: translateX(3px);
  color: #6366f1;
}

@keyframes occ-card-in {
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
  .occ-card {
    animation: none;
    transition: none;
    &:hover {
      transform: none;
    }
  }
  .occ-card--clickable:hover .occ-card__arrow {
    transform: none;
  }
}

@media (prefers-color-scheme: dark) {
  .occ-card:not([data-theme='light']) {
    background: #1c1c1f;
    border-color: rgba(255, 255, 255, 0.08);
  }
  .occ-card:not([data-theme='light']) .occ-card__index {
    color: rgba(255, 255, 255, 0.04);
  }
  .occ-card:not([data-theme='light']) .occ-card__tile {
    background: rgba(96, 165, 250, 0.16);
    color: #93c5fd;
  }
}
</style>