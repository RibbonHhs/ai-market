<template>
  <section class="home-hero" data-testid="home-hero">
    <div class="home-hero__backdrop" aria-hidden="true"></div>
    <div class="home-hero__inner">
      <div class="home-hero__eyebrow">SKILLSMAP / SKILLS MARKETPLACE</div>
      <h1 class="home-hero__title">Skills Marketplace</h1>
      <p class="home-hero__lede">
        发现与分享 Claude Code 及所有 SKILL.md 工具的开源 Agent Skills。
      </p>

      <!-- 居中搜索卡 -->
      <div class="home-hero__search-card">
        <div class="home-hero__tabs">
          <button
            type="button"
            class="home-hero__tab"
            :class="{ 'is-active': activeTab === 'assist' }"
            @click="activeTab = 'assist'"
            data-testid="home-tab-human"
          >
            <span aria-hidden="true">👤</span>
            <span>我是人类</span>
          </button>
          <button
            type="button"
            class="home-hero__tab"
            :class="{ 'is-active': activeTab === 'agent' }"
            @click="activeTab = 'agent'"
            data-testid="home-tab-agent"
          >
            <span aria-hidden="true">🤖</span>
            <span>我是智能体</span>
          </button>
        </div>

        <!-- 我是人类：搜索框 + 热门 chips -->
        <template v-if="activeTab === 'assist'">
          <div class="home-hero__input-row">
            <span class="home-hero__search-icon" aria-hidden="true">⌕</span>
            <input
              v-model="keyword"
              type="text"
              class="home-hero__input"
              placeholder="搜索 Skills"
              @keydown.enter="onSearch"
            />
            <button type="button" class="home-hero__search-btn" @click="onSearch">
              <span aria-hidden="true">✦</span>
              <span>搜索</span>
            </button>
          </div>

          <div class="home-hero__chips">
            <a-empty
              v-if="!chipSkills.length"
              :image="emptyImg"
              description="热门 skills 加载中…"
              class="home-hero__chips-empty"
            />
            <template v-else>
              <button
                v-show="canScrollLeft"
                type="button"
                class="home-hero__chip-arrow home-hero__chip-arrow--left"
                aria-label="向左滑动"
                @click="scrollBy(-1)"
              >
                <CaretLeftOutlined />
              </button>
              <div ref="rowRef" class="home-hero__chips-row" @scroll="onScroll">
                <button
                  v-for="s in chipSkills"
                  :key="s.id"
                  type="button"
                  class="home-hero__chip"
                  :title="s.displayName || s.name"
                  @click="onChip(s)"
                >
                  <SkillLogo :name="s.name" :size="22" />
                  <span class="home-hero__chip-name">{{ s.displayName || s.name }}</span>
                </button>
              </div>
              <button
                v-show="canScrollRight"
                type="button"
                class="home-hero__chip-arrow home-hero__chip-arrow--right"
                aria-label="向右滑动"
                @click="scrollBy(1)"
              >
                <CaretRightOutlined />
              </button>
            </template>
          </div>
        </template>

        <!-- 我是智能体：skills-manager 说明 + 下载 -->
        <div v-else class="home-hero__agent">
          <div class="home-hero__agent-intro">
            <h3>🤖 skills-manager</h3>
            <p>
              SkillsMap 官方出品的客户端 skill。装在 Claude Code 里，就能直接在对话中浏览、搜索、下载、同步 SkillsMap 平台上的 skills。
            </p>
          </div>

          <ol class="home-hero__agent-steps">
            <li>
              <span class="home-hero__agent-num">1</span>
              <div class="home-hero__agent-body">
                <strong>下载 skill 包</strong>
                <a-button
                  type="primary"
                  :loading="downloading"
                  :disabled="downloading"
                  size="small"
                  @click="downloadSkill"
                >
                  {{ downloading ? '下载中…' : '下载 Skill 包' }}
                </a-button>
              </div>
            </li>
            <li>
              <span class="home-hero__agent-num">2</span>
              <div class="home-hero__agent-body">
                <strong>解压到 Claude skills 目录</strong>
                <p>把 zip 解压到 <code>~/.claude/skills/skills-manager/</code></p>
                <pre class="home-hero__agent-cmd"><code>mkdir -p ~/.claude/skills/skills-manager
