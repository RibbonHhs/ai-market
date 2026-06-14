<template>
  <section class="home-stats" data-testid="home-stats">
    <div class="home-stats__inner">
      <!-- 左侧 banner：第一次使用 Skill? → 新手指引（S36 更新文案 + 跳转目标） -->
      <div class="home-stats__cta">
        <div class="home-stats__cta-icon" aria-hidden="true">M</div>
        <div class="home-stats__cta-body">
          <h3 class="home-stats__cta-title">第一次使用 Skill?</h3>
          <p class="home-stats__cta-desc">
            不知道从哪开始？3 分钟带你了解 Skills 是什么、怎么安装、怎么用 API 接入。
          </p>
        </div>
        <button
          type="button"
          class="home-stats__cta-btn"
          data-testid="home-stats-cta"
          @click="onStart"
        >
          开始新手指引 →
        </button>
      </div>

      <!-- 右侧 3 列数字 -->
      <div class="home-stats__grid">
        <div class="home-stats__cell">
          <div class="home-stats__num">{{ formattedTotal }}</div>
          <div class="home-stats__label">可用 Skills</div>
        </div>
        <div class="home-stats__cell">
          <div class="home-stats__num">{{ platformCount }}+</div>
          <div class="home-stats__label">支持平台</div>
        </div>
        <div class="home-stats__cell">
          <div class="home-stats__num">{{ safetyCheck }}</div>
          <div class="home-stats__label">安全检查</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

interface Props {
  totalSkills: number
  platformCount?: number
  safetyCheck?: string
}
const props = withDefaults(defineProps<Props>(), {
  platformCount: 50,
  safetyCheck: 'CL5'
})

const router = useRouter()
const formattedTotal = computed(() => props.totalSkills.toLocaleString('en-US'))

function onStart() {
  // S36: 跳转目标从 /browse 改为 /newbie-guide（HomeStats 旧 CTA 改承担新手指引入口）
  router.push({ name: 'newbie-guide' })
}
</script>

<style scoped lang="scss">
.home-stats {
  padding: 24px 0 48px;
  &__inner {
    max-width: 1280px;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: 1.4fr 1fr;
    gap: 24px;
    align-items: stretch;
  }
  &__cta {
    display: flex;
    align-items: center;
    gap: 20px;
    background: var(--bg-primary);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 20px 24px;
    box-shadow: var(--shadow-sm);
  }
  &__cta-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: linear-gradient(135deg, #1e40af 0%, #7c3aed 100%);
    color: #fff;
    font-size: 20px;
    font-weight: 900;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  &__cta-body {
    flex: 1;
    min-width: 0;
  }
  &__cta-title {
    margin: 0 0 4px;
    font-size: 16px;
    font-weight: 700;
    color: var(--text-primary);
  }
  &__cta-desc {
    margin: 0;
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.5;
  }
  &__cta-btn {
    padding: 8px 16px;
    border: 1px solid var(--primary);
    border-radius: 999px;
    background: var(--bg-elevated);
    color: var(--primary);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 150ms ease-out;
    &:hover {
      background: var(--primary);
      color: var(--text-inverse);
    }
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  &__cell {
    background: var(--bg-primary);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 20px;
    text-align: center;
    box-shadow: var(--shadow-sm);
  }
  &__num {
    font-size: 32px;
    font-weight: 900;
    color: var(--text-primary);
    line-height: 1;
    margin-bottom: 6px;
    font-variant-numeric: tabular-nums;
  }
  &__label {
    font-size: 12px;
    color: var(--text-secondary);
    font-weight: 500;
  }
}

@media (max-width: 768px) {
  .home-stats__inner {
    grid-template-columns: 1fr;
  }
  .home-stats__cta {
    flex-direction: column;
    align-items: flex-start;
  }
  .home-stats__cta-btn {
    align-self: stretch;
    text-align: center;
  }
}
</style>
