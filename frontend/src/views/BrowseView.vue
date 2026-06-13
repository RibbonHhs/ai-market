<template>
  <a-layout class="app-layout">
    <AppHeader />
    <a-layout-content class="app-content">
      <div class="browse">
        <!-- 左侧筛选 -->
        <aside class="browse__filter">
          <div class="filter-block">
            <h4>分类维度</h4>
            <a-radio-group v-model:value="dim" button-style="solid" size="small">
              <a-radio-button value="soc">按职业</a-radio-button>
              <a-radio-button value="usage">按用途</a-radio-button>
            </a-radio-group>
          </div>

          <div class="filter-block">
            <h4>分类</h4>
            <div
              class="cat-all"
              :class="{ active: activeCategoryId === '0' }"
              @click="onCategoryClick({ key: '0' })"
            >
              <span class="cat-label">📦 全部</span>
            </div>
            <a-tree
              v-if="categoryTree.length"
              :key="dim"
              class="cat-tree"
              :tree-data="categoryTree"
              :default-expanded-keys="defaultExpandedKeys"
              :selected-keys="[selectedTreeKey]"
              :field-names="{ title: 'name', key: 'slug', children: 'children' }"
              block-node
              show-line
              @select="onTreeSelect"
            >
              <template #title="{ name, code, skillCount, slug }">
                <span class="cat-row">
                  <span v-if="code" class="cat-code">{{ code }}</span>
                  <span class="cat-label">{{ name }}</span>
                  <span class="cat-count">{{ skillCount || 0 }}</span>
                </span>
              </template>
            </a-tree>
          </div>

          <div class="filter-block">
            <h4>来源</h4>
            <a-radio-group v-model:value="query.source" button-style="solid" size="small">
              <a-radio-button value="">全部</a-radio-button>
              <a-radio-button value="official">官方</a-radio-button>
              <a-radio-button value="community">社区</a-radio-button>
            </a-radio-group>
          </div>
        </aside>

        <!-- 主列表 -->
        <main class="browse__main">
          <!-- S24: 顶部 USAGE 横向 chip 筛选条（一级 USAGE） -->
          <div class="browse__usage-filter" role="toolbar" aria-label="按用途筛选" data-testid="usage-filter">
            <span class="usage-filter__label">用途：</span>
            <div class="usage-filter__chips">
              <button
                class="usage-chip"
                :class="{ 'usage-chip--active': activeUsageTop === null }"
                @click="onUsageTopClick(null)"
                aria-label="清除用途筛选"
                data-testid="usage-filter-all"
              >
                <span class="usage-chip__emoji">📦</span>
                <span>全部</span>
              </button>
              <button
                v-for="c in usageTopList"
                :key="c.code"
                class="usage-chip"
                :class="[
                  `usage-chip--code-${c.code.toLowerCase()}`,
                  { 'usage-chip--active': activeUsageTop === c.id }
                ]"
                @click="onUsageTopClick(c.id)"
                :aria-label="`筛选：${c.name}`"
                :data-testid="`usage-filter-${c.code.toLowerCase()}`"
              >
                <span class="usage-chip__emoji">{{ c.emoji }}</span>
                <span>{{ c.name }}</span>
              </button>
            </div>
          </div>

          <div class="browse__head">
            <a-input-search
              v-model:value="query.keyword"
              placeholder="搜索 Skills..."
              enter-button
              @search="reload"
              class="browse__search"
            />
            <a-select v-model:value="query.sort" @change="reload" style="width: 140px">
              <a-select-option value="latest">最新发布</a-select-option>
              <a-select-option value="installs">最多安装</a-select-option>
              <a-select-option value="rating">最高评分</a-select-option>
              <a-select-option value="views">最多浏览</a-select-option>
            </a-select>
          </div>

          <div v-if="loading" class="loading-block">
            <a-spin />
          </div>
          <a-empty v-else-if="!list.length" description="没有匹配的 Skill" />
          <a-row v-else :gutter="[16, 16]" data-testid="skill-grid">
            <a-col v-for="s in list" :key="s.id" :xs="24" :sm="12" :md="8" :lg="6">
              <SkillCard :skill="s" />
            </a-col>
          </a-row>

          <a-pagination
            v-if="total > size"
            v-model:current="query.page"
            v-model:page-size="query.size"
            :total="total"
            show-size-changer
            @change="reload"
            class="browse__pager"
          />
        </main>
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import SkillCard from '@/components/SkillCard.vue'
import { skillApi, categoryApi, type SkillQuery } from '@/api/skill'
import type { Skill, Category } from '@/types/skill'
import { USAGE_TOP_ORDER, USAGE_COLORS, getUsageColor } from '@/constants/usage-colors'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const list = ref<Skill[]>([])
const total = ref(0)
const size = ref(20)
const categories = ref<Category[]>([])
/** S18: 当前维度 — 'soc' 按职业 / 'usage' 按用途 */
const dim = ref<'soc' | 'usage'>('soc')
const activeCategoryId = ref<number | string>('0')

