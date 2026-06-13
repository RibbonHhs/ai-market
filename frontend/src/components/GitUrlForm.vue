<template>
  <div class="git-url-form">
    <a-form layout="vertical" :model="form" :rules="rules" ref="formRef">
      <a-form-item label="仓库 URL" required name="url">
        <a-input
          v-model:value="form.url"
          placeholder="https://github.com/user/repo(.git)"
          :disabled="loading"
          allow-clear
        />
        <small class="hint">
          支持 GitHub / GitLab / Gitea · URL 末尾 .git 可选
        </small>
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="Branch / Tag / Commit SHA">
            <a-input
              v-model:value="form.ref"
              placeholder="main"
              :disabled="loading"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="子目录（可选）">
            <a-input
              v-model:value="form.subdir"
              placeholder="（仓库根目录）"
              :disabled="loading"
            />
            <small class="hint">高级：仅扫描指定子目录</small>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="Username（私有仓库）">
            <a-input
              v-model:value="form.username"
              :disabled="loading"
              autocomplete="username"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="Access Token（私有仓库）">
            <a-input-password
              v-model:value="form.token"
              :disabled="loading"
              autocomplete="current-password"
              placeholder="ghp_xxx / glpat-xxx / 自建 token"
            />
            <small class="hint hint--warn">
              ⚠️ 仅本次保存,后端 Jasypt 加密入库,前端永不回显
            </small>
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item>
        <a-checkbox v-model:checked="form.insecureSkipTls" :disabled="loading">
          跳过 TLS 证书校验（自建 Gitea / GitLab 用）
        </a-checkbox>
        <a-alert
          v-if="form.insecureSkipTls"
          type="warning"
          show-icon
          message="仅用于自建 Gitea / GitLab 自签证书,公网仓库请勿勾选"
          style="margin-top: 8px"
        />
      </a-form-item>

      <a-form-item>
        <a-button
          type="primary"
          size="large"
          :loading="loading"
          :disabled="!form.url"
          @click="onSubmit"
          block
        >
          克隆并解析
        </a-button>
      </a-form-item>

      <a-alert
        v-if="errorMsg"
        type="error"
        show-icon
        closable
        :message="errorMsg"
        style="margin-top: 8px"
        @close="errorMsg = ''"
      />
    </a-form>

    <!-- 成功结果预览 -->
    <div v-if="result" class="result" style="margin-top: 16px">
      <a-divider>解析结果</a-divider>
      <a-alert
        :type="result.totalImported > 0 ? 'success' : 'warning'"
        show-icon
        :message="`发现 ${result.totalDiscovered} 个目录 · 导入 ${result.totalImported} · 跳过 ${result.totalSkipped}`"
      />
      <a-list
        :data-source="result.discovered"
        size="small"
        style="margin-top: 12px"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a-space>
                  <strong>{{ item.name }}</strong>
                  <a-tag v-if="item.action === 'created'" color="green">新建</a-tag>
                  <a-tag v-else-if="item.action === 'updated'" color="blue">更新</a-tag>
                  <a-tag v-else color="default">跳过</a-tag>
                </a-space>
              </template>
              <template #description>
                <small>{{ item.path }} · {{ item.description || '（无描述）' }}</small>
                <div v-if="item.skipReason" style="color: #999">{{ item.skipReason }}</div>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api/admin'
import type { GitImportRequest, GitImportResult } from '@/types/skill'

const emit = defineEmits<{
  (e: 'imported', result: GitImportResult): void
}>()

const formRef = ref()
const loading = ref(false)
const errorMsg = ref('')
const result = ref<GitImportResult | null>(null)

const form = reactive<GitImportRequest>({
  url: '',
  ref: '',
  username: '',
  token: '',
  insecureSkipTls: false
})

const rules = {
  url: [
    { required: true, message: 'URL 必填' },
    { type: 'string' as const, max: 500, message: 'URL 过长（> 500 字符）' },
    {
      pattern: /^https?:\/\/[\w.\-/]+$/i,
      message: 'URL 必须以 http:// 或 https:// 开头'
    }
  ]
}

async function onSubmit() {
  if (!form.url) {
    message.error('请填写仓库 URL')
    return
  }
  errorMsg.value = ''
  result.value = null
  loading.value = true
  try {
    const res = await adminApi.importSkillFromGit({
      url: form.url,
      ref: form.ref || undefined,
      username: form.username || undefined,
      token: form.token || undefined,
      insecureSkipTls: form.insecureSkipTls
    })
    result.value = res
    message.success(`导入完成：${res.totalImported} 个新建/更新 · ${res.totalSkipped} 跳过`)
    emit('imported', res)
  } catch (e: any) {
    // 不暴露后端原始 message（避免泄露用户名格式 / token）
    const code = e?.bizResponse?.code
    const msgMap: Record<number, string> = {
      50300: 'Git 源功能未启用',
      50301: '仓库不存在或不可达',
      50302: '鉴权失败,请检查 Username / Access Token',
      50303: 'TLS 证书校验失败（自建仓库请勾选"跳过 TLS"）',
      50304: '仓库内未发现符合 Agent Skills 规范的子目录',
      50305: 'Clone 超时（>30s）',
      50306: '磁盘空间不足,需 >= 500MB',
      50309: 'URL 格式非法'
    }
    errorMsg.value = msgMap[code] || e?.bizResponse?.message || e?.message || '导入失败'
    message.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.git-url-form {
  .hint {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: #999;
    line-height: 1.5;
    &--warn {
      color: #d48806;
    }
  }
}
.result {
  :deep(.ant-list-item) {
    padding: 8px 0;
  }
}
</style>
