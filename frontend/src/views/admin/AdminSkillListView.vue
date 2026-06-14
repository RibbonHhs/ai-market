<template>
  <div>
    <a-page-header title="Skill 管理" subtitle="维护所有 Skill">
      <template #extra>
        <a-button type="primary" @click="$router.push('/admin/skills/new')">
          <PlusOutlined /> 新建
        </a-button>
        <a-button @click="showUpload = true">
          <CloudUploadOutlined /> 上传 SKILL 文件
        </a-button>
        <a-button @click="onImport">
          <CloudDownloadOutlined /> 从本地导入
        </a-button>
      </template>
    </a-page-header>

    <a-card style="margin-top: 16px">
      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="关键词">
          <a-input v-model:value="query.keyword" placeholder="名称/描述" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" style="width: 140px" allow-clear>
            <a-select-option value="published">已发布</a-select-option>
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="deprecated">已废弃</a-select-option>
            <a-select-option value="flagged">已下架</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="来源">
          <a-select v-model:value="query.sourceType" style="width: 140px" allow-clear>
            <a-select-option value="GIT_URL">Git URL</a-select-option>
            <a-select-option value="LOCAL_ZIP">本地 zip</a-select-option>
            <a-select-option value="LOCAL_FILE">本地 .md</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="reload">查询</a-button>
        </a-form-item>
      </a-form>

      <!-- 批量操作工具栏：仅在有选中行时显示 -->
      <div v-if="selectedRowKeys.length > 0" class="bulk-toolbar">
        <a-space :size="8" align="center">
          <span class="bulk-toolbar__count">
            已选 <strong>{{ selectedRowKeys.length }}</strong> 项
          </span>
          <a-divider type="vertical" />
          <a-popconfirm
            v-if="hasUnpublishedSelected"
            title="确认批量上架已选 Skill？"
            :ok-button-props="{ loading: bulkAction === 'publish' }"
            @confirm="confirmBulkPublish(true)"
          >
            <a-button size="small" type="link">批量上架</a-button>
          </a-popconfirm>
          <a-popconfirm
            v-if="hasPublishedSelected"
            title="确认批量下架已选 Skill？"
            :ok-button-props="{ loading: bulkAction === 'unpublish' }"
            @confirm="confirmBulkPublish(false)"
          >
            <a-button size="small" type="link" style="color: var(--warning)">批量下架</a-button>
          </a-popconfirm>
          <a-popconfirm
            v-if="hasGitUrlSelected"
            :title="`确认批量同步 ${gitUrlSelectedCount} 个 Git 源 Skill？将依次覆盖本地内容。`"
            :ok-button-props="{ loading: bulkAction === 'sync' }"
            @confirm="confirmBulkSync"
          >
            <a-button size="small" type="link" style="color: var(--link)">
              <SyncOutlined /> 批量同步 ({{ gitUrlSelectedCount }})
            </a-button>
          </a-popconfirm>
          <a-popconfirm
            title="确认批量删除已选 Skill？此操作不可恢复。"
            :ok-button-props="{ danger: true, loading: bulkAction === 'delete' }"
            @confirm="confirmBulkDelete"
          >
            <a-button size="small" type="link" danger>
              <DeleteOutlined /> 批量删除
            </a-button>
          </a-popconfirm>
          <a-divider type="vertical" />
          <a-button size="small" type="link" @click="clearSelection">取消选择</a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :row-selection="rowSelection"
        :scroll="{ x: 1400 }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'icon'">
            <SkillLogo :name="record.name" :size="40" />
          </template>
          <template v-else-if="column.key === 'name'">
            <strong>{{ record.displayName || record.name }}</strong>
            <div style="color: var(--text-tertiary); font-size: 12px">{{ record.name }}</div>
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tooltip v-if="record.sourceType === 'GIT_URL'" :title="record.sourceUrl">
              <a-tag color="geekblue">🔗 Git @ {{ record.sourceRef || 'main' }}</a-tag>
            </a-tooltip>
            <a-tag v-else-if="record.sourceType === 'LOCAL_FILE'" color="default">📄 .md</a-tag>
            <a-tag v-else-if="record.sourceType === 'LOCAL_ZIP'" color="default">📦 本地</a-tag>
            <span v-else style="color: var(--text-tertiary); font-size: 12px">—</span>
          </template>
          <template v-else-if="column.key === 'uploader'">
            <a-tag v-if="record.createdByDisplayName" color="cyan">
              {{ record.createdByDisplayName }}
            </a-tag>
            <span v-else style="color: var(--text-tertiary); font-size: 12px">系统</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'rating'">
            <a-rate :value="record.ratingAvg || 0" disabled allow-half :count="5" style="font-size: 12px" />
            <span style="margin-left: 4px; color: var(--text-tertiary); font-size: 12px">({{ record.ratingCount || 0 }})</span>
          </template>
          <template v-else-if="column.key === 'sync'">
            <a-tag v-if="record.lastSyncStatus === 'success'" color="green">✓ {{ formatTime(record.lastSyncAt) }}</a-tag>
            <a-tag v-else-if="record.lastSyncStatus === 'failed'" color="red">✗ {{ formatTime(record.lastSyncAt) }}</a-tag>
            <a-tag v-else-if="record.lastSyncStatus === 'syncing'" color="blue">⟳ 同步中</a-tag>
            <span v-else style="color: var(--text-tertiary); font-size: 12px">—</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4" class="action-cell">
              <a @click="$router.push(`/admin/skills/${record.id}/edit`)">编辑</a>
              <a-divider type="vertical" />
              <a @click="onTogglePublish(record)">
                {{ record.status === 'published' ? '下架' : '上架' }}
              </a>
              <template v-if="record.sourceType === 'GIT_URL'">
                <a-divider type="vertical" />
                <a style="color: var(--link); cursor: pointer" @click="askSync(record)">同步</a>
              </template>
              <a-divider type="vertical" />
              <a style="color: var(--danger); cursor: pointer" @click="askDelete(record)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 删除确认 Modal -->
    <a-modal
      v-model:open="deleteModal.open"
      title="确认删除"
      :ok-button-props="{ danger: true, loading: deleteModal.loading }"
      ok-text="确认删除"
      cancel-text="取消"
      @ok="confirmDelete"
      @cancel="deleteModal.open = false"
    >
      <p>
        确认删除 Skill
        <strong>{{ deleteModal.target?.displayName || deleteModal.target?.name }}</strong> ？
      </p>
      <p style="color: var(--text-tertiary); font-size: 12px">
        ID: {{ deleteModal.target?.id }} · 此操作会同步删除
        <code>data/skill-packages/{{ deleteModal.target?.name }}/</code> 下的包文件，不可恢复
      </p>
    </a-modal>

    <!-- 同步确认 Modal -->
    <a-modal
      v-model:open="syncModal.open"
      title="⚠️ 确认从 Git 同步"
      :ok-button-props="{ danger: true, loading: syncModal.loading }"
      ok-text="确认同步"
      cancel-text="取消"
      @ok="confirmSync"
      @cancel="syncModal.open = false"
    >
      <a-alert
        v-if="syncModal.target"
        type="warning"
        show-icon
        style="margin-bottom: 12px"
        message="将覆盖本地 SKILL.md 与 assets/ 内容"
        description="分类 / 标签 / 精选标记等本地元数据会保留。"
      />
      <p v-if="syncModal.target">
        从远端拉取 <strong>{{ syncModal.target.name }}</strong>：
      </p>
      <a-descriptions v-if="syncModal.target" :column="1" size="small" bordered>
        <a-descriptions-item label="URL">{{ syncModal.target.sourceUrl }}</a-descriptions-item>
        <a-descriptions-item label="Ref">{{ syncModal.target.sourceRef || 'main' }}</a-descriptions-item>
        <a-descriptions-item label="Token">{{ syncModal.target.tokenHint || '（公开仓库）' }}</a-descriptions-item>
        <a-descriptions-item label="上次同步">
          {{ syncModal.target.lastSyncAt || '从未' }}
          <a-tag v-if="syncModal.target.lastSyncStatus === 'success'" color="green">成功</a-tag>
          <a-tag v-else-if="syncModal.target.lastSyncStatus === 'failed'" color="red">失败</a-tag>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 上传 Modal -->
    <a-modal
      v-model:open="showUpload"
      title="📤 上传 SKILL 文件"
      :footer="null"
      width="700px"
      @cancel="showUpload = false"
    >
      <SkillUploader
        @apply="onUploadApply"
        @git-imported="onGitImported"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  CloudDownloadOutlined,
  CloudUploadOutlined,
  DeleteOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import SkillUploader from '@/components/SkillUploader.vue'
