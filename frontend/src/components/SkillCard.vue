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
      <a-tag v-if="skill.featured" color="gold" class="featured-tag" data-testid="skill-featured-tag">★ 精选</a-tag>
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

    <!-- S32: 双 chip 同行（职业 ToolOutlined + 用途 AimOutlined），用 <UsageChip kind> 统一 -->
    <div class="skill-card__categories">
      <UsageChip
        v-if="skill.categoryName"
        kind="occupation"
        :parent-name="skill.categoryName"
        size="sm"
        clickable
        :to="skill.categorySlug ? { name: 'category-browse', params: { slug: skill.categorySlug } } : undefined"
        testid="skill-soc-chip"
      />
      <UsageChip
        v-if="skill.usageCategory"
        kind="usage"
        :parent-code="skill.usageCategory.parentCode"
        :parent-name="skill.usageCategory.parentName"
        :child-name="skill.usageCategory.name"
        size="sm"
        clickable
        :to="skill.usageCategorySlug ? { name: 'category-browse', params: { slug: skill.usageCategorySlug } } : undefined"
        testid="skill-usage-chip"
      />
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
import { useRouter } from 'vue-router'
import SkillLogo from './SkillLogo.vue'
import UsageChip from './UsageChip.vue'
import type { Skill } from '@/types/skill'

const props = defineProps<{ skill: Skill }>()
const router = useRouter()

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
/* S32: 分类 chip 行（职业 ToolOutlined + 用途 AimOutlined，统一用 <UsageChip>） */
.skill-card__categories {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 8px;
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
