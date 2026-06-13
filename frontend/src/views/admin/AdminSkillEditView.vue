<template>
  <div>
    <a-page-header :title="isEdit ? '编辑 Skill' : '新建 Skill'" @back="() => $router.push('/admin/skills')" />

    <!-- 文件上传导入（仅新建时可用） -->
    <SkillUploader
      v-if="!isEdit"
      :parsed-data="lastParsed"
      @update:parsed-data="(v: any) => (lastParsed = v)"
      @apply="onApplyFromUpload"
      style="margin-top: 16px"
    />

    <a-card title="✏️ 表单信息" style="margin-top: 16px">
      <a-form
        :model="form"
        layout="vertical"
        :label-col="{ style: { width: '120px' } }"
        @finish="onSubmit"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="名称 (kebab-case)" required>
              <a-input v-model:value="form.name" :disabled="isEdit" placeholder="my-skill" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Slug" required>
              <a-input v-model:value="form.slug" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示名">
              <a-input v-model:value="form.displayName" placeholder="My Skill" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="图标 (emoji)">
              <a-input v-model:value="form.icon" placeholder="📦" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="分类">
              <a-select v-model:value="form.categoryId" allow-clear>
                <a-select-option v-for="c in categories" :key="c.id" :value="c.id">
                  {{ c.icon }} {{ c.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="版本">
              <a-input v-model:value="form.version" placeholder="1.0.0" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="License">
              <a-input v-model:value="form.license" placeholder="MIT" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="来源">
              <a-select v-model:value="form.source">
                <a-select-option value="official">Official</a-select-option>
                <a-select-option value="community">Community</a-select-option>
                <a-select-option value="private">Private</a-select-option>
                <a-select-option value="imported">Imported</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="简介" required>
              <a-textarea v-model:value="form.description" :rows="2" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="作者名">
              <a-input v-model:value="form.authorName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="作者邮箱">
              <a-input v-model:value="form.authorEmail" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="GitHub">
              <a-input v-model:value="form.authorGithub" placeholder="username" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="主页">
              <a-input v-model:value="form.homepage" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标签 (逗号分隔)">
              <a-input v-model:value="tagsText" placeholder="claude, ai, productivity" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="允许工具 (逗号分隔)">
              <a-input v-model:value="form.allowedTools" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="安装命令">
              <a-input v-model:value="form.installCommand" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="正文 (Markdown)">
              <a-textarea v-model:value="form.body" :rows="12" placeholder="# My Skill\n\nWrite your SKILL.md content..." />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="状态">
              <a-select v-model:value="form.status">
                <a-select-option value="draft">草稿</a-select-option>
                <a-select-option value="published">发布</a-select-option>
                <a-select-option value="deprecated">废弃</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="首页精选">
              <a-switch v-model:checked="form.featured" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="saving" size="large">保存</a-button>
          <a-button style="margin-left: 8px" @click="$router.back()">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import SkillUploader from '@/components/SkillUploader.vue'
import { adminApi } from '@/api/admin'
import { categoryApi } from '@/api/skill'
import type { Category } from '@/types/skill'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const categories = ref<Category[]>([])
const tagsText = ref('')
const lastParsed = ref<any>(null) // v-model:parsedData 兜底通道

const form = reactive<any>({
  name: '',
  slug: '',
  displayName: '',
  icon: '📦',
  categoryId: undefined,
  version: '1.0.0',
  license: 'MIT',
  source: 'community',
  description: '',
  authorName: '',
  authorEmail: '',
  authorGithub: '',
  homepage: '',
  allowedTools: '',
  installCommand: '',
  body: '',
  status: 'draft',
  featured: false
})

/**
 * 处理 SkillUploader 解析后的数据 → 回填表单
 */
function onApplyFromUpload(res: any) {
  const p = res.preview || {}
  if (p.name) form.name = p.name
  if (p.name) form.slug = p.name
  if (p.description) form.description = p.description
  if (p.license) form.license = p.license
  if (p.version) form.version = p.version
  if (p.allowedTools) form.allowedTools = p.allowedTools
  if (p.compatibility) form.compatibility = p.compatibility
  // 正文
  if (res.body) form.body = res.body
  // 显示名（如果没有就格式化为 Title Case）
  if (!form.displayName && p.name) {
    form.displayName = p.name.split('-').map((s: string) => s.charAt(0).toUpperCase() + s.slice(1)).join(' ')
  }
  // 安装命令
  if (!form.installCommand && p.name) {
    form.installCommand = `npx skills add ${p.name}`
  }
  // 滚动到表单
  setTimeout(() => {
    document.querySelector('.ant-card')?.scrollIntoView({ behavior: 'smooth' })
  }, 100)
}

onMounted(async () => {
  categories.value = await categoryApi.list()
  if (isEdit.value) {
    const data = await adminApi.getSkill(Number(route.params.id))
    Object.assign(form, data)
    tagsText.value = (data.tags || []).join(', ')
  }
})

// v-model 兜底：无论 @apply 是否触发，parsedData 变化都会同步到表单
watch(lastParsed, (res) => {
  if (res && Object.keys(res).length > 0) {
    onApplyFromUpload(res)
  }
})

async function onSubmit() {
  if (!form.name.match(/^[a-z0-9-]+$/)) {
    message.error('name 必须为 kebab-case（小写字母+数字+连字符）')
    return
  }
  if (!form.description) {
    message.error('请填写简介')
    return
  }
  form.tags = JSON.stringify(
    tagsText.value
      .split(',')
      .map((t: string) => t.trim())
      .filter(Boolean)
  )
  form.slug = form.slug || form.name
  saving.value = true
  try {
    if (isEdit.value) {
      await adminApi.updateSkill(Number(route.params.id), form)
      message.success('已更新')
    } else {
      await adminApi.createSkill(form)
      message.success('已创建')
    }
    router.push('/admin/skills')
  } catch { /* interceptor */ } finally {
    saving.value = false
  }
}
</script>