import SkillLogo from '@/components/SkillLogo.vue'
import { adminApi } from '@/api/admin'
import type { Skill } from '@/types/skill'

const router = useRouter()
const list = ref<Skill[]>([])
const loading = ref(false)
const total = ref(0)
const showUpload = ref(false)

// ===== 批量选择状态 =====
const selectedRowKeys = ref<number[]>([])
type BulkAction = 'delete' | 'publish' | 'unpublish' | 'sync' | null
const bulkAction = ref<BulkAction>(null)

// 仅当前页可见行的派生选择（checkbox 状态）
const selectedRecords = computed<Skill[]>(() =>
  list.value.filter((s) => selectedRowKeys.value.includes(s.id))
)
const hasPublishedSelected = computed(() => selectedRecords.value.some((s) => s.status === 'published'))
const hasUnpublishedSelected = computed(() => selectedRecords.value.some((s) => s.status !== 'published'))
const gitUrlSelectedRecords = computed<Skill[]>(() => selectedRecords.value.filter((s) => s.sourceType === 'GIT_URL'))
const hasGitUrlSelected = computed(() => gitUrlSelectedRecords.value.length > 0)
const gitUrlSelectedCount = computed(() => gitUrlSelectedRecords.value.length)

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

function clearSelection() {
  selectedRowKeys.value = []
}

