<template>
  <a-page-header title="用户管理" />
  <a-card style="margin-top: 16px">
    <a-table :columns="columns" :data-source="list" row-key="id" :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <a-tag :color="record.role === 'ADMIN' ? 'gold' : 'blue'">
            {{ record.role === 'ADMIN' ? '👑 管理员' : '🙂 用户' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="onToggleRole(record)">
              {{ record.role === 'ADMIN' ? '降为用户' : '升为管理员' }}
            </a>
            <a-divider type="vertical" />
            <a @click="onToggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api/admin'

const list = ref<any[]>([])
const loading = ref(false)
const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username' },
  { title: '显示名', dataIndex: 'displayName' },
  { title: '邮箱', dataIndex: 'email' },
  { key: 'role', title: '角色', width: 120 },
  { key: 'status', title: '状态', width: 100 },
  { title: '注册时间', dataIndex: 'createTime', width: 180 },
  { key: 'action', title: '操作', width: 200, fixed: 'right' as const }
]

async function reload() {
  loading.value = true
  try {
    const data = await adminApi.listUsers({ size: 100 })
    list.value = data.records
  } finally {
    loading.value = false
  }
}

async function onToggleRole(u: any) {
  const newRole = u.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    await adminApi.updateUserRole(u.id, newRole)
    message.success('已更新角色')
    reload()
  } catch { /* interceptor */ }
}

async function onToggleStatus(u: any) {
  try {
    await adminApi.updateUserStatus(u.id, u.status === 1 ? 0 : 1)
    message.success('已更新状态')
    reload()
  } catch { /* interceptor */ }
}

onMounted(reload)
</script>
