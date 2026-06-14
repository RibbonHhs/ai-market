<template>
  <a-layout class="app-layout">
    <AppHeader />
    <a-layout-content class="app-content">
      <section class="newbie-guide" data-testid="newbie-guide">
        <!-- Hero -->
        <header class="newbie-hero">
          <div class="newbie-hero__eyebrow">S36 · 3 分钟上手</div>
          <h1 class="newbie-hero__title">新手指引 · 3 分钟上手 SkillsMap</h1>
          <p class="newbie-hero__sub">
            从「Skill 是什么」到「装到 Claude Code」再到「程序化访问 API」，
            跟着三大区块 3 分钟把 SkillsMap 玩起来。
          </p>
        </header>

        <hr class="newbie-hero__divider" />

        <!-- 锚点导航 -->
        <nav class="anchor-nav" aria-label="快速跳转">
          <a
            v-for="a in anchors"
            :key="a.hash"
            :href="`#${a.hash}`"
            class="anchor-nav__pill"
            :class="{ 'is-active': activeAnchor === a.hash }"
            :data-testid="`newbie-anchor-${a.hash}`"
            @click="onAnchorClick($event, a.hash)"
          >{{ a.label }}</a>
        </nav>

        <!-- §1 Skill 是什么 -->
        <article id="what-is-skill" class="card" data-testid="newbie-section-what">
          <h2>
            <span class="card__no">1</span>
            Skill 是什么
          </h2>
          <p>
            <strong>Agent Skill</strong> 是一段带有 <code>SKILL.md</code> 的可复用提示 / 工具包。
            它告诉大模型 / Agent 在某个特定场景下应该遵循什么指令、调用什么工具、输出什么格式。
            SkillsMap 把这些 Skill 收集、打包、版本化，让你「一次发布，多端复用」。
          </p>
          <p>一个典型的 <code>SKILL.md</code> 形如：</p>
          <pre class="code-block"><code>{{ skillMdExample }}</code></pre>

          <p class="muted">
            运行时配合 <code>manifest.json</code> 还可以声明依赖、入口、版本等元信息。
          </p>
          <pre class="code-block"><code>{{ manifestExample }}</code></pre>
        </article>

        <!-- §2 Skills Manager 使用说明 -->
        <article id="skills-manager" class="card" data-testid="newbie-section-manager">
          <h2>
            <span class="card__no">2</span>
            Skills Manager 使用说明
          </h2>
          <p>
            <strong>skills-manager</strong> 是 SkillsMap 官方出品的客户端 skill。
            装在 Claude Code 里，就能直接在对话中浏览、搜索、下载、同步 SkillsMap 上的 skills。
          </p>

          <ol class="steps">
            <li class="steps__item">
              <span class="steps__num">1</span>
              <div class="steps__body">
                <strong>下载 skill 包</strong>
                <p>点击下面按钮，浏览器会保存 <code>skills-manager.zip</code>。</p>
                <a-button
                  type="primary"
                  :loading="downloading"
                  :disabled="downloading"
                  size="small"
                  data-testid="newbie-download-btn"
                  @click="downloadSkill"
                >
                  {{ downloading ? '下载中…' : '下载 Skill 包' }}
                </a-button>
              </div>
            </li>

            <li class="steps__item">
              <span class="steps__num">2</span>
              <div class="steps__body">
                <strong>解压到 Claude skills 目录</strong>
                <p>把 zip 解压到 <code>~/.claude/skills/skills-manager/</code>：</p>
                <pre class="code-block"><code>{{ unzipCmd }}</code></pre>
              </div>
            </li>

            <li class="steps__item">
              <span class="steps__num">3</span>
              <div class="steps__body">
                <strong>重启 Claude Code</strong>
                <p>新会话开始时 Claude 会自动加载 <code>skills-manager</code>。</p>
              </div>
            </li>

            <li class="steps__item">
              <span class="steps__num">4</span>
              <div class="steps__body">
                <strong>试试这些指令</strong>
                <ul class="prompts">
                  <li>「列出 SkillsMap 上的热门 skills」</li>
                  <li>「搜索包含 claude 的 skill」</li>
                  <li>「下载 skills-manager 这个 skill」</li>
                </ul>
              </div>
            </li>
          </ol>
        </article>

        <!-- §3 API 接入（跳转卡片，不复制 ApiGuideView） -->
        <article id="api-access" class="card card--api" data-testid="newbie-section-api">
          <h2>
            <span class="card__no">3</span>
            API 接入
          </h2>
          <p>
            想程序化访问 SkillsMap？下面是常用端点速览；
            <strong>完整文档（端点列表、参数、响应字段、示例）</strong>
            已在 <code>/api-guide</code> 单独成页。
          </p>

          <ul class="endpoint-chips">
            <li v-for="ep in quickEndpoints" :key="ep.path" class="endpoint-chip">
              <span class="endpoint-chip__method">{{ ep.method }}</span>
              <code class="endpoint-chip__path">{{ ep.path }}</code>
              <span class="endpoint-chip__desc">{{ ep.desc }}</span>
            </li>
          </ul>

          <div class="api-cta">
            <a-button
              type="primary"
              size="large"
              data-testid="newbie-api-cta"
              @click="goApiGuide"
            >
              前往完整 API 接入指南
              <span aria-hidden="true" class="api-cta__arrow">→</span>
            </a-button>
            <p class="api-cta__hint">
              跳转到 <code>/api-guide</code> · 端点总览 / 参数 / 响应字段 / 快速上手示例
            </p>
          </div>
        </article>
      </section>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import AppHeader from '@/components/AppHeader.vue'

