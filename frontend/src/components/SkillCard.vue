<template>
  <a-card hoverable class="skill-card" data-testid="skill-card" @click="onClick">
    <div class="skill-card__head">
      <SkillLogo :name="skill.name" :size="48" />
      <div class="skill-card__title-area">
        <a-tooltip :title="skill.name">
          <h3 class="skill-card__title">{{ skill.displayName || skill.name }}</h3>
        </a-tooltip>
        <div class="skill-card__meta">
          <span v-if="skill.authorName" class="meta-item">👤 {{ skill.authorName }}</span>
          <span v-if="skill.version" class="meta-item">v{{ skill.version }}</span>
        </div>
      </div>
      <a-tag v-if="skill.featured" color="gold" class="featured-tag" data-testid="skill-soc-chip">★ 精选</a-tag>
    </div>

    <!-- Sprint S02: 来源徽章 -->
    <div class="skill-card__source">
      <a-tooltip v-if="skill.sourceType === 'GIT_URL'" :title="skill.sourceUrl">
        <a-tag color="geekblue">
          🔗 Git @ {{ skill.sourceRef || 'main' }}
        </a-tag>
      </a-tooltip>
      <a-tag v-else-if="skill.sourceType === 'LOCAL_FILE'" color="default">📄 .md</a-tag>
      <a-tag v-else-if="skill.sourceType === 'LOCAL_ZIP'" color="default">📦 本地</a-tag>
    </div>

    <p class="skill-card__desc">{{ truncate(skill.description, 120) }}</p>

    <!-- S24: 分类 chip（SOC 职业 + USAGE 用途） -->
    <div class="skill-card__categories">
      <a-tooltip v-if="skill.categoryName" :title="`职业：${skill.categoryName}`">
        <a-tag color="blue" class="cat-chip">
          <span class="cat-chip__label">职业</span>
          <span class="cat-chip__name">{{ skill.categoryName }}</span>
        </a-tag>
      </a-tooltip>
      <a-tooltip v-if="usageChip" :title="`用途：${usageChip.fullLabel}`">
        <a-tag
          :class="['cat-chip', 'cat-chip--usage', usageChip.modifierClass]"
          data-testid="skill-usage-chip"
        >
          <span class="cat-chip__emoji">{{ usageChip.emoji }}</span>
          <span class="cat-chip__label">用途</span>
          <span class="cat-chip__name">{{ usageChip.shortLabel }}</span>
        </a-tag>
      </a-tooltip>
    </div>

    <div class="skill-card__tags">
      <a-tag v-for="t in (skill.tags || []).slice(0, 3)" :key="t" color="blue">{{ t }}</a-tag>
    </div>

    <div class="skill-card__footer">
      <a-rate :value="skill.ratingAvg || 0" disabled allow-half :count="5" />
      <span class="rating-text">
        {{ (skill.ratingAvg || 0).toFixed(1) }}
        <span class="muted">({{ skill.ratingCount || 0 }})</span>
      </span>
      <span class="installs">⬇ {{ formatNumber(skill.installs || 0) }}</span>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import SkillLogo from './SkillLogo.vue'
import type { Skill } from '@/types/skill'
import { getUsageColor } from '@/constants/usage-colors'

const props = defineProps<{ skill: Skill }>()
const router = useRouter()

/** S24 + S25: USAGE chip 派生（按 parentCode 取色，CSS 变量驱动主题切换） */
const usageChip = computed(() => {
  const u = props.skill.usageCategory
  if (!u) return null
  const color = getUsageColor(u.parentCode)
  const shortLabel = u.parentName && u.name && u.parentName !== u.name
    ? `${u.parentName}·${u.name}`
    : (u.parentName || u.name || '未分类')
  // S25: className 模式（CSS 变量驱动 bg/fg，自动跟 prefers-color-scheme）
  const modifierClass = u.parentCode
    ? `cat-chip--usage-${u.parentCode.toLowerCase()}`
    : 'cat-chip--usage-default'
  return {
    bg: color.bg,
    fg: color.fg,
    emoji: color.emoji,
    shortLabel,
    fullLabel: u.description ? `${shortLabel} — ${u.description}` : shortLabel,
    modifierClass
  }
})