// 简易并发限流 runner
async function runWithConcurrency<T, R>(items: T[], limit: number, fn: (item: T) => Promise<R>): Promise<Array<{ ok: true; value: R } | { ok: false; error: any; item: T }>> {
  const results: Array<{ ok: true; value: R } | { ok: false; error: any; item: T }> = []
  let cursor = 0
  async function worker() {
    while (cursor < items.length) {
      const idx = cursor++
      const item = items[idx]
      try {
        const value = await fn(item)
        results[idx] = { ok: true, value }
      } catch (error) {
        results[idx] = { ok: false, error, item }
      }
    }
  }
  const workers = Array.from({ length: Math.min(limit, items.length) }, () => worker())
  await Promise.all(workers)
  return results
}

async function confirmBulkDelete() {
  bulkAction.value = 'delete'
  const ids = [...selectedRowKeys.value]
  const hide = message.loading(`正在删除 ${ids.length} 个 Skill...`, 0)
  try {
    const results = await runWithConcurrency(ids, 3, (id) => adminApi.deleteSkill(id))
    const failed = results.filter((r) => !r.ok)
    if (failed.length === 0) {
      message.success(`已删除 ${ids.length} 个 Skill`)
    } else {
      message.warning(`删除完成：成功 ${ids.length - failed.length}，失败 ${failed.length}`)
    }
    clearSelection()
    reload()
  } finally {
    hide()
    bulkAction.value = null
  }
}

async function confirmBulkPublish(publish: boolean) {
  bulkAction.value = publish ? 'publish' : 'unpublish'
  const records = selectedRecords.value
  const targets = publish
    ? records.filter((s) => s.status !== 'published')
    : records.filter((s) => s.status === 'published')
  if (targets.length === 0) {
    message.info(publish ? '没有可上架的 Skill' : '没有可下架的 Skill')
    bulkAction.value = null
    return
  }
  const hide = message.loading(`正在${publish ? '上架' : '下架'} ${targets.length} 个 Skill...`, 0)
  try {
    const results = await runWithConcurrency(targets, 3, (s) =>
      publish ? adminApi.publishSkill(s.id) : adminApi.unpublishSkill(s.id)
    )
    const failed = results.filter((r) => !r.ok)
    if (failed.length === 0) {
      message.success(`已${publish ? '上架' : '下架'} ${targets.length} 个 Skill`)
    } else {
      message.warning(`操作完成：成功 ${targets.length - failed.length}，失败 ${failed.length}`)
    }
    clearSelection()
    reload()
  } finally {
    hide()
    bulkAction.value = null
  }
}

async function confirmBulkSync() {
  bulkAction.value = 'sync'
  const targets = gitUrlSelectedRecords.value
  if (targets.length === 0) {
    message.info('没有可同步的 Git 源 Skill')
    bulkAction.value = null
    return
  }
  const hide = message.loading(`正在同步 ${targets.length} 个 Git 源 Skill (0/${targets.length})...`, 0)
  let done = 0
  let failed = 0
  try {
    // 串行 + 进度回调（同步是 I/O 重活，并发反而容易把 git 服务器打爆）
    for (const s of targets) {
      try {
        await adminApi.syncSkill(s.id)
        done++
      } catch {
        failed++
      }
      hide()
      message.loading(`正在同步 ${targets.length} 个 Git 源 Skill (${done + failed}/${targets.length})...`, 0)
    }
    if (failed === 0) {
      message.success(`已同步 ${done} 个 Skill`)
    } else {
      message.warning(`同步完成：成功 ${done}，失败 ${failed}`)
    }
    clearSelection()
    reload()
  } finally {
    hide()
    bulkAction.value = null
  }
}

const deleteModal = reactive<{ open: boolean; loading: boolean; target: Skill | null }>({
  open: false,
  loading: false,
  target: null
})
const syncModal = reactive<{ open: boolean; loading: boolean; target: Skill | null }>({
  open: false,
  loading: false,
  target: null
})
const query = reactive<{ keyword?: string; status?: string; sourceType?: string; page: number; size: number }>({
  page: 1,
  size: 20
})

const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
})

