<template>
  <a-layout class="app-layout">
    <AppHeader />
    <a-layout-content class="app-content">
      <section class="api-guide">
        <header class="hero">
          <div class="hero__eyebrow">S21 · 编程接入</div>
          <h1>SkillsMap REST API 接入指南</h1>
          <p class="hero__sub">
            通过公开 REST API 编程访问 SkillsMap 上的 Skills 数据。支持关键字搜索、SOC 分类筛选、SOC 职业维度筛选、分页与排序。
          </p>
          <p class="hero__meta">
            基础 URL：<code>{{ baseUrl }}</code> · 完整文档：
            <a :href="`${baseUrl}/doc.html`" target="_blank" rel="noopener">Knife4j /doc.html</a>
          </p>
        </header>

        <hr class="hero__divider" />

        <!-- S22: 顶部锚点快速跳转 -->
        <nav class="anchor-nav" aria-label="快速跳转">
          <a
            v-for="a in anchors"
            :key="a.hash"
            :href="`#${a.hash}`"
            class="anchor-nav__pill"
            :class="{ 'is-active': activeAnchor === a.hash }"
            @click="onAnchorClick($event, a.hash)"
          >{{ a.label }}</a>
        </nav>

        <!-- 端点总览 -->
        <article id="endpoint-list" class="card">
          <h2>1. 端点总览</h2>
          <a-table
            :data-source="endpoints"
            :columns="endpointColumns"
            :pagination="false"
            size="middle"
            row-key="path"
          />
        </article>

        <!-- GET /api/skills 参数说明 -->
        <article id="params" class="card">
          <h2>2. <code>GET /api/skills</code> 参数说明</h2>
          <a-table
            :data-source="queryParams"
            :columns="paramColumns"
            :pagination="false"
            size="middle"
            row-key="name"
          />
        </article>

        <!-- 响应字段 -->
        <article id="response" class="card">
          <h2>3. 响应字段（SkillVO 核心字段）</h2>
          <a-table
            :data-source="responseFields"
            :columns="fieldColumns"
            :pagination="false"
            size="middle"
            row-key="name"
          />
        </article>

        <!-- 示例 -->
        <article id="examples" class="card">
          <h2>4. 快速上手示例</h2>
          <p class="muted">点击右上角「拷贝」按钮可复制完整命令。</p>

          <div v-for="(ex, idx) in examples" :key="idx" class="example">
            <div class="example__head">
              <span class="example__no">#{{ idx + 1 }}</span>
              <span class="example__title">{{ ex.title }}</span>
              <a-button
                size="small"
                class="example__copy"
                @click="copy(ex.code)"
              >
                <template #icon><CopyOutlined /></template>
                拷贝
              </a-button>
            </div>
            <p class="example__desc">{{ ex.desc }}</p>
            <pre class="example__code"><code>{{ ex.code }}</code></pre>
          </div>
        </article>

        <!-- 统一响应 -->
        <article id="response-format" class="card">
          <h2>5. 统一响应格式</h2>
          <pre class="example__code"><code>{{ responseFormat }}</code></pre>
          <p class="muted">
            <code>code = 0</code> 表示成功；分页接口 <code>data</code> 形如
            <code>{ records, total, page, size }</code>。错误码见
            <a :href="`${baseUrl}/doc.html`" target="_blank" rel="noopener">Knife4j</a>。
          </p>
        </article>
      </section>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { CopyOutlined } from '@ant-design/icons-vue'
import AppHeader from '@/components/AppHeader.vue'

// S22: 锚点导航数据
const anchors = [
  { hash: 'endpoint-list', label: '端点总览' },
  { hash: 'params', label: '参数' },
  { hash: 'response', label: '响应字段' },
  { hash: 'examples', label: '示例' }
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
      // 选第一个与 viewport 顶部相交且 visible ratio > 0 的
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
  // 初次进入若 URL 带 hash，标记当前
  const h = window.location.hash.replace('#', '')
  if (h) activeAnchor.value = h
})
onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})

const baseUrl = 'http://127.0.0.1:8767'