function onClick() {
  router.push({ name: 'skill-detail', params: { slug: props.skill.slug } })
}

function truncate(s: string, n: number) {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '…' : s
}

function formatNumber(n: number) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
</script>

<style scoped lang="scss">
.skill-card {
  cursor: pointer;
  border-radius: 10px;
  height: 100%;
  :deep(.ant-card-body) {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 16px;
  }
}
.skill-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.skill-card__title-area {
  flex: 1;
  min-width: 0;
}
.skill-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.skill-card__meta {
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  gap: 8px;
  margin-top: 2px;
  .meta-item {
    &:not(:last-child)::after {
      content: ' · ';
      margin-left: 4px;
      color: var(--text-tertiary);
    }
  }
}
.featured-tag {
  flex-shrink: 0;
}
.skill-card__source {
  margin-bottom: 8px;
  :deep(.ant-tag) {
    font-size: 11px;
    padding: 0 6px;
    line-height: 20px;
  }
}
.skill-card__desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 4px 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}
.skill-card__tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
/* S24: 分类 chip 行（SOC + USAGE） */
.skill-card__categories {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  :deep(.cat-chip) {
    font-size: 11px;
    padding: 0 8px;
    line-height: 22px;
    border-radius: 4px;
    margin: 0;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
  :deep(.cat-chip__label) {
    font-size: 10px;
    opacity: 0.7;
    margin-right: 2px;
  }
  :deep(.cat-chip__emoji) {
    font-size: 12px;
    line-height: 1;
  }
  :deep(.cat-chip__name) {
    font-weight: 500;
  }
  :deep(.cat-chip--usage) {
    font-weight: 500;
    border: 1px solid transparent;
    /* S25: 走 CSS 变量，自动随主题切换（light → dark） */
    background: var(--usage-bg);
    color: var(--usage-fg);
  }
  /* S25: 12 个 USAGE 一级 className（kebab-case 化） */
  :deep(.cat-chip--usage-purpose-tool)        { --usage-bg: #F0F5FF; --usage-fg: #1D39C4; }
  :deep(.cat-chip--usage-purpose-biz)         { --usage-bg: #FFF7E6; --usage-fg: #AD4E00; }
  :deep(.cat-chip--usage-purpose-dev)         { --usage-bg: #E6FFFB; --usage-fg: #006D75; }
  :deep(.cat-chip--usage-purpose-qasec)       { --usage-bg: #F9F0FF; --usage-fg: #391085; }
  :deep(.cat-chip--usage-purpose-ai)          { --usage-bg: #FFF0F6; --usage-fg: #9E1068; }
  :deep(.cat-chip--usage-purpose-devops)      { --usage-bg: #FFF2E8; --usage-fg: #A8071A; }
  :deep(.cat-chip--usage-purpose-doc)         { --usage-bg: #FCFFE6; --usage-fg: #435106; }
  :deep(.cat-chip--usage-purpose-media)       { --usage-bg: #E6FAFF; --usage-fg: #003A8C; }
  :deep(.cat-chip--usage-purpose-research)    { --usage-bg: #F0FBE6; --usage-fg: #135200; }
  :deep(.cat-chip--usage-purpose-life)        { --usage-bg: #FFF1F0; --usage-fg: #820014; }
  :deep(.cat-chip--usage-purpose-db)          { --usage-bg: #F4FFB8; --usage-fg: #874D00; }
  :deep(.cat-chip--usage-purpose-blockchain)  { --usage-bg: #FFE7BA; --usage-fg: #874D00; }
  :deep(.cat-chip--usage-default)             { --usage-bg: #F5F5F5; --usage-fg: #595959; }

  /* S25: 暗色模式（跟随系统 + data-theme="dark" 钩子） */
  :deep(.dark) .cat-chip--usage-purpose-tool        { --usage-bg: #0F1B3D; --usage-fg: #ADC6FF; }
  :deep(.dark) .cat-chip--usage-purpose-biz         { --usage-bg: #3D2200; --usage-fg: #FFD591; }
  :deep(.dark) .cat-chip--usage-purpose-dev         { --usage-bg: #003D40; --usage-fg: #87E8DE; }
  :deep(.dark) .cat-chip--usage-purpose-qasec       { --usage-bg: #220F3D; --usage-fg: #D3ADF7; }
  :deep(.dark) .cat-chip--usage-purpose-ai          { --usage-bg: #3D0029; --usage-fg: #FFADD2; }
  :deep(.dark) .cat-chip--usage-purpose-devops      { --usage-bg: #3D0F0F; --usage-fg: #FFA39E; }
  :deep(.dark) .cat-chip--usage-purpose-doc         { --usage-bg: #1F2600; --usage-fg: #EAFF8F; }
  :deep(.dark) .cat-chip--usage-purpose-media       { --usage-bg: #001D3D; --usage-fg: #85C5FF; }
  :deep(.dark) .cat-chip--usage-purpose-research    { --usage-bg: #0F3D00; --usage-fg: #B7EB8F; }
  :deep(.dark) .cat-chip--usage-purpose-life        { --usage-bg: #3D0011; --usage-fg: #FFA39E; }
  :deep(.dark) .cat-chip--usage-purpose-db          { --usage-bg: #3D2E00; --usage-fg: #FFE066; }
  :deep(.dark) .cat-chip--usage-purpose-blockchain  { --usage-bg: #3D2E00; --usage-fg: #FFD666; }
  :deep(.dark) .cat-chip--usage-default             { --usage-bg: #1F1F1F; --usage-fg: #BFBFBF; }

  @media (prefers-color-scheme: dark) {
    :deep(.cat-chip--usage-purpose-tool)        { --usage-bg: #0F1B3D; --usage-fg: #ADC6FF; }
    :deep(.cat-chip--usage-purpose-biz)         { --usage-bg: #3D2200; --usage-fg: #FFD591; }
    :deep(.cat-chip--usage-purpose-dev)         { --usage-bg: #003D40; --usage-fg: #87E8DE; }
    :deep(.cat-chip--usage-purpose-qasec)       { --usage-bg: #220F3D; --usage-fg: #D3ADF7; }
    :deep(.cat-chip--usage-purpose-ai)          { --usage-bg: #3D0029; --usage-fg: #FFADD2; }
    :deep(.cat-chip--usage-purpose-devops)      { --usage-bg: #3D0F0F; --usage-fg: #FFA39E; }
    :deep(.cat-chip--usage-purpose-doc)         { --usage-bg: #1F2600; --usage-fg: #EAFF8F; }
    :deep(.cat-chip--usage-purpose-media)       { --usage-bg: #001D3D; --usage-fg: #85C5FF; }
    :deep(.cat-chip--usage-purpose-research)    { --usage-bg: #0F3D00; --usage-fg: #B7EB8F; }
    :deep(.cat-chip--usage-purpose-life)        { --usage-bg: #3D0011; --usage-fg: #FFA39E; }
    :deep(.cat-chip--usage-purpose-db)          { --usage-bg: #3D2E00; --usage-fg: #FFE066; }
    :deep(.cat-chip--usage-purpose-blockchain)  { --usage-bg: #3D2E00; --usage-fg: #FFD666; }
    :deep(.cat-chip--usage-default)             { --usage-bg: #1F1F1F; --usage-fg: #BFBFBF; }
  }
}
.skill-card__footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  border-top: 1px solid var(--border-color);
  padding-top: 8px;
  :deep(.ant-rate) {
    font-size: 12px;
  }
  .rating-text {
    font-weight: 600;
    color: #faad14;
  }
  .muted {
    color: var(--text-tertiary);
    font-weight: 400;
    margin-left: 2px;
  }
  .installs {
    margin-left: auto;
    color: var(--text-tertiary);
  }
}
</style>