const router = useRouter()

// ===== 锚点导航 =====
const anchors = [
  { hash: 'what-is-skill', label: 'Skill 是什么' },
  { hash: 'skills-manager', label: 'Skills Manager 使用说明' },
  { hash: 'api-access', label: 'API 接入' }
]
const activeAnchor = ref<string>('')

function onAnchorClick(e: MouseEvent, hash: string) {
  e.preventDefault()
  const el = document.getElementById(hash)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    history.replaceState(null, '', `#${hash}`)
    activeAnchor.value = hash
  }
}

let observer: IntersectionObserver | null = null
function setupScrollSpy() {
  if (typeof window === 'undefined' || !('IntersectionObserver' in window)) return
  observer = new IntersectionObserver(
    entries => {
      const visible = entries
        .filter(e => e.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
      if (visible.length > 0) {
        activeAnchor.value = visible[0].target.id
      }
    },
    { rootMargin: '-80px 0px -65% 0px', threshold: 0 }
  )
  anchors.forEach(a => {
    const el = document.getElementById(a.hash)
    if (el) observer!.observe(el)
  })
}

onMounted(() => {
  setupScrollSpy()
  const h = window.location.hash.replace('#', '')
  if (h) activeAnchor.value = h
})
onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})

// ===== 代码示例 =====
const skillMdExample = `---
name: my-skill
description: 简短描述这个 skill 是干嘛的
---

# My Skill

当用户说「……」时，按下面的步骤处理：

1. 读取输入
2. 调取相关工具
3. 输出结构化结果
`

const manifestExample = `{
  "name": "my-skill",
  "version": "1.0.0",
  "author": "you@example.com",
  "entry": "SKILL.md"
}`

