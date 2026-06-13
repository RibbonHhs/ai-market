<template>
  <component
    v-if="visible"
    :is="tag"
    class="usage-chip usage-chip--pill"
    :class="[sizeClass, codeClass, { 'is-clickable': clickable }]"
    :data-testid="testid"
    :data-kind="kind"
    :to="clickable && to ? to : undefined"
    :role="ariaLabel ? 'img' : undefined"
    :aria-label="ariaLabel || undefined"
  >
    <!-- S32: 类型图标 — AimOutlined (用途) / ToolOutlined (职业)，结构化矢量图标 -->
    <span class="usage-chip__type-icon" aria-hidden="true">
      <component :is="typeIconComp" />
    </span>
    <span v-if="showCategoryEmoji" class="usage-chip__emoji" aria-hidden="true">{{ resolvedEmoji }}</span>
    <span class="usage-chip__parent">{{ resolvedParent }}</span>
    <template v-if="showArrow">
      <span class="usage-chip__sep" aria-hidden="true">→</span>
      <span class="usage-chip__child">{{ childName }}</span>
    </template>
    <!-- S33: 技能计数（仅当非 null 时显示） -->
    <span v-if="resolvedSkillCount != null" class="usage-chip__count" :data-testid="testid ? `${testid}-count` : undefined">
      · {{ resolvedSkillCount }}
    </span>
  </component>
</template>

<script setup lang="ts">
/**
 * S32: 分类 chip — 用途分类（USAGE）+ 职业技能（OCCUPATION）两处统一使用。
 *  - kind="usage"        → <AimOutlined /> + 12 色 USAGE 配色（按 parentCode）
 *  - kind="occupation"   → <ToolOutlined /> + 蓝色 SOC 配色
 *  - kind 取代 v1 的 variant（保留 variant 兼容旧调用）
 * 样式在 global.scss 的 .usage-chip + .usage-chip--code-*（含 --code-occupation）。
 * 视觉规范见 docs/sprints/S32/design-chip-row.md
 */
import { computed } from 'vue'
import { AimOutlined, ToolOutlined } from '@ant-design/icons-vue'
import { getUsageColor } from '@/constants/usage-colors'

const props = withDefaults(defineProps<{
  parentCode?: string | null
  parentName?: string | null
  childName?: string | null
  emoji?: string | null
  /** S32: 取代 variant；'usage'=用途分类，'occupation'=职业技能 */
  kind?: 'usage' | 'occupation'
  /** 旧 prop，过渡期兼容；内部映射到 kind */
  variant?: 'usage' | 'occupation'
  size?: 'sm' | 'md' | 'lg'
  clickable?: boolean
  to?: string | Record<string, unknown>
  testid?: string
  /** S33: 该分类下的 published skill 数；null/undefined 时不显示 */
  skillCount?: number | null
}>(), {
  parentCode: null,
  parentName: null,
  childName: null,
  emoji: null,
  kind: undefined,
  variant: 'usage',
  size: 'md',
  clickable: false,
  to: '',
  testid: undefined,
  skillCount: null
})

/* S32: kind 优先，variant 作为兼容回退 */
const kind = computed<'usage' | 'occupation'>(() => props.kind ?? props.variant)

const tag = computed(() => (props.clickable && props.to ? 'router-link' : 'span'))

const visible = computed(() => Boolean(props.parentName || props.childName))

const showArrow = computed(() =>
  Boolean(props.childName && props.parentName && props.parentName !== props.childName)
)

const resolvedParent = computed(() => props.parentName || props.childName || '')

const resolvedEmoji = computed(() => {
  if (props.emoji) return props.emoji
  return getUsageColor(props.parentCode).emoji
})

/* S32: 类型图标 — 矢量，结构化（取代 v1 的 emoji 占位 🎯 / 💼） */
const typeIconComp = computed(() => (kind.value === 'occupation' ? ToolOutlined : AimOutlined))

