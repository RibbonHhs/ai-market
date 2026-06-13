<template>
  <a-page-header title="用途分类管理" sub-title="USAGE 维度 · 12 一级 + 70 sub = 82 类目">
    <template #extra>
      <a-button @click="$router.push('/admin/categories')">
        <ArrowLeftOutlined /> 切到职业技能
      </a-button>
    </template>
  </a-page-header>
  <a-card style="margin-top: 16px">
    <template #extra>
      <a-button type="primary" @click="showModal()">
        <PlusOutlined /> 新建用途分类
      </a-button>
    </template>
    <a-table
      :columns="columns"
      :data-source="list"
      row-key="id"
      :loading="loading"
      :pagination="{ pageSize: 20 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag color="purple">{{ record.type }}</a-tag>
        </template>
        <template v-else-if="column.key === 'icon'">
          <span style="font-size: 24px">{{ record.icon }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="showModal(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确定删除？" @confirm="onDelete(record)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal v-model:open="modalVisible" :title="editing.id ? '编辑用途分类' : '新建用途分类'" @ok="onSave">
    <a-form layout="vertical">
      <a-form-item label="名称" required>
        <a-input v-model:value="editing.name" />
      </a-form-item>
      <a-form-item label="Slug" required>
        <a-input v-model:value="editing.slug" />
      </a-form-item>
      <a-form-item label="图标">
        <a-input v-model:value="editing.icon" />
      </a-form-item>
      <a-form-item label="描述">
        <a-textarea v-model:value="editing.description" :rows="2" />
      </a-form-item>
      <a-form-item label="排序">
        <a-input-number v-model:value="editing.sortOrder" :min="0" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'
import { adminApi } from '@/api/admin'
import type { Category } from '@/types/skill'

const list = ref<Category[]>([])
const loading = ref(false)
const modalVisible = ref(false)
const editing = reactive<any>({
  id: null,
  name: '',
  slug: '',
  type: 'USAGE',
  icon: '🧭',
  description: '',
  sortOrder: 0
})

const columns = [
  { key: 'type', title: '维度', width: 90 },
  { key: 'icon', title: '', width: 60 },
  { title: '名称', dataIndex: 'name' },
  { title: 'Slug', dataIndex: 'slug' },
  { title: '描述', dataIndex: 'description' },
  { title: '排序', dataIndex: 'sortOrder', width: 80 },
  { title: 'Skill 数', dataIndex: 'skillCount', width: 100 },
  { key: 'action', title: '操作', width: 150 }
]

async function reload() {
  loading.value = true
  try {
    list.value = await adminApi.listCategories('USAGE')
  } finally {
    loading.value = false
  }
}

function showModal(c?: Category) {
  if (c) {
    Object.assign(editing, c)
  } else {
    Object.assign(editing, {
      id: null,
      name: '',
      slug: '',
      type: 'USAGE',
      icon: '🧭',
      description: '',
      sortOrder: 0
    })
  }
  modalVisible.value = true
}

async function onSave() {
  try {
    if (editing.id) {
      await adminApi.updateCategory(editing.id, editing)
      message.success('已更新')
    } else {
      await adminApi.createCategory(editing)
      message.success('已创建')
    }
    modalVisible.value = false
    reload()
  } catch {
    /* interceptor */
  }
}

async function onDelete(c: Category) {
  try {
    await adminApi.deleteCategory(c.id)
    message.success('已删除')
    reload()
  } catch {
    /* interceptor */
  }
}

onMounted(reload)
</script>
