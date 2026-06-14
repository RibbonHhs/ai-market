<!--
  S40: Skill 上传弹窗
  - 抽自 UploadSkillView.vue 的核心 form 逻辑
  - 用 a-modal 包裹，props.open 控制显隐，emit update:open / close 通知父组件
  - 父组件：AppHeader (顶栏 dropdown 快捷入口) / UploadSkillView (全屏入口)
-->
<template>
  <a-modal
    :open="open"
    :title="null"
    :width="720"
    :footer="null"
    :destroy-on-close="true"
    :mask-closable="!uploading"
    @cancel="handleClose"
  >
    <div class="skill-upload-modal__head">
      <h3 class="skill-upload-modal__title">📤 上传我的 Skill</h3>
      <p class="skill-upload-modal__sub">分享到 SkillsMap 社区 → 选分类 → 立即展示</p>
    </div>

    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 12px"
      message="zip 必须含 SKILL.md（YAML frontmatter 必填 name + description），最大 10MB"
    />

    <a-form
      v-if="formReady"
      layout="vertical"
      :model="form"
      :label-col="{ style: { width: '100%' } }"
      @finish="onSubmit"
    >
      <a-row :gutter="16">
        <a-col :xs="24" :md="14">
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
              <p class="ant-upload-drag-icon"><InboxOutlined /></p>
              <p class="ant-upload-drag-text">
                {{ selectedFile ? '✓ 已选择文件，点击或拖拽可替换' : '点击或拖拽 .skill / .zip 到这里' }}
              </p>
              <p v-if="selectedFile" class="ant-upload-drag-hint">
                {{ selectedFile.name }}（{{ formatSize(selectedFile.size) }}）
              </p>
              <p v-else class="ant-upload-drag-hint">解析 SKILL.md frontmatter 后自动发布到平台</p>
            </a-upload-dragger>
          </a-form-item>
        </a-col>

        <a-col :xs="24" :md="10">
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

          <a-form-item label="USAGE 维度（可选）">
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

          <a-form-item label="标签（可选，回车确认）">
            <a-select
              v-model:value="form.tagSlugs"
              mode="tags"
              placeholder="输入标签后回车"
              :max-tag-count="6"
              data-testid="select-tags"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-space direction="vertical" style="width: 100%" :size="10">
        <a-button
          type="primary"
          html-type="submit"
          size="large"
          :loading="uploading"
          :disabled="!canSubmit"
          block
          data-testid="submit-upload"
        >
          <template #icon><CloudUploadOutlined /></template>
          {{ uploading ? `上传中 ${progress}%` : '上传并发布' }}
        </a-button>
        <a-progress
          v-if="uploading"
          :percent="progress"
          :show-info="false"
          :status="progress < 100 ? 'active' : 'success'"
        />
      </a-space>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { InboxOutlined, CloudUploadOutlined } from '@ant-design/icons-vue'
import { skillUploadApi } from '@/api/skillUpload'
import { categoryApi } from '@/api/skill'
import { BizCode } from '@/axios/biz-code'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/skill'

const props = defineProps<{
  open: boolean
}>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'close'): void
}>()

const router = useRouter()
const auth = useAuthStore()

// ===== 移动端检测 =====
const isMobile = ref(false)
function updateIsMobile() {
  isMobile.value = window.innerWidth < 768
}

// ===== 状态 =====
const formReady = ref(true)
const selectedFile = ref<File | null>(null)
const categoryIdPath = ref<number[]>([])
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

function onCategoryPathChange(path: number[]) {
  form.categoryId = path[path.length - 1]
  if (form.categoryId) errors.categoryId = undefined
}

const canSubmit = computed(() => !!selectedFile.value && !!form.categoryId && !uploading.value)

// ===== 文件处理 =====
const MAX_FILE_SIZE = 10 * 1024 * 1024
const ACCEPTED_EXTS = ['.zip', '.skill']

function formatSize(n: number): string {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

function handleBeforeUpload(file: File): boolean {
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
    socFlatOptions.value = flattenSoc(socTree)
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

// cascader v-model:value 同步 → form.categoryId
watch(categoryIdPath, (v) => onCategoryPathChange(v || []))

// ===== 业务错误处理 =====
interface BizFailure {
  code?: number
  message?: string
  bizResponse?: { code: number; message: string }
}

function handleBizError(err: BizFailure): boolean {
  const code = err?.bizResponse?.code ?? err?.code ?? 0
  const msg = err?.bizResponse?.message ?? err?.message ?? '上传失败'

  switch (code) {
    case BizCode.UNAUTHORIZED:
    case BizCode.TOKEN_INVALID:
    case BizCode.TOKEN_EXPIRED:
      message.warning('登录已过期，请重新登录')
      setTimeout(() => {
        emit('update:open', false)
        emit('close')
        router.push({ name: 'login', query: { redirect: '/upload' } })
      }, 100)
      return true
    case BizCode.UPLOAD_FILE_INVALID:
    case BizCode.UPLOAD_NO_SKILLMD:
    case BizCode.UPLOAD_FRONTMATTER:
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
      if (code >= 500 && code < 600) {
        message.error('服务器错误，已通知运维')
      } else {
        message.error(msg)
      }
      return false
  }
}

function handleClose() {
  if (uploading.value) {
    message.warning('上传进行中，请稍候…')
    return
  }
  emit('update:open', false)
  emit('close')
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
    handleClose()
    router.push({ name: 'login', query: { redirect: '/upload' } })
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
    // 关闭弹窗，跳详情页
    setTimeout(() => {
      emit('update:open', false)
      emit('close')
      router.push(`/skills/${result.slug}`)
    }, 800)
  } catch (err) {
    handleBizError(err as BizFailure)
  } finally {
    uploading.value = false
    setTimeout(() => {
      if (!uploading.value) progress.value = 0
    }, 800)
  }
}

// ===== 生命周期 =====
onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
  loadCategories()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIsMobile)
})
</script>

<style scoped lang="scss">
.skill-upload-modal {
  &__head {
    margin-bottom: 12px;
  }
  &__title {
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
  }
  &__sub {
    margin: 0;
    font-size: 13px;
    color: var(--text-secondary);
  }
}
:deep(.ant-modal-body) {
  padding: 20px 24px;
}
.ant-upload-drag-icon {
  color: var(--primary);
  font-size: 42px;
  margin-bottom: 6px;
}
.ant-upload-drag-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 6px 0;
}
.ant-upload-drag-hint {
  color: var(--text-tertiary);
  font-size: 12px;
}
:deep(.ant-upload-drag) {
  background: var(--bg-primary);
  border-color: var(--border);
  &:hover {
    border-color: var(--primary);
  }
}
</style>
