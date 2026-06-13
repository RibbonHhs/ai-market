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
      :aria-label="`${cat.name}，${cat.skillCount ?? 0} 个技能`"
      @click="onSelect(cat)"
      @keydown.enter="onSelect(cat)"
      @keydown.space.prevent="onSelect(cat)"
    >
      <h3 class="occ-card__name">{{ cat.name }}</h3>
      <div class="occ-card__count" :aria-hidden="true">
        <span class="occ-card__count-num">{{ cat.skillCount ?? 0 }}</span>
        <span class="occ-card__count-unit">个技能</span>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import type { Category } from '@/types/skill'

withDefaults(defineProps<{
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

/* S34-followup: 极简大卡片 — indigo 渐变背景 + 名称 + 计数 */
.occ-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 28px 24px;
  min-height: 168px;
  border-radius: 20px;
  background:
    radial-gradient(140% 110% at 100% 0%, #ede9fe 0%, transparent 60%),
    linear-gradient(180deg, #eef2ff 0%, #f5f3ff 100%);
  cursor: default;
  outline: none;
  animation: occ-card-in 360ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--i, 0) * 40ms);
  transition:
    transform 240ms cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 240ms ease-out;
}

.occ-card--clickable {
  cursor: pointer;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 28px -10px rgba(99, 102, 241, 0.35);
  }
  &:active {
    transform: translateY(-1px) scale(0.99);
    transition-duration: 120ms;
  }
  &:focus-visible {
    outline: 2px solid #6366f1;
    outline-offset: 3px;
  }
}

.occ-card__name {
  margin: 0;
  font-size: clamp(20px, 1.8vw, 24px);
  font-weight: 700;
  color: #1e1b4b;
  letter-spacing: -0.01em;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.occ-card__count {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 16px;

  &-num {
    font-size: 28px;
    font-weight: 700;
    color: #4338ca;
    font-variant-numeric: tabular-nums;
    line-height: 1;
  }
  &-unit {
    font-size: 13px;
    color: #6d28d9;
    font-weight: 500;
  }
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
}

@media (prefers-color-scheme: dark) {
  .occ-card:not([data-theme='light']) {
    background:
      radial-gradient(140% 110% at 100% 0%, rgba(99, 102, 241, 0.20) 0%, transparent 60%),
      linear-gradient(180deg, rgba(99, 102, 241, 0.10) 0%, rgba(139, 92, 246, 0.08) 100%);
  }
  .occ-card:not([data-theme='light']) .occ-card__name {
    color: #c7d2fe;
  }
  .occ-card:not([data-theme='light']) .occ-card__count-num {
    color: #a5b4fc;
  }
  .occ-card:not([data-theme='light']) .occ-card__count-unit {
    color: rgba(199, 210, 254, 0.7);
  }
}
</style>