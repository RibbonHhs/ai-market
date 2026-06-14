<!--
  S38: Admin - 一段式上传 Skill 页（/admin/skills/new）
  - 拖拽 .zip / .skill + 表单 + 提交 → 立即发布
  - 错误码映射见 PRD §4.2
  - 移动端 (<768px) a-cascader 改 a-select 单列
-->
<template>
  <div class="admin-skill-new" data-testid="admin-skill-new">
    <a-page-header
      title="上传 Skill"
      sub-title="选择 .skill zip 包 → 选分类 → 立即发布"
      @back="() => $router.push('/admin/skills')"
    />

    <a-alert
      type="info"
      show-icon
      style="margin-top: 16px"
      message="zip 必须含 SKILL.md（YAML frontmatter 必填 name + description），最大 10MB"
    />

    <a-form
      v-if="formReady"
      layout="vertical"
      :model="form"
      :label-col="{ style: { width: '100%' } }"
      class="upload-form"
      @finish="onSubmit"
    >
      <a-row :gutter="24">
        <a-col :xs="24" :md="14">
          <a-card title="📦 1. 选择 Skill 包" :bordered="false">
            <a-form-item
              label="Skill 包"
              required
              :validate-status="errors.file ? 'error' : ''"
              :help="errors.file || '点击或拖拽 .skill / .zip 到这里（≤ 10MB）'"
            >
              <a-upload-dragger
                :before-upload="handleBeforeUpload"
                :show-upload-list="false"
                accept=".zip,.skill"
                :disabled="uploading"
                :multiple="false"
                :max-count="1"
                data-testid="upload-dragger"
              >
                <p class="ant-upload-drag-icon">
                  <InboxOutlined />
                </p>
                <p class="ant-upload-drag-text">
                  {{ selectedFile ? '✓ 已选择文件，点击或拖拽可替换' : '点击或拖拽 .skill / .zip 到这里' }}
                </p>
                <p v-if="selectedFile" class="ant-upload-drag-hint">
                  {{ selectedFile.name }}（{{ formatSize(selectedFile.size) }}）
                </p>
                <p v-else class="ant-upload-drag-hint">
                  解析 SKILL.md frontmatter 后自动发布到平台
                </p>
              </a-upload-dragger>
            </a-form-item>
          </a-card>
        </a-col>

        <a-col :xs="24" :md="10">
          <a-card title="🏷️ 2. 分类与标签" :bordered="false">
            <a-form-item
              label="SOC 分类（必填）"
              required
              :validate-status="errors.categoryId ? 'error' : ''"
              :help="errors.categoryId || '选择二级职业分类'"
            >
              <a-cascader
                v-if="!isMobile"
                v-model:value="categoryIdPath"
                :options="socOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择职业分类"
                change-on-select
                :disabled="loadingCats"
                data-testid="cascader-soc"
              />
              <a-select
                v-else
                v-model:value="form.categoryId"
                :options="socFlatOptions"
                :field-names="{ label: 'name', value: 'id' }"
                placeholder="请选择职业分类"
                :loading="loadingCats"
                data-testid="select-soc"
              />
            </a-form-item>

            <a-form-item label="USAGE 维度（可选，可多选）">
              <a-select
                v-model:value="form.usageCategoryIds"
                mode="multiple"
                :options="usageOptions"
                :field-names="{ label: 'name', value: 'id' }"
                placeholder="选择用途维度"
                :loading="loadingCats"
                allow-clear
                data-testid="select-usage"
              />
            </a-form-item>

            <a-form-item label="标签（可选，回车确认；缺失自动创建）">
              <a-select
                v-model:value="form.tagSlugs"
                mode="tags"
                placeholder="输入标签后回车"
                :max-tag-count="8"
                data-testid="select-tags"
              />
            </a-form-item>
          </a-card>
        </a-col>
      </a-row>

      <a-card title="🚀 3. 上传" :bordered="false" style="margin-top: 16px">
        <a-space direction="vertical" style="width: 100%" :size="12">
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="uploading"
            :disabled="!canSubmit"
            block
            data-testid="submit-upload"
          >
            <template #icon>
              <CloudUploadOutlined />
            </template>
            {{ uploading ? `上传中 ${progress}%` : '上传并发布' }}
          </a-button>
          <a-progress
            v-if="uploading"
            :percent="progress"
            :show-info="false"
            :status="progress < 100 ? 'active' : 'success'"
          />
        </a-space>
      </a-card>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { InboxOutlined, CloudUploadOutlined } from '@ant-design/icons-vue'