/* 职业 chip 没有 category emoji（数据无 code）— 只在 usage 时显示 */
const showCategoryEmoji = computed(() => kind.value === 'usage' && Boolean(resolvedEmoji.value))

const sizeClass = computed(() => `usage-chip--size-${props.size}`)

const codeClass = computed(() => {
  if (kind.value === 'occupation') return 'usage-chip--code-occupation'
  const code = (props.parentCode || 'default').toLowerCase()
  return `usage-chip--code-${code}`
})

/* a11y: 屏幕阅读器友好 — "用途分类：xxx" / "职业分类：xxx" */
const ariaLabel = computed(() => {
  if (!visible.value) return ''
  const prefix = kind.value === 'occupation' ? '职业分类' : '用途分类'
  const text = resolvedParent.value + (showArrow.value ? ` ${props.childName}` : '')
  const countSuffix = resolvedSkillCount.value != null ? `（${resolvedSkillCount.value} 个 skill）` : ''
  return `${prefix}：${text}${countSuffix}`
})

/* S33: 计数渲染 — null/undefined 不渲染；负数视为 0 */
const resolvedSkillCount = computed<number | null>(() => {
  if (props.skillCount == null) return null
  return Math.max(0, props.skillCount)
})
</script>

<style scoped lang="scss">
/* 在 global.scss .usage-chip 基础上加 pill 形状 + size 变体。
   scoped data-v attr 提供更高优先级，覆盖 global 的 height/padding/font-size。 */
.usage-chip--pill {
  border-radius: 14px !important;
  padding: 2px 12px !important;
  height: auto !important;
  min-height: 24px;
  line-height: 1.4 !important;
  font-size: 13px !important;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  text-decoration: none;
  white-space: nowrap;
  cursor: default;
  transition:
    transform 150ms ease-out,
    box-shadow 150ms ease-out,
    filter 150ms ease-out;
}

.usage-chip--size-sm {
  font-size: 11px !important;
  padding: 1px 8px !important;
  min-height: 20px;
  border-radius: 10px !important;
  gap: 3px;
}

.usage-chip--size-lg {
  font-size: 14px !important;
  padding: 4px 16px !important;
  min-height: 30px;
  border-radius: 16px !important;
  gap: 6px;
}

/* S32: 类型 icon 容器 — 矢量图标，currentColor 跟随 chip 前景色 */
.usage-chip__type-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  line-height: 1;
  opacity: 0.9;
  :deep(svg) {
    width: 1em;
    height: 1em;
    fill: currentColor;
    display: block;
  }
}
.usage-chip--size-sm .usage-chip__type-icon {
  font-size: 12px;
}
.usage-chip--size-md .usage-chip__type-icon {
  font-size: 14px;
}
.usage-chip--size-lg .usage-chip__type-icon {
  font-size: 16px;
}

.usage-chip__emoji {
  font-size: 14px;
  line-height: 1;
}
.usage-chip--size-sm .usage-chip__emoji {
  font-size: 12px;
}
.usage-chip--size-lg .usage-chip__emoji {
  font-size: 16px;
}

.usage-chip__parent {
  font-weight: 500;
}
.usage-chip__sep {
  opacity: 0.55;
  margin: 0 2px;
  font-weight: 400;
}
.usage-chip__child {
  font-weight: 600;
}

/* S33: 计数 chip — 略弱化，对比度通过 opacity 实现 */
.usage-chip__count {
  margin-left: 2px;
  font-weight: 500;
  opacity: 0.75;
  font-variant-numeric: tabular-nums;
}

/* 可点击态：仅当 clickable=true 时启用 hover/focus 反馈 */
.usage-chip--pill.is-clickable {
  cursor: pointer;
}
.usage-chip--pill.is-clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  filter: brightness(1.04);
}
.usage-chip--pill.is-clickable:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .usage-chip--pill {
    transition: none;
  }
  .usage-chip--pill.is-clickable:hover {
    transform: none;
  }
}
</style>