// ===== §2 下载 =====
const downloading = ref(false)
async function downloadSkill() {
  if (downloading.value) return
  downloading.value = true
  try {
    const resp = await fetch('/api/skills/slug/skills-manager/download')
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
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
const unzipCmd = 'mkdir -p ~/.claude/skills/skills-manager\nunzip skills-manager.zip -d ~/.claude/skills/skills-manager'

// ===== §3 端点速览 =====
const quickEndpoints = [
  { method: 'GET', path: '/api/skills', desc: '列表 / 关键字搜索' },
  { method: 'GET', path: '/api/skills/slug/{slug}', desc: '按 slug 查详情' },
  { method: 'GET', path: '/api/skills/slug/{slug}/download', desc: '下载 .skill zip' },
  { method: 'GET', path: '/api/skills/hot', desc: '热门 skills' },
  { method: 'GET', path: '/api/categories', desc: '全部分类' }
]

function goApiGuide() {
  router.push({ name: 'api-guide' })
}
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}
.app-content {
  background: var(--bg-primary);
}

.newbie-guide {
  max-width: 960px;
  margin: 0 auto;
  padding: 48px 24px 80px;
  color: var(--text-primary);
}
.newbie-hero {
  text-align: center;
  margin-bottom: 24px;
  &__eyebrow {
    color: var(--primary);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    margin-bottom: 8px;
  }
  &__title {
    margin: 0 0 12px;
    font-size: 32px;
    font-weight: 800;
    color: var(--text-primary);
    @media (max-width: 640px) {
      font-size: 24px;
    }
  }
  &__sub {
    margin: 0 auto;
    max-width: 640px;
    color: var(--text-secondary);
    font-size: 15px;
    line-height: 1.7;
  }
  &__divider {
    border: none;
    border-top: 1px solid var(--border);
    margin: 32px 0 24px;
  }
}

// 锚点导航
.anchor-nav {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  margin: 0 0 24px;
  padding: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 999px;
  overflow-x: auto;
  scrollbar-width: thin;
  -webkit-overflow-scrolling: touch;
  max-width: 100%;
  min-width: 0;
  &__pill {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    padding: 6px 16px;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 150ms ease-out;
    white-space: nowrap;
    &:hover {
      background: var(--bg-tertiary);
      color: var(--primary);
    }
    &.is-active {
      background: var(--bg-tertiary);
      color: var(--primary);
    }
  }
}

// 卡片
.card {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 24px;
  scroll-margin-top: 24px;
  box-shadow: var(--shadow-sm);
  h2 {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
  p {
    margin: 0 0 12px;
    color: var(--text-primary);
    font-size: 14px;
    line-height: 1.7;
    strong {
      color: var(--text-primary);
    }
    code {
      background: var(--bg-tertiary);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'SF Mono', Menlo, Consolas, monospace;
      color: var(--primary);
      font-size: 0.9em;
    }
  }
  .muted {
    color: var(--text-tertiary);
    font-size: 13px;
  }
  @media (prefers-color-scheme: dark) {
    background: var(--bg-secondary);
  }
}
.card__no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--text-inverse);
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.code-block {
  margin: 0 0 16px;
  padding: 14px 16px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.65;
  code {
    color: inherit;
    background: transparent;
    padding: 0;
  }
  @media (prefers-color-scheme: dark) {
    background: var(--bg-elevated);
  }
}

// §2 步骤
.steps {
  list-style: none;
  margin: 0;
  padding: 0;
  &__item {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 14px 0;
    border-top: 1px solid var(--border);
    &:first-child {
      border-top: none;
      padding-top: 4px;
    }
  }
  &__num {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--bg-tertiary);
    color: var(--text-primary);
    font-weight: 700;
    font-size: 13px;
    flex-shrink: 0;
    @media (prefers-color-scheme: dark) {
      background: var(--primary);
      color: var(--text-inverse);
    }
  }
  &__body {
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
      font-size: 13px;
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
}
.prompts {
  list-style: disc;
  padding-left: 18px;
  margin: 4px 0 0;
  li {
    padding: 1px 0;
    color: var(--text-secondary);
    font-size: 13px;
    line-height: 1.6;
  }
}

// §3 跳转卡片
.card--api {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.06) 0%, rgba(79, 70, 229, 0.06) 100%);
  border: 1px solid var(--primary-border);
  @media (prefers-color-scheme: dark) {
    background: linear-gradient(135deg, rgba(167, 139, 250, 0.12) 0%, rgba(79, 70, 229, 0.10) 100%);
  }
}
.endpoint-chips {
  list-style: none;
  margin: 12px 0 20px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.endpoint-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
  flex-wrap: wrap;
  @media (prefers-color-scheme: dark) {
    background: var(--bg-elevated);
  }
  &__method {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    background: var(--primary-bg);
    color: var(--primary);
    font-weight: 700;
    font-size: 11px;
    letter-spacing: 0.04em;
    flex-shrink: 0;
  }
  &__path {
    font-family: 'SF Mono', Menlo, Consolas, monospace;
    font-size: 12px;
    color: var(--text-primary);
    background: var(--bg-tertiary);
    padding: 2px 6px;
    border-radius: 4px;
    flex-shrink: 0;
  }
  &__desc {
    color: var(--text-secondary);
    font-size: 12px;
  }
}
.api-cta {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
  &__hint {
    margin: 0;
    color: var(--text-tertiary);
    font-size: 12px;
    code {
      background: var(--bg-tertiary);
      padding: 1px 5px;
      border-radius: 3px;
      font-family: 'SF Mono', Menlo, Consolas, monospace;
      color: var(--text-secondary);
      font-size: 11px;
    }
  }
  &__arrow {
    margin-left: 6px;
    font-size: 16px;
    line-height: 1;
  }
}

@media (max-width: 640px) {
  .newbie-guide {
    padding: 24px 16px 48px;
  }
  .card {
    padding: 18px 18px;
  }
  .code-block {
    // 长行代码块缩字号 + 横向滚动
    font-size: 12px;
    padding: 10px 12px;
  }
}
</style>