import { skillUploadApi } from '@/api/skillUpload'
import { categoryApi } from '@/api/skill'
import { BizCode } from '@/axios/biz-code'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/skill'

const router = useRouter()
const auth = useAuthStore()

// ===== 移动端检测（<768px 时用 a-select 替代 a-cascader） =====
const isMobile = ref(false)
function updateIsMobile() {
  isMobile.value = window.innerWidth < 768
}
onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIsMobile)
})

// ===== 状态 =====
const formReady = ref(true)
const selectedFile = ref<File | null>(null)
const categoryIdPath = ref<number[]>([])   // a-cascader 用
const socOptions = ref<Category[]>([])
const socFlatOptions = ref<Array<{ id: number; name: string; parentLabel?: string }>>([])
const usageOptions = ref<Category[]>([])
const loadingCats = ref(false)

const uploading = ref(false)
const progress = ref(0)
const errors = reactive<{ file?: string; categoryId?: string }>({})

const form = reactive<{
  categoryId?: number
  usageCategoryIds: number[]
  tagSlugs: string[]
}>({
  categoryId: undefined,
  usageCategoryIds: [],
  tagSlugs: []
})

// 同步 cascader → categoryId
function onCategoryPathChange(path: number[]) {
  form.categoryId = path[path.length - 1]
  if (form.categoryId) errors.categoryId = undefined
}

const canSubmit = computed(() => !!selectedFile.value && !!form.categoryId && !uploading.value)

// ===== 文件处理 =====
const MAX_FILE_SIZE = 10 * 1024 * 1024  // 10MB
const ACCEPTED_EXTS = ['.zip', '.skill']

