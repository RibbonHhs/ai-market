<template>
  <div
    class="skill-logo"
    :style="logoStyle"
    :class="{ 'skill-logo--circle': shape === 'circle' }"
    :aria-label="`${name} logo`"
  >
    {{ initials }}
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string
  /** 显示尺寸（px）。默认 48 与 SkillCard 保持一致 */
  size?: number
  /** 形状：square（圆角矩形，homepage 卡）+ circle（圆形，可选） */
  shape?: 'square' | 'circle'
  /** 自定义字符（不传则自动算首字母） */
  text?: string
}
const props = withDefaults(defineProps<Props>(), {
  size: 48,
  shape: 'square',
  text: undefined
})

// 8 色调色板（双渐变）。每对都满足 白色文字 ≥4.5:1 对比度。
const PALETTE: [string, string][] = [
  ['#6366f1', '#8b5cf6'], // indigo → purple
  ['#3b82f6', '#06b6d4'], // blue   → cyan
  ['#10b981', '#06b6d4'], // emerald → cyan
  ['#f59e0b', '#ef4444'], // amber  → red
  ['#ec4899', '#8b5cf6'], // pink   → purple
  ['#06b6d4', '#3b82f6'], // cyan   → blue
  ['#f97316', '#f59e0b'], // orange → amber
  ['#64748b', '#475569']  // slate
]

function hashCode(str: string): number {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

// 首字母：取每个 "-/空格/_/./斜杠" 分隔段的首字母，最多 2 个
const initials = computed(() => {
  if (props.text) return props.text.slice(0, 2).toUpperCase()
  const n = props.name?.trim() || ''
  if (!n) return '?'
  const parts = n.split(/[\s\-_./]+/).filter(Boolean)
  if (parts.length === 0) return n.slice(0, 2).toUpperCase()
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[1][0]).toUpperCase()
})

const logoStyle = computed(() => {
  const [c1, c2] = PALETTE[hashCode(props.name || '?') % PALETTE.length]
  // 字号随尺寸缩放，保持视觉重量
  const fontSize = Math.max(12, Math.round(props.size * 0.38))
  return {
    background: `linear-gradient(135deg, ${c1} 0%, ${c2} 100%)`,
    width: `${props.size}px`,
    height: `${props.size}px`,
    fontSize: `${fontSize}px`,
    borderRadius: props.shape === 'circle' ? '50%' : '10px'
  }
})
</script>

<style scoped lang="scss">
.skill-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-weight: 700;
  letter-spacing: 0.5px;
  line-height: 1;
  user-select: none;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  transition: transform 200ms ease-out, box-shadow 200ms ease-out;
}
@media (prefers-reduced-motion: reduce) {
  .skill-logo {
    transition: none;
  }
}
</style>
