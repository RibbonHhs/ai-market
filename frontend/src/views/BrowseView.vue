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
              :expanded-keys="expandedKeys"
              @update:expanded-keys="(keys: string[]) => (expandedKeys = keys)"
              :selected-keys="selectedKeysArr"
              @update:selected-keys="(keys: (string | number)[]) => (selectedKeysArr = keys as string[])"
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

/** 当前选中节点的 tree key（slug），用于高亮 + 祖先链展开
 * 必须先声明，因为下方 watch 用了它，且 immediate:true 会同步触发 */
const selectedTreeKey = computed(() => {
  if (activeCategoryId.value === '0') return ''
  const c = categories.value.find((cc) => cc.id === activeCategoryId.value)
  return c?.slug || ''
})

/** 受控 selectedKeys：watch 同步推送到 a-tree，确保初次挂载即高亮。
 * 之前用 `:selected-keys="[selectedTreeKey]"` 创建的是 inline array，
 * a-tree 在 prop 频繁变化时不会重新渲染选中态。改成 watch→ref。 */
const selectedKeysArr = ref<string[]>([])
watch(
  selectedTreeKey,
  (slug) => {
    selectedKeysArr.value = slug ? [slug] : []
  },
  { immediate: true }
)

/** 展开键：受控（v-model:expanded-keys）。
 * 包含「首个一级」+「选中节点的祖先链」—— 让树定位到 slug 对应节点。
 * S35-fix: 必须受控，否则 a-tree 的 default-expanded-keys 只在初次挂载生效，
 * 同维度跳转（USAGE→USAGE）时树不重挂载，新键不生效。
 */
const expandedKeys = ref<string[]>([])

watch(
  [categoryTree, selectedTreeKey, () => categories.value.length],
  () => {
    const keys = new Set<string>()
    const first = categoryTree.value[0]
    if (first) keys.add(first.slug)
    if (selectedTreeKey.value) {
      const c = categories.value.find((cc) => cc.slug === selectedTreeKey.value)
      if (c?.parentId) {
        let cur: Category | undefined = categories.value.find(
          (cc) => cc.id === c.parentId
        )
        while (cur) {
          if (cur.slug) keys.add(cur.slug)
          cur = cur.parentId
            ? categories.value.find((cc) => cc.id === cur!.parentId)
            : undefined
        }
      }
    }
    expandedKeys.value = Array.from(keys)
  },
  { immediate: true }
)

const query = reactive<SkillQuery>({
  keyword: '',
  categoryId: undefined,
  source: '',
  sort: 'latest',
  page: 1,
  size: 20
})

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

/** S18: 维度切换时重置过滤（用户主动切"按职业/按用途"radio 时触发）
 * S35-fix: 当有 slug 路由时，applySlugFilter 会先 set dim 再 set activeCategoryId；
 * 此时 watch(dim) 会在下一 tick 把 activeCategoryId 强制重置为 '0'，覆盖 slug 选择。
 * 解决：有 slug 时让 applySlugFilter 全权管理状态，watch 不重置。 */
watch(dim, () => {
  if (route.params.slug) return
  activeCategoryId.value = '0'
  query.categoryId = undefined
  query.usageCategoryId = undefined
  query.page = 1
  reload()
})

/**
 * S35: 根据 slug + 可选 type hint 应用分类过滤
 * - 用于 onMounted 首次进入和 watch(slug) 路由复用场景
 * - typeHint 优先（来自 query.type = USAGE / SOC），无则按 slug 全局匹配
 */
async function applySlugFilter(slug: string, typeHint?: string) {
  if (!categories.value.length) {
    await loadCategories()
  }
  const c = categories.value.find(
    (cc) =>
      cc.slug === slug &&
      (!typeHint || (cc as Category & { type?: string }).type === typeHint)
  )
  if (!c) return false
  const cType = (c as Category & { type?: string }).type
  if (cType === 'USAGE') {
    dim.value = 'usage'
    activeCategoryId.value = c.id
    query.usageCategoryId = c.id
    query.categoryId = undefined
  } else {
    dim.value = 'soc'
    activeCategoryId.value = c.id
    query.categoryId = c.id
    query.usageCategoryId = undefined
  }
  query.page = 1
  return true
}

onMounted(async () => {
  await loadCategories()
  // 同步路由参数
  if (route.query.keyword) query.keyword = String(route.query.keyword)
  if (route.query.sort) query.sort = String(route.query.sort) as SkillQuery['sort']
  // S18: 路由 ?dim=usage 切维度
  if (route.query.dim === 'usage') {
    dim.value = 'usage'
  }
  // S35: slug 应用过滤（带 type hint 避免 USAGE/SOC 撞名风险）
  if (route.params.slug) {
    await applySlugFilter(String(route.params.slug), route.query.type as string | undefined)
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

// S35: 路由复用（同组件 /categories/A → /categories/B）时重新应用 slug
watch(
  () => [route.params.slug, route.query.type],
  async ([newSlug, newType]) => {
    if (newSlug) {
      const ok = await applySlugFilter(String(newSlug), newType as string | undefined)
      if (ok) reload()
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
.browse__pager {
  text-align: right;
  margin-top: 24px;
}
.loading-block {
  text-align: center;
  padding: 60px 0;
}
</style>