unzip skills-manager.zip -d ~/.claude/skills/skills-manager</code></pre>
              </div>
            </li>
            <li>
              <span class="home-hero__agent-num">3</span>
              <div class="home-hero__agent-body">
                <strong>重启 Claude Code</strong>
                <p>新会话开始时 Claude 会自动加载 <code>skills-manager</code>。</p>
              </div>
            </li>
            <li>
              <span class="home-hero__agent-num">4</span>
              <div class="home-hero__agent-body">
                <strong>试试这些指令</strong>
                <ul class="home-hero__agent-prompts">
                  <li>「列出 SkillsMap 上的热门 skills」</li>
                  <li>「搜索包含 claude 的 skill」</li>
                  <li>「下载 skills-manager 这个 skill」</li>
                </ul>
              </div>
            </li>
          </ol>

          <p class="home-hero__agent-foot">
            想看完整 API？<router-link to="/api-guide">前往 API 接入指南</router-link>
          </p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CaretLeftOutlined, CaretRightOutlined } from '@ant-design/icons-vue'
import SkillLogo from '../SkillLogo.vue'
import { track } from '@/utils/track'
import type { Skill } from '@/types/skill'

interface Props {
  topSkills?: Skill[]
}
const props = withDefaults(defineProps<Props>(), { topSkills: () => [] })

const router = useRouter()
const activeTab = ref<'assist' | 'agent'>('assist')
const keyword = ref('')
const emptyImg = ref(undefined as unknown) // 隐藏 a-empty 默认图
const rowRef = ref<HTMLDivElement | null>(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)
const downloading = ref(false)

// 取前 6 个热门 skill，单行
const chipSkills = computed(() => (props.topSkills || []).slice(0, 6))

function onScroll() {
  updateArrows()
}

function updateArrows() {
  const el = rowRef.value
  if (!el) return
  canScrollLeft.value = el.scrollLeft > 4
  canScrollRight.value =
    el.scrollLeft + el.clientWidth < el.scrollWidth - 4
}

function scrollBy(dir: number) {
  const el = rowRef.value
  if (!el) return
  // 每次滑约 70% 容器宽（或最小 200px）
  const delta = Math.max(200, el.clientWidth * 0.7) * dir
  el.scrollBy({ left: delta, behavior: 'smooth' })
}

function onResize() {
  // 等待数据/样式 ready 后再算
  nextTick(updateArrows)
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  // 监听 chipSkills 变化（数据到达）
  nextTick(updateArrows)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

function onSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  router.push({ name: 'browse', query: { keyword: kw } })
}

function onChip(s: Skill) {
  // 点击 chip → 跳到该 skill 详情（更符合"热门 skills 入口"语义）
  if (s.slug) {
    router.push({ name: 'skill-detail', params: { slug: s.slug } })
  } else if (s.name) {
    router.push({ name: 'browse', query: { keyword: s.name } })
  }
}

async function downloadSkill() {
  if (downloading.value) return
  downloading.value = true
  // S22: 埋点 — 下载按钮点击
  track('skills_manager_download_click', { slug: 'skills-manager' })
  try {
    const resp = await fetch('/api/skills/slug/skills-manager/download')
    if (!resp.ok) {
      throw new Error(`HTTP ${resp.status}`)
    }
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'skills-manager.zip'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    message.success('skills-manager.zip 已下载')
  } catch (e) {
    message.error('下载失败：' + (e instanceof Error ? e.message : String(e)))
  } finally {
    downloading.value = false
  }
}