function formatSize(n: number): string {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

function handleBeforeUpload(file: File): boolean {
  // a-upload 通过 before-upload 返回 false 阻止其内置上传
  const lower = file.name.toLowerCase()
  const ok = ACCEPTED_EXTS.some((ext) => lower.endsWith(ext))
  if (!ok) {
    errors.file = '仅支持 .zip 或 .skill 文件'
    message.error(errors.file)
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    errors.file = `文件 ${formatSize(file.size)} 超过 10MB 限制`
    message.error(errors.file)
    return false
  }
  selectedFile.value = file
  errors.file = undefined
  return false
}

// ===== 分类数据加载 =====
async function loadCategories() {
  loadingCats.value = true
  try {
    const [socTree, usageTree] = await Promise.all([
      categoryApi.tree('SOC'),
      categoryApi.tree('USAGE')
    ])
    socOptions.value = socTree
    // 拍平二级为 a-select 选项（带一级 label 前缀便于辨识）
    socFlatOptions.value = flattenSoc(socTree)
    // USAGE 维度只取二级（一级是"父"分类，前端展示是 L2 chip）
    usageOptions.value = flattenUsage(usageTree)
  } catch (e) {
    console.error('load categories error', e)
    message.error('分类加载失败')
  } finally {
    loadingCats.value = false
  }
}

function flattenSoc(tree: Category[]): Array<{ id: number; name: string; parentLabel?: string }> {
  const out: Array<{ id: number; name: string; parentLabel?: string }> = []
  for (const parent of tree) {
    if (parent.children?.length) {
      for (const child of parent.children) {
        out.push({ id: child.id!, name: child.name!, parentLabel: parent.name })
      }
    }
  }
  return out
}

function flattenUsage(tree: Category[]): Category[] {
  const out: Category[] = []
  for (const parent of tree) {
    if (parent.children?.length) {
      for (const child of parent.children) {
        out.push(child)
      }
    }
  }
  return out
}

onMounted(loadCategories)

// ===== 业务错误处理（5 种）=====
interface BizFailure {
  code?: number
  message?: string
  bizResponse?: { code: number; message: string }
}

function handleBizError(err: BizFailure): boolean {
  // 兼容 error.code（直接抛错）或 err.bizResponse.code（包装）
  const code = err?.bizResponse?.code ?? err?.code ?? 0
  const msg = err?.bizResponse?.message ?? err?.message ?? '上传失败'

  switch (code) {
    case BizCode.UNAUTHORIZED:
    case BizCode.TOKEN_INVALID:
    case BizCode.TOKEN_EXPIRED:
      message.warning('登录已过期，请重新登录')
      // 拦截器已处理跳转；此处再保底
      setTimeout(() => {
        router.push({ name: 'login', query: { redirect: '/admin/skills/new' } })
      }, 100)
      return true
    case BizCode.UPLOAD_FILE_INVALID:
      errors.file = msg
      message.error(msg)
      return true
    case BizCode.UPLOAD_NO_SKILLMD:
      errors.file = msg
      message.error(msg)
      return true
    case BizCode.UPLOAD_FRONTMATTER:
      errors.file = msg
      message.error(msg)
      return true
    case BizCode.UPLOAD_BOMB:
      errors.file = msg
      message.error(msg)
      return true
    case BizCode.CONFLICT:
      message.warning('slug 已存在，建议改名后重试')
      return true
    case BizCode.UPLOAD_TOO_LARGE:
      errors.file = '文件超过 10MB 限制'
      message.error(errors.file)
      return true
    default:
      // 4xx 其他 / 5xx / 网络错：toast
      if (code >= 500 && code < 600) {
        message.error('服务器错误，已通知运维')
      } else if (code === 0) {
        // 网络错（axios 抛错而非业务错）
        message.error(msg)
      } else {
        message.error(msg)
      }
      return false
  }
}

// ===== 提交 =====
async function onSubmit() {
  if (!selectedFile.value) {
    errors.file = '请先选择 .skill zip 包'
    message.error(errors.file)
    return
  }
  if (!form.categoryId) {
    errors.categoryId = '请选择 SOC 分类'
    message.error(errors.categoryId)
    return
  }
  if (!auth.isLoggedIn) {
    message.warning('登录已过期，请重新登录')
    router.push({ name: 'login', query: { redirect: '/admin/skills/new' } })
    return
  }

  uploading.value = true
  progress.value = 0
  errors.file = undefined
  errors.categoryId = undefined

  try {
    const result = await skillUploadApi.uploadSkillZip(
      selectedFile.value,
      form.categoryId,
      form.usageCategoryIds,
      form.tagSlugs,
      {
        onProgress: (pct) => {
          progress.value = pct
        }
      }
    )
    message.success('上传成功！')
    setTimeout(() => {
      router.push(`/skills/${result.slug}`)
    }, 1500)
  } catch (err) {
    handleBizError(err as BizFailure)
  } finally {
    uploading.value = false
    // 保留进度短暂展示
    setTimeout(() => {
      if (!uploading.value) progress.value = 0
    }, 800)
  }
}
</script>

<style scoped lang="scss">
.admin-skill-new {
  max-width: 1100px;
}
.upload-form {
  margin-top: 16px;
}
.ant-upload-drag-icon {
  color: var(--primary);
  font-size: 48px;
  margin-bottom: 8px;
}
.ant-upload-drag-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 8px 0;
}
.ant-upload-drag-hint {
  color: var(--text-tertiary);
  font-size: 13px;
}
:deep(.ant-upload-drag) {
  background: var(--bg-primary);
  border-color: var(--border);
  &:hover {
    border-color: var(--primary);
  }
}
:deep(.ant-form-item-explain-error) {
  color: var(--danger);
}
:deep(.ant-card) {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
}
:deep(.ant-card-head-title) {
  color: var(--text-primary);
}
</style>
