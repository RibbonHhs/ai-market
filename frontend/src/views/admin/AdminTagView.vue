<template>
  <a-page-header title="标签管理" />
  <a-card style="margin-top: 16px">
    <a-table :columns="columns" :data-source="list" row-key="id" :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除？" @confirm="onDelete(record)">
            <a style="color: var(--danger)">删除</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api/admin'
import type { Tag } from '@/types/skill'

const list = ref<Tag[]>([])
const loading = ref(false)
const columns = [
  { title: '名称', dataIndex: 'name' },
  { title: 'Slug', dataIndex: 'slug' },
  { title: 'Skill 数', dataIndex: 'skillCount', width: 100 },
  { key: 'action', title: '操作', width: 100 }
]

async function reload() {
  loading.value = true
  try {
    list.value = await adminApi.listTags()
  } finally {
    loading.value = false
  }
}

async function onDelete(t: Tag) {
  try {
    await adminApi.deleteTag(t.id)
    message.success('已删除')
    reload()
  } catch { /* interceptor */ }
}

onMounted(reload)
</script>