// ===== 端点列表 =====
interface Endpoint {
  method: string
  path: string
  auth: string
  desc: string
}
const endpoints = ref<Endpoint[]>([
  { method: 'GET', path: '/api/skills', auth: '公开', desc: '列表（搜索 / 筛选 / 分页）' },
  { method: 'GET', path: '/api/skills/{id}', auth: '公开', desc: '按 ID 查详情' },
  { method: 'GET', path: '/api/skills/slug/{slug}', auth: '公开', desc: '按 slug 查详情（推荐）' },
  { method: 'GET', path: '/api/skills/hot', auth: '公开', desc: '热门（按 installs）' },
  { method: 'GET', path: '/api/skills/latest', auth: '公开', desc: '最新（按 create_time）' },
  { method: 'GET', path: '/api/skills/featured', auth: '公开', desc: '精选（管理员标记）' },
  { method: 'GET', path: '/api/skills/slug/{slug}/download', auth: '公开', desc: '下载 .skill zip' },
  { method: 'GET', path: '/api/categories', auth: '公开', desc: '全部分类' },
  { method: 'GET', path: '/api/tags', auth: '公开', desc: '全部标签' }
])
const endpointColumns = [
  { title: 'Method', dataIndex: 'method', width: 90, key: 'method' },
  { title: 'Path', dataIndex: 'path', key: 'path' },
  { title: 'Auth', dataIndex: 'auth', width: 90, key: 'auth' },
  { title: '说明', dataIndex: 'desc', key: 'desc' }
]

// ===== /api/skills 参数 =====
interface Param {
  name: string
  type: string
  required: string
  desc: string
  example: string
}
const queryParams = ref<Param[]>([
  { name: 'keyword', type: 'string', required: '否', desc: '关键字模糊匹配 name / displayName / description', example: 'claude' },
  { name: 'categoryId', type: 'number', required: '否', desc: 'SOC 分类 id，传入一级时自动展开到所有子分类', example: '2' },
  { name: 'occupationCode', type: 'string', required: '否', desc: 'SOC 一级 code（#01）或子 code（01-01），按职业维度筛', example: '#01' },
  { name: 'usageCategoryId', type: 'number', required: '否', desc: 'USAGE 维度 id（精确匹配，不展开子分类）', example: '5' },
  { name: 'tagSlug', type: 'string', required: '否', desc: '标签 slug', example: 'claude' },
  { name: 'source', type: 'string', required: '否', desc: '来源过滤：official / official-bundled / community', example: 'official' },
  { name: 'sort', type: 'string', required: '否', desc: 'latest / hot / installs / rating / views', example: 'hot' },
  { name: 'page', type: 'number', required: '否', desc: '页码（从 1 开始，默认 1）', example: '1' },
  { name: 'size', type: 'number', required: '否', desc: '每页条数（最大 100，默认 20）', example: '20' }
])
const paramColumns = [
  { title: '参数', dataIndex: 'name', width: 160, key: 'name' },
  { title: '类型', dataIndex: 'type', width: 90, key: 'type' },
  { title: '必填', dataIndex: 'required', width: 70, key: 'required' },
  { title: '说明', dataIndex: 'desc', key: 'desc' },
  { title: '示例', dataIndex: 'example', width: 140, key: 'example' }
]

// ===== 响应字段 =====
interface Field {
  name: string
  type: string
  desc: string
}
const responseFields = ref<Field[]>([
  { name: 'id', type: 'number', desc: 'Skill 唯一 id' },
  { name: 'name', type: 'string', desc: 'Skill 名称（slug 前缀，与下载文件名一致）' },
  { name: 'slug', type: 'string', desc: 'URL 友好 slug（与 name 通常相同）' },
  { name: 'displayName', type: 'string', desc: '展示用名' },
  { name: 'description', type: 'string', desc: '一句话描述（≤ 1024 字符）' },
  { name: 'categoryId / categoryName / categorySlug', type: 'object', desc: 'SOC 分类（S04 后代表职业维度）' },
  { name: 'usageCategoryId / usageCategoryName / usageCategorySlug', type: 'object', desc: 'USAGE 维度（S18 起独立于 SOC）' },
  { name: 'tags', type: 'string[]', desc: '标签数组' },
  { name: 'version', type: 'string', desc: '版本号（来自 manifest.json 或 metadata.version）' },
  { name: 'authorName / authorEmail', type: 'string', desc: '作者信息' },
  { name: 'installCommand', type: 'string', desc: '推荐安装命令（如 npx skills add ...）' },
  { name: 'downloadUrl', type: 'string', desc: '建议下载路径（与 /api/skills/slug/{slug}/download 等价）' },
  { name: 'installs / views / stars', type: 'number', desc: '统计计数' },
  { name: 'ratingAvg / ratingCount', type: 'number', desc: '评分均值 / 总评数' },
  { name: 'status', type: 'string', desc: 'published / draft / archived（公开 API 仅返回 published）' },
  { name: 'featured', type: 'boolean', desc: '是否精选' },
  { name: 'createTime / updateTime', type: 'string', desc: 'ISO 格式时间（yyyy-MM-dd HH:mm:ss）' }
])
const fieldColumns = [
  { title: '字段', dataIndex: 'name', width: 320, key: 'name' },
  { title: '类型', dataIndex: 'type', width: 90, key: 'type' },
  { title: '说明', dataIndex: 'desc', key: 'desc' }
]

