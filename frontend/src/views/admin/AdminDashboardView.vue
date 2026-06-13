<template>
  <div>
    <h2>📊 Dashboard</h2>
    <a-row :gutter="16" style="margin-top: 16px">
      <a-col v-for="s in stats" :key="s.label" :xs="12" :sm="6">
        <a-card>
          <a-statistic :title="s.label" :value="s.value" :value-style="{ color: s.color }">
            <template #prefix>
              <component :is="s.icon" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :xs="24" :md="12">
        <a-card title="📈 Skill 来源分布">
          <a-list :data-source="sourceStats" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <span>{{ item.label }}</span>
                <a-progress :percent="item.percent" :show-info="true" style="width: 50%" />
                <span>{{ item.count }}</span>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-card title="🚀 快速操作">
          <a-space direction="vertical" style="width: 100%">
            <a-button type="primary" block @click="$router.push('/admin/skills/new')">
              <PlusOutlined /> 新建 Skill
            </a-button>
            <a-button block @click="onImport">
              <CloudDownloadOutlined /> 从本地 SKILL.md 导入
            </a-button>
            <a-button block @click="onRefresh">
              <ReloadOutlined /> 刷新分类计数
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="24">
        <a-card title="🌿 Git 同步状态">
          <a-row :gutter="16">
            <a-col :xs="24" :sm="6">
              <a-statistic
                title="状态"
                :value="gitStatus.enabled ? (gitStatus.ready ? '运行中' : '启用但未就绪') : '未启用（仅本地）'"
                :value-style="{ color: gitStatus.enabled && gitStatus.ready ? '#52c41a' : '#999' }"
              />
            </a-col>
            <a-col :xs="24" :sm="6">
              <a-statistic title="成功推送" :value="gitStatus.successCount" :value-style="{ color: '#52c41a' }" />
            </a-col>
            <a-col :xs="24" :sm="6">
              <a-statistic title="推送失败" :value="gitStatus.failureCount" :value-style="{ color: gitStatus.failureCount > 0 ? '#ff4d4f' : '#999' }" />
            </a-col>
            <a-col :xs="24" :sm="6">
              <a-statistic title="最后同步" :value="gitStatus.lastSyncAt || '—'" />
            </a-col>
          </a-row>
          <a-alert
            v-if="gitStatus.lastError"
            type="error"
            show-icon
            style="margin-top: 12px"
            :message="'最近错误：' + gitStatus.lastError"
          />
          <a-alert
            v-else-if="!gitStatus.enabled"
            type="info"
            show-icon
            style="margin-top: 12px"
            message="Git 同步未启用"
            description="设置 skillsmap.storage.git.enabled=true 并配置 repoUrl/username/token 即可启用"
          />
          <a-alert
            v-else-if="!gitStatus.ready"
            type="warning"
            show-icon
            style="margin-top: 12px"
            message="Git 已启用但未就绪"
            description="请检查 repoUrl/网络/token 配置"
          />
          <div v-if="gitStatus.recentCommits?.length" style="margin-top: 12px">
            <a-typography-title :level="5">最近 5 次提交</a-typography-title>
            <a-list size="small" :data-source="gitStatus.recentCommits">
              <template #renderItem="{ item }">
                <a-list-item>
                  <code>{{ item }}</code>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, CloudDownloadOutlined, ReloadOutlined, AppstoreOutlined, FolderOutlined, TagsOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { skillApi, categoryApi, tagApi } from '@/api/skill'
import { adminApi } from '@/api/admin'

const stats = ref([
  { label: 'Skills', value: 0, color: '#1677ff', icon: () => h(AppstoreOutlined) },
  { label: '分类', value: 0, color: '#722ed1', icon: () => h(FolderOutlined) },
  { label: '标签', value: 0, color: '#13c2c2', icon: () => h(TagsOutlined) },
  { label: '用户', value: 0, color: '#fa8c16', icon: () => h(TeamOutlined) }
])

const sourceStats = ref<Array<{ label: string; count: number; percent: number }>>([])

const gitStatus = ref<{
  enabled: boolean
  ready: boolean
  successCount: number
  failureCount: number
  lastSyncAt: string | null
  lastError: string | null
  recentCommits: string[]
}>({
  enabled: false,
  ready: false,
  successCount: 0,
  failureCount: 0,
  lastSyncAt: null,
  lastError: null,
  recentCommits: []
})

async function load() {
  const [s, c, t, src] = await Promise.all([
    skillApi.list({ size: 1 }),
    categoryApi.list(),
    tagApi.list(),
    adminApi.dashboardSourceStats().catch(() => ({ official: 0, community: 0, total: 0 }))
  ])
  stats.value[0].value = s.total
  stats.value[1].value = c.length
  stats.value[2].value = t.length
  // 用户数
  try {
    const u = await adminApi.listUsers({ size: 1 })
    stats.value[3].value = u.total
  } catch {
    stats.value[3].value = 0
  }
  // 来源分布
  const official = (src as any).official || 0
  const community = (src as any).community || 0
  const total = s.total || 1
  sourceStats.value = [
    { label: 'Official 官方', count: official, percent: Math.round((official / total) * 100) },
    { label: 'Community 社区', count: community, percent: Math.round((community / total) * 100) }
  ]
  // Git 同步状态
  try {
    gitStatus.value = await adminApi.dashboardGitStatus()
  } catch {
    /* ignore */
  }
}

async function onImport() {
  const hide = message.loading('正在扫描本地 Skills 目录...', 0)
  try {
    const res = await adminApi.importFromLocal()
    message.success(`导入完成，新增 ${res.imported} 个 Skill${res.skipped ? `，跳过 ${res.skipped} 个` : ''}`)
    load()
  } catch {
    /* interceptor 提示 */
  } finally {
    hide()
  }
}

async function onRefresh() {
  const hide = message.loading('刷新中...', 0)
  try {
    await adminApi.refreshCategoryCount()
    message.success('分类计数已刷新')
    load()
  } finally {
    hide()
  }
}

onMounted(load)
</script>