const columns = [
  { key: 'icon', title: '', width: 60 },
  { key: 'name', title: '名称', dataIndex: 'name' },
  { key: 'categoryName', title: '分类', dataIndex: 'categoryName', width: 120 },
  { key: 'source', title: '来源', width: 140 },
  { key: 'uploader', title: '上传者', width: 100 },
  { key: 'installs', title: '安装数', dataIndex: 'installs', width: 100, sorter: true },
  { key: 'views', title: '浏览', dataIndex: 'views', width: 100 },
  { key: 'rating', title: '评分', width: 180 },
  { key: 'status', title: '状态', dataIndex: 'status', width: 100 },
  { key: 'sync', title: '同步', width: 180 },
  { key: 'action', title: '操作', width: 240, fixed: 'right' as const }
]

async function reload() {
  loading.value = true
  try {
    const data = await adminApi.listSkills(query)
    list.value = data.records
    total.value = data.total
    pagination.value.current = data.page
    pagination.value.pageSize = data.size
    pagination.value.total = data.total
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: any) {
  query.page = pag.current
  query.size = pag.pageSize
  reload()
}

function statusColor(s?: string) {
  switch (s) {
    case 'published': return 'green'
    case 'draft': return 'default'
    case 'deprecated': return 'orange'
    case 'flagged': return 'red'
    default: return 'default'
  }
}
function statusLabel(s?: string) {
  return { published: '已发布', draft: '草稿', deprecated: '已废弃', flagged: '已下架' }[s || ''] || s
}
function formatTime(s?: string) {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const now = Date.now()
  const diff = Math.floor((now - d.getTime()) / 1000)
  if (diff < 60) return `${diff} 秒前`
  if (diff < 3600) return `${Math.floor(diff / 60)} 分前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  return d.toLocaleDateString('zh-CN')
}

async function onTogglePublish(s: Skill) {
  try {
    if (s.status === 'published') {
      await adminApi.unpublishSkill(s.id)
      message.success('已下架')
    } else {
      await adminApi.publishSkill(s.id)
      message.success('已上架')
    }
    reload()
  } catch { /* interceptor */ }
}

function askDelete(s: Skill) {
  deleteModal.target = s
  deleteModal.open = true
}

async function confirmDelete() {
  if (!deleteModal.target) return
  deleteModal.loading = true
  try {
    await adminApi.deleteSkill(deleteModal.target.id)
    message.success(`已删除 ${deleteModal.target.name}`)
    deleteModal.open = false
    reload()
  } catch (e: any) {
    message.error(e?.bizResponse?.message || '删除失败')
  } finally {
    deleteModal.loading = false
  }
}

function askSync(s: Skill) {
  syncModal.target = s
  syncModal.open = true
}

async function confirmSync() {
  if (!syncModal.target) return
  syncModal.loading = true
  try {
    const res = await adminApi.syncSkill(syncModal.target.id)
    message.success(res.message || '同步成功')
    syncModal.open = false
    reload()
  } catch (e: any) {
    message.error(e?.bizResponse?.message || '同步失败')
  } finally {
    syncModal.loading = false
  }
}

function onUploadApply(res: any) {
  // 上传完成后，跳转到编辑页补全字段
  showUpload.value = false
  router.push({
    name: 'admin-skill-new',
    state: { fromUpload: res }
  })
}

function onGitImported(_res: any) {
  // 关闭 modal 并刷新列表，让用户看到新导入的 skill
  showUpload.value = false
  message.success('Git 导入完成，列表已刷新')
  reload()
}

async function onImport() {
  const hide = message.loading('正在扫描本地 Skills 目录...', 0)
  try {
    const res = await adminApi.importFromLocal()
    message.success(`导入完成，新增 ${res.imported}，跳过 ${res.skipped}`)
    reload()
  } finally {
    hide()
  }
}

onMounted(reload)
</script>

<style scoped>
/* 操作列：强制不换行，阻止中文字符被 flex 容器拆字 */
:deep(.ant-table-cell) .action-cell {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 0;
}
:deep(.ant-table-cell) .action-cell :deep(.ant-space-item) {
  white-space: nowrap;
}
/* 操作列内 <a> 标签不换行 + 不允许字符级断行 */
:deep(.action-cell) a {
  white-space: nowrap;
  word-break: keep-all;
  padding: 0 4px;
}

/* 批量操作工具栏 — S37: 跟随主题（浅=紫底 / 深=紫半透明） */
.bulk-toolbar {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--primary-bg);
  border: 1px solid var(--primary-border);
  border-radius: 4px;
}
.bulk-toolbar__count {
  color: var(--link);
  font-size: 13px;
}
.bulk-toolbar__count strong {
  font-weight: 600;
  margin: 0 2px;
}
</style>