/** 树形节点（前端组装，不改后端/类型） */
interface CategoryTreeNode extends Category {
  parentId?: number | null
  code?: string
  children?: CategoryTreeNode[]
}

const categoryTree = computed<CategoryTreeNode[]>(() => {
  const all = categories.value
  if (!all.length) return []
  // S18: 按当前维度过滤
  const filtered = all.filter(
    (c) => (c as Category & { type?: string }).type === (dim.value === 'soc' ? 'SOC' : 'USAGE')
  )
  if (!filtered.length) return []
  const roots = filtered.filter((c) => c.parentId == null)
  const byParent = new Map<number, CategoryTreeNode[]>()
  for (const c of filtered) {
    if (c.parentId == null) continue
    const arr = byParent.get(c.parentId) || []
    arr.push(c as CategoryTreeNode)
    byParent.set(c.parentId, arr)
  }
  return roots.map((r) => {
    const direct = byParent.get(r.id) || []
    // 聚合 skillCount：直接子项 + 一级自身
    const aggregated =
      (r.skillCount || 0) +
      direct.reduce((sum, ch) => sum + (ch.skillCount || 0), 0)
    return {
      ...r,
      code: (r as Category & { code?: string }).code,
      skillCount: aggregated,
      children: direct
    } as CategoryTreeNode
  })
})

/** 默认展开第一个一级（避免侧栏太长） */
const defaultExpandedKeys = computed(() => {
  const first = categoryTree.value[0]
  return first ? [first.slug] : []
})

/** 当前选中节点的 tree key（slug），用于高亮 */
const selectedTreeKey = computed(() => {
  if (activeCategoryId.value === '0') return ''
  const c = categories.value.find((cc) => cc.id === activeCategoryId.value)
  return c?.slug || ''
})

const query = reactive<SkillQuery>({
  keyword: '',
  categoryId: undefined,
  source: '',
  sort: 'latest',
  page: 1,
  size: 20
})

/** S24: 顶部 chip 选中态（null = 全部）；按下后写入 query.usageCategoryId 并 reload */
const activeUsageTop = ref<number | null>(null)

/** S24 + S25: 12 个一级 USAGE 列表（按 USAGE_TOP_ORDER 顺序；CSS 变量驱动主题） */
const usageTopList = computed(() => {
  return USAGE_TOP_ORDER
    .map(code => {
      const c = USAGE_COLORS[code]
      if (!c) return null
      const cat = categories.value.find(
        (cc) => (cc as Category & { type?: string }).type === 'USAGE' && cc.code === code && cc.parentId == null
      )
      if (!cat) return null
      // S25: 不再返回 bg/fg（CSS 变量驱动）
      return { id: cat.id, code, name: c.name, emoji: c.emoji }
    })
    .filter((x): x is NonNullable<typeof x> => x !== null)
})

/** S24: 点击顶部 chip（再次点同色 = 取消） */
function onUsageTopClick(id: number | null) {
  if (id === null) {
    activeUsageTop.value = null
    query.usageCategoryId = undefined
  } else if (activeUsageTop.value === id) {
    activeUsageTop.value = null
    query.usageCategoryId = undefined
  } else {
    activeUsageTop.value = id
    query.usageCategoryId = id
  }
  // 顶部 chip 流是粗筛；左 sidebar 树不联动（避免冲突）
  query.page = 1
  reload()
}

async function loadCategories() {
  categories.value = await categoryApi.list()
}

async function reload() {
  loading.value = true
  try {
    const data = await skillApi.list(query)
    list.value = data.records
    total.value = data.total
    size.value = data.size
  } finally {
    loading.value = false
  }
}

function onCategoryClick({ key }: { key: string | number }) {
  activeCategoryId.value = key
  // S18: 按维度写入对应过滤参数
  if (dim.value === 'soc') {
    query.categoryId = key === '0' ? undefined : Number(key)
    query.usageCategoryId = undefined
  } else {
    query.usageCategoryId = key === '0' ? undefined : Number(key)
    query.categoryId = undefined
  }
  query.page = 1
  reload()
}

/** a-tree select：key 是 slug（field-names 映射） */
function onTreeSelect(_keys: (string | number)[], info: { node: { dataRef?: CategoryTreeNode } }) {
  const node = info.node.dataRef
  if (!node) return
  // 一级和二级都可点：直接用节点 id 筛选（后端按 id 筛）
  activeCategoryId.value = node.id
  if (dim.value === 'soc') {
    query.categoryId = node.id
    query.usageCategoryId = undefined
  } else {
    query.usageCategoryId = node.id
    query.categoryId = undefined
  }
  query.page = 1
  reload()
}