// ===== 示例 =====
interface Example {
  title: string
  desc: string
  code: string
}
const examples = ref<Example[]>([
  {
    title: '关键字搜索',
    desc: '按关键字 `claude` 搜前 5 条。',
    code: `curl "${baseUrl}/api/skills?keyword=claude&size=5"`
  },
  {
    title: '按 SOC 分类筛',
    desc: 'categoryId=2 是一级分类，会自动展开到所有子分类。',
    code: `curl "${baseUrl}/api/skills?categoryId=2&size=10"`
  },
  {
    title: '按职业维度筛',
    desc: 'occupationCode=#01 表示 SOC 一级「计算机与数学类职业」，会展开到所有子职业下的 skill。',
    code: `curl "${baseUrl}/api/skills?occupationCode=%2301&size=10"`
  },
  {
    title: '组合筛选 + 排序',
    desc: '关键字 `code` + 职业 `01-01`（计算机职业）+ 按热度排序。',
    code: `curl "${baseUrl}/api/skills?keyword=code&occupationCode=01-01&sort=hot&size=20"`
  },
  {
    title: '分页',
    desc: 'page=2 size=20 取第 2 页。',
    code: `curl "${baseUrl}/api/skills?page=2&size=20&sort=latest"`
  },
  {
    title: '下载 Skill 包',
    desc: '按 slug 下载 zip 流（-OJ 用服务端返回的 filename）。',
    code: `curl -OJ "${baseUrl}/api/skills/slug/skills-manager/download"`
  }
])

// ===== 统一响应 =====
const responseFormat = `{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [
      { "id": 1, "name": "skills-manager", "displayName": "Skills Manager", "description": "..." }
    ],
    "total": 123,
    "page": 1,
    "size": 20
  }
}`

// ===== 拷贝 =====
async function copy(text: string) {
  try {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    message.success('已拷贝到剪贴板')
  } catch (e) {
    message.error('拷贝失败，请手动复制')
  }
}
</script>

<style scoped lang="scss">
.api-guide {
  max-width: 1100px;
  margin: 0 auto;
  padding: 48px 24px 80px;
  color: var(--text-primary);
  scroll-margin-top: 24px;
}
.api-guide .card {
  scroll-margin-top: 24px;
}
// S22: 锚点导航条
.anchor-nav {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  margin: 0 0 24px;
  padding: 8px;
  background: var(--bg-primary);
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  overflow-x: auto;
  scrollbar-width: thin;
  -webkit-overflow-scrolling: touch;
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
      color: #7c3aed;
    }
    &.is-active {
      background: var(--bg-tertiary);
      color: var(--bg-primary);
    }
  }
}
.hero {
  text-align: center;
  margin-bottom: 24px;
  &__eyebrow {
    color: #7c3aed;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    margin-bottom: 8px;
  }
  h1 {
    margin: 0 0 12px;
    font-size: 32px;
    font-weight: 800;
  }
  &__meta {
    margin: 0;
    font-size: 13px;
    color: var(--text-secondary);
    code {
      background: var(--bg-tertiary);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'SF Mono', Menlo, Consolas, monospace;
      color: var(--text-primary);
    }
    a {
      color: #7c3aed;
    }
  }
  &__divider {
    border: none;
    border-top: 1px solid #e2e8f0;
    margin: 32px 0 24px;
  }
}
.card {
  background: var(--bg-primary);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 24px;
  h2 {
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
    code {
      font-family: 'SF Mono', Menlo, Consolas, monospace;
      font-size: 16px;
      background: var(--bg-tertiary);
      padding: 2px 8px;
      border-radius: 4px;
      color: #7c3aed;
    }
  }
  .muted {
    color: var(--text-tertiary);
    font-size: 13px;
    margin: 0 0 12px;
  }
}
.example {
  margin-bottom: 20px;
  &:last-child {
    margin-bottom: 0;
  }
  &__head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 6px;
  }
  &__no {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    background: var(--bg-tertiary);
    color: var(--bg-primary);
    border-radius: 50%;
    font-size: 12px;
    font-weight: 700;
    flex-shrink: 0;
  }
  &__title {
    font-size: 15px;
    font-weight: 700;
    color: var(--text-primary);
    flex: 1;
  }
  &__copy {
    flex-shrink: 0;
  }
  &__desc {
    margin: 4px 0 8px;
    color: var(--text-secondary);
    font-size: 13px;
    line-height: 1.6;
  }
  &__code {
    margin: 0;
    padding: 14px 16px;
    background: var(--text-primary);
    color: var(--border-color);
    border-radius: 8px;
    overflow-x: auto;
    font-family: 'SF Mono', Menlo, Consolas, monospace;
    font-size: 13px;
    line-height: 1.6;
    code {
      color: inherit;
      background: transparent;
      padding: 0;
    }
  }
}
</style>