// S22: 埋点 — 智能体 tab PV（切换到 agent 时上报一次）
watch(activeTab, (val, oldVal) => {
  if (val === 'agent' && oldVal !== 'agent') {
    track('home_agent_tab_view', { source: 'home_hero' })
  }
})
</script>

<style scoped lang="scss">
.home-hero {
  position: relative;
  padding: 72px 0 80px;
  text-align: center;
  isolation: isolate;
  &__backdrop {
    position: absolute;
    inset: 0;
    z-index: -1;
    background: linear-gradient(180deg, #f3f0ff 0%, #eef2ff 60%, transparent 100%);
    pointer-events: none;
    // 暗色态：嵌套在 &__backdrop 内，避免 Vue scoped CSS 把 .home-hero 插到 selector 前面破坏 :root
    @media (prefers-color-scheme: dark) {
      background: linear-gradient(180deg, var(--bg-primary) 0%, var(--bg-secondary) 60%, transparent 100%);
    }
  }
  &__inner {
    max-width: 880px;
    margin: 0 auto;
    padding: 0 24px;
  }
  &__eyebrow {
    display: inline-block;
    color: #7c3aed;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 2px;
    margin-bottom: 20px;
    text-transform: uppercase;
  }
  &__title {
    font-size: 64px;
    font-weight: 900;
    color: var(--text-primary);
    line-height: 1.05;
    margin: 0 0 20px;
    letter-spacing: -0.03em;
  }
  &__lede {
    font-size: 18px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin: 0 0 40px;
    max-width: 640px;
    margin-left: auto;
    margin-right: auto;
  }

  &__search-card {
    background: var(--bg-elevated);
    border-radius: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04), 0 8px 24px rgba(0, 0, 0, 0.06);
    padding: 28px 32px;
    max-width: 800px;
    margin: 0 auto;
  }
  &__tabs {
    display: flex;
    gap: 10px;
    justify-content: center;
    margin-bottom: 20px;
  }
  &__tab {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 22px;
    border: none;
    border-radius: 999px;
    background: transparent;
    color: var(--text-secondary);
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 150ms ease-out;
    &:hover {
      background: var(--bg-tertiary);
    }
    &.is-active {
      background: var(--bg-tertiary);
      color: var(--primary);
    }
  }
  &__input-row {
    display: flex;
    align-items: center;
    gap: 10px;
    background: var(--bg-tertiary);
    border: 2px solid #e2e8f0;
    border-radius: 999px;
    padding: 6px 6px 6px 22px;
    transition: border-color 150ms ease-out;
    &:focus-within {
      border-color: #7c3aed;
      background: var(--bg-primary);
    }
  }
  &__search-icon {
    color: var(--text-tertiary);
    font-size: 22px;
  }
  &__input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 18px;
    color: var(--text-primary);
    padding: 12px 0;
    &::placeholder {
      color: var(--text-tertiary);
    }
  }
  &__search-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 12px 28px;
    border: none;
    border-radius: 999px;
    background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%);
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: transform 150ms ease-out, box-shadow 150ms ease-out;
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);
    }
  }

  &__chips {
    position: relative;
    margin-top: 16px;
    &-empty {
      padding: 8px 0;
      :deep(.ant-empty-description) {
        color: var(--text-tertiary);
        font-size: 12px;
      }
    }
  }
  &__chips-row {
    display: flex;
    flex-wrap: nowrap;
    justify-content: flex-start; // 多于 6 个时箭头接管
    gap: 6px;
    overflow-x: hidden; // 默认隐藏滚动条
    padding: 4px 28px; // 留出箭头位
    scroll-behavior: smooth;
  }
  &__chip-arrow {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    z-index: 2;
    width: 24px;
    height: 24px;
    border: 1px solid #e2e8f0;
    border-radius: 50%;
    background: var(--bg-primary);
    color: var(--text-secondary);
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
    transition: all 150ms ease-out;
    &:hover {
      border-color: #7c3aed;
      color: #7c3aed;
    }
    &--left {
      left: 0;
    }
    &--right {
      right: 0;
    }
  }
  &__chip {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 4px 10px 4px 5px;
    border: 1px solid #e2e8f0;
    border-radius: 999px;
    background: var(--bg-primary);
    color: var(--text-primary);
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 150ms ease-out;
    white-space: nowrap;
    flex-shrink: 0;
    &:hover {
      border-color: #7c3aed;
      background: var(--bg-tertiary);
      color: #7c3aed;
    }
  }
  &__chip-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 80px;
  }

  // 我是智能体 tab 内容（浅色态保留紫粉渐变；暗色态改用 token）
  &__agent {
    text-align: left;
    background: linear-gradient(180deg, #faf5ff 0%, #fff 100%);
    border: 1px solid #f1e8ff;
    border-radius: 14px;
    padding: 22px 24px;
    // 暗色态：嵌套避免 Vue scoped CSS 破坏 selector
    @media (prefers-color-scheme: dark) {
      background: var(--bg-secondary);
      border-color: var(--border);
    }
  }
  &__agent-intro {
    margin-bottom: 18px;
    h3 {
      margin: 0 0 6px;
      font-size: 18px;
      font-weight: 800;
      color: var(--text-primary);
    }
    p {
      margin: 0;
      color: var(--text-secondary);
      font-size: 13px;
      line-height: 1.65;
    }
  }
  &__agent-steps {
    list-style: none;
    margin: 0;
    padding: 0;
    > li {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 12px 0;
      border-top: 1px solid #f1f5f9;
      &:first-child {
        border-top: none;
        padding-top: 4px;
      }
    }
  }
  &__agent-num {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    border-radius: 50%;
    background: var(--bg-tertiary);
    color: var(--text-primary);
    font-weight: 700;
    font-size: 13px;
    flex-shrink: 0;
    // 暗色态：紫底深字（--primary + --text-inverse）— 嵌套避免 Vue scoped 破坏
    @media (prefers-color-scheme: dark) {
      background: var(--primary);
      color: var(--text-inverse);
    }
  }
  &__agent-body {
    flex: 1;
    min-width: 0;
    strong {
      display: block;
      font-size: 14px;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 4px;
    }
    p {
      margin: 0 0 4px;
      color: var(--text-secondary);
      font-size: 12px;
      line-height: 1.6;
      code {
        background: var(--bg-tertiary);
        padding: 1px 5px;
        border-radius: 3px;
        font-family: 'SF Mono', Menlo, Consolas, monospace;
        color: var(--primary);
        font-size: 11px;
      }
    }
  }
  &__agent-cmd {
    margin: 6px 0 0;
    padding: 8px 10px;
    background: var(--bg-elevated);
    color: var(--text-primary);
    border: 1px solid var(--border);
    border-radius: 5px;
    font-family: 'SF Mono', Menlo, Consolas, monospace;
    font-size: 11px;
    line-height: 1.6;
    overflow-x: auto;
    code {
      color: inherit;
      background: transparent;
      padding: 0;
    }
  }
  &__agent-prompts {
    list-style: disc;
    padding-left: 18px;
    margin: 4px 0 0;
    li {
      padding: 1px 0;
      color: var(--text-secondary);
      font-size: 12px;
      line-height: 1.6;
    }
  }
  &__agent-foot {
    margin: 14px 0 0;
    padding-top: 12px;
    border-top: 1px dashed var(--border);
    font-size: 12px;
    color: var(--text-secondary);
    a {
      color: var(--link);
      font-weight: 600;
      text-decoration: none;
      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 640px) {
  .home-hero {
    padding: 40px 0 48px;
    &__title {
      font-size: 40px;
    }
    &__lede {
      font-size: 14px;
      margin-bottom: 28px;
    }
    &__search-card {
      padding: 20px 16px;
    }
    &__input {
      font-size: 15px;
    }
    &__chip-name {
      max-width: 56px;
    }
  }
}
</style>