/** S18: 维度切换时重置过滤 */
watch(dim, () => {
  activeCategoryId.value = '0'
  query.categoryId = undefined
  query.usageCategoryId = undefined
  query.page = 1
  reload()
})

onMounted(async () => {
  await loadCategories()
  // 同步路由参数
  if (route.query.keyword) query.keyword = String(route.query.keyword)
  if (route.query.sort) query.sort = String(route.query.sort) as SkillQuery['sort']
  // S18: 路由 ?dim=usage 切维度
  if (route.query.dim === 'usage') {
    dim.value = 'usage'
  }
  if (route.params.slug) {
    const c = categories.value.find((cc) => cc.slug === route.params.slug)
    if (c) {
      const cType = (c as Category & { type?: string }).type
      if (cType === 'USAGE') {
        dim.value = 'usage'
        activeCategoryId.value = c.id
        query.usageCategoryId = c.id
      } else {
        activeCategoryId.value = c.id
        query.categoryId = c.id
      }
    }
  }
  reload()
})

watch(
  () => route.query.keyword,
  (v) => {
    if (v != null) {
      query.keyword = String(v)
      reload()
    }
  }
)
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}
.app-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
}
.browse {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}
.browse__filter {
  background: var(--bg-secondary);
  border-radius: 10px;
  padding: 16px;
  height: fit-content;
  position: sticky;
  top: 88px;
  border: 1px solid var(--border-color);

  // 「全部」置顶项
  .cat-all {
    padding: 6px 8px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    margin-bottom: 4px;
    color: var(--text-primary);
    transition: background-color 0.15s ease;
    &:hover {
      background-color: var(--bg-tertiary);
    }
    &.active {
      background-color: var(--bg-tertiary);
      color: var(--primary);
      font-weight: 500;
    }
  }

  // 树形容器：去掉 antd 默认的左侧缩进/连接线干扰
  :deep(.cat-tree) {
    .ant-tree-node-content-wrapper {
      flex: 1;
      min-width: 0;
      padding: 4px 6px !important;
      border-radius: 4px;
      color: var(--text-primary);
      &:hover {
        background-color: var(--bg-tertiary);
      }
      &.ant-tree-node-selected {
        background-color: var(--bg-tertiary) !important;
        .cat-row {
          color: var(--primary);
          font-weight: 500;
        }
      }
    }
    // 缩进更紧凑
    .ant-tree-indent-unit {
      width: 14px;
    }
  }

  // 节点内容布局：code | name 撑满 | count 贴右
  :deep(.cat-row) {
    display: flex;
    align-items: center;
    width: 100%;
    min-width: 0;
    gap: 6px;
    font-size: 13px;
    color: var(--text-primary);
  }
  :deep(.cat-code) {
    color: var(--text-tertiary);
    font-size: 11px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    flex-shrink: 0;
    min-width: 32px;
  }
  :deep(.cat-label) {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  :deep(.cat-count) {
    color: var(--text-tertiary);
    font-size: 11px;
    flex-shrink: 0;
    margin-left: auto;
  }
}
.filter-block {
  margin-bottom: 16px;
  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .muted {
    color: var(--text-tertiary);
    font-size: 12px;
    float: right;
  }
}
.browse__main {
  min-width: 0;
}
.browse__head {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  background: var(--bg-secondary);
  padding: 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  .browse__search {
    flex: 1;
  }
}
/* S24: 顶部 USAGE chip 流 */
.browse__usage-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-secondary);
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 12px;
  overflow-x: auto;
  scrollbar-width: thin;
  touch-action: manipulation; /* 减 300ms tap delay */
  border: 1px solid var(--border-color);
}
.usage-filter__label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  flex-shrink: 0;
}
.usage-filter__chips {
  display: flex;
  gap: 6px;
  flex-wrap: nowrap;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.usage-filter__chips .usage-chip {
  flex-shrink: 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 12px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid transparent;
  background: #f5f5f5;
  color: #595959;
  transition: all 150ms ease-out;
  white-space: nowrap;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  min-width: 44px; /* 触控最小尺寸 */
}
.usage-filter__chips .usage-chip:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.usage-filter__chips .usage-chip:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}
.usage-filter__chips .usage-chip--active {
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.usage-chip__emoji {
  font-size: 14px;
  line-height: 1;
}
@media (max-width: 768px) {
  .browse__usage-filter {
    padding: 8px 10px;
  }
  .usage-filter__label {
    display: none;
  }
  .usage-filter__chips .usage-chip {
    font-size: 12px;
    height: 32px;
    padding: 0 10px;
  }
}
.browse__pager {
  text-align: right;
  margin-top: 24px;
}
.loading-block {
  text-align: center;
  padding: 60px 0;
}
</style>
