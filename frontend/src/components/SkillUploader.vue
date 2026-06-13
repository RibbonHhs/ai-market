<template>
  <a-card class="skill-uploader" title="📤 从文件 / Git 导入 Skill">
    <a-tabs v-model:active-key="mode">
      <a-tab-pane key="md" tab="本地 .md">
        <a-upload-dragger
          :before-upload="handleMdBefore"
          :show-upload-list="false"
          accept=".md"
          :disabled="loading"
        >
          <p class="ant-upload-drag-icon">
            <CloudUploadOutlined />
          </p>
          <p class="ant-upload-text">点击或拖拽 SKILL.md 到这里</p>
          <p class="ant-upload-hint">
            解析 frontmatter 后预填表单。name 必须为 kebab-case。
          </p>
        </a-upload-dragger>
      </a-tab-pane>

      <a-tab-pane key="zip" tab=".skill 包 (zip)">
        <a-upload-dragger
          :before-upload="handleZipBefore"
          :show-upload-list="false"
          accept=".zip,.skill"
          :disabled="loading"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">点击或拖拽 .skill / .zip 包</p>
          <p class="ant-upload-hint">
            包含 SKILL.md + scripts/ + references/ + assets/ 的完整 Skill 包
          </p>
        </a-upload-dragger>
      </a-tab-pane>

      <a-tab-pane key="git" tab="Git URL">
        <GitUrlForm @imported="onGitImported" />
      </a-tab-pane>
    </a-tabs>

    <!-- 解析预览（仅 md / zip 流程） -->
    <div v-if="preview" class="preview">
      <a-divider>解析预览</a-divider>
      <a-descriptions :column="2" size="small" bordered>
        <a-descriptions-item label="name">{{ preview.name }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ preview.filename }} ({{ formatSize(preview.size) }})</a-descriptions-item>
        <a-descriptions-item v-if="preview.preview?.description" label="description" :span="2">
          {{ preview.preview.description }}
        </a-descriptions-item>
        <a-descriptions-item v-if="preview.preview?.version" label="version">{{ preview.preview.version }}</a-descriptions-item>
        <a-descriptions-item v-if="preview.preview?.license" label="license">{{ preview.preview.license }}</a-descriptions-item>
        <a-descriptions-item v-if="preview.preview?.allowedTools" label="allowed-tools" :span="2">
          {{ preview.preview.allowedTools }}
        </a-descriptions-item>
        <a-descriptions-item v-if="preview.preview?.compatibility" label="compatibility" :span="2">
          {{ preview.preview.compatibility }}
        </a-descriptions-item>
        <a-descriptions-item v-if="preview.resources?.length" label="资源文件" :span="2">
          <a-space wrap size="small">
            <a-tag v-for="r in preview.resources" :key="r.path" :color="kindColor(r.kind)">
              {{ r.kind }} · {{ r.path }} · {{ formatSize(r.size) }}
            </a-tag>
          </a-space>
        </a-descriptions-item>
      </a-descriptions>
      <div class="preview__actions">
        <a-button @click="preview = null">
          清除预览
        </a-button>
        <a-tag color="success" style="margin-left: 8px">✓ 已自动应用，可继续编辑后保存</a-tag>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  CloudUploadOutlined,
  InboxOutlined
} from '@ant-design/icons-vue'
import { adminApi } from '@/api/admin'
import GitUrlForm from './GitUrlForm.vue'
import type { GitImportResult } from '@/types/skill'

const props = defineProps<{ parsedData?: any }>()
const emit = defineEmits<{
  (e: 'apply', data: any): void
  (e: 'update:parsedData', data: any): void
  (e: 'git-imported', result: GitImportResult): void
}>()

const mode = ref<'md' | 'zip' | 'git'>('md')
const loading = ref(false)
const preview = ref<any>(null)

function formatSize(n: number) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

function kindColor(k: string) {
  return {
    script: 'blue',
    reference: 'green',
    asset: 'orange',
    agent: 'purple',
    template: 'cyan',
    theme: 'magenta',
    other: 'default'
  }[k] || 'default'
}

async function handleMdBefore(file: File) {
  loading.value = true
  try {
    const res = await adminApi.uploadSkillMd(file)
    preview.value = res
    emit('update:parsedData', res)
    emit('apply', res)
    message.success(`已解析 ${file.name} 并自动应用`)
  } catch (e: any) {
    message.error(e?.bizResponse?.message || e?.message || '上传失败')
  } finally {
    loading.value = false
  }
  return false
}

async function handleZipBefore(file: File) {
  loading.value = true
  try {
    const res = await adminApi.uploadSkillZip(file)
    preview.value = res
    emit('update:parsedData', res)
    emit('apply', res)
    message.success(`已解压 ${file.name}（${res.resources?.length || 0} 个文件）并自动应用`)
  } catch (e: any) {
    message.error(e?.bizResponse?.message || e?.message || '上传失败')
  } finally {
    loading.value = false
  }
  return false
}

function onGitImported(res: GitImportResult) {
  emit('git-imported', res)
}

function apply() {
  if (!preview.value) return
  emit('apply', preview.value)
}
</script>

<style scoped lang="scss">
.skill-uploader {
  margin-bottom: 16px;
  :deep(.ant-upload-drag-icon) {
    color: #1677ff;
    font-size: 48px;
  }
  :deep(.ant-upload-text) {
    font-size: 15px;
    font-weight: 500;
    margin: 8px 0;
  }
}
.preview {
  margin-top: 16px;
  &__actions {
    margin-top: 12px;
    display: flex;
    gap: 8px;
  }
}
</style>
