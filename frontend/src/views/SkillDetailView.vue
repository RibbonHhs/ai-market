<template>
  <a-layout class="app-layout">
    <AppHeader />
    <a-layout-content class="app-content">
      <a-spin :spinning="loading" tip="加载中...">
        <div v-if="skill" class="detail" data-testid="skill-detail">
          <!-- 顶部信息 -->
          <a-card class="detail__header">
            <div class="header-flex">
              <SkillLogo :name="skill.name" :size="64" />
              <div class="header-info">
                <h1 class="title">
                  {{ skill.displayName || skill.name }}
                  <a-tag v-if="skill.featured" color="gold">★ 精选</a-tag>
                  <a-tag v-if="skill.version" color="cyan">v{{ skill.version }}</a-tag>
                </h1>
                <div class="badges">
                  <span class="badge">📦 {{ skill.name }}</span>
                  <span v-if="skill.license" class="badge">📄 {{ skill.license }}</span>
                  <span v-if="skill.authorName" class="badge">👤 {{ skill.authorName }}</span>
                </div>
                <!-- S32: 职业 + 用途 同行（<UsageChip kind> 区分类型图标：occupation→ToolOutlined，usage→AimOutlined） -->
                <div class="detail__categories">
                  <UsageChip
                    v-if="skill.categoryName"
                    kind="occupation"
                    :parent-name="skill.categoryName"
                    size="md"
                    clickable
                    :to="skill.categorySlug ? { name: 'category-browse', params: { slug: skill.categorySlug } } : undefined"
                    testid="skill-detail-soc-chip"
                  />
                  <UsageChip
                    v-if="skill.usageCategory"
                    kind="usage"
                    :parent-code="skill.usageCategory.parentCode"
                    :parent-name="skill.usageCategory.parentName"
                    :child-name="skill.usageCategory.name"
                    size="md"
                    clickable
                    :to="skill.usageCategorySlug ? { name: 'category-browse', params: { slug: skill.usageCategorySlug } } : undefined"
                    testid="skill-detail-usage-chip"
                  />
                </div>
                <p class="desc">{{ skill.description }}</p>
                <div class="actions">
                  <a-button type="primary" @click="onInstall">
                    <DownloadOutlined /> 安装
                  </a-button>
                  <a-button @click="onCopy">
                    <CopyOutlined /> 复制安装命令
                  </a-button>
                  <a-button
                    :type="skill.favorited ? 'primary' : 'default'"
                    @click.stop="onToggleFav"
                  >
                    <StarFilled v-if="skill.favorited" style="color: #faad14" />
                    <StarOutlined v-else />
                    {{ skill.favorited ? '已收藏' : '收藏' }}
                  </a-button>
                  <a-button v-if="skill.homepage" :href="skill.homepage" target="_blank">
                    <LinkOutlined /> 主页
                  </a-button>
                </div>
                <div class="meta-stats">
                  <span>⭐ {{ (skill.ratingAvg || 0).toFixed(1) }} ({{ skill.ratingCount || 0 }})</span>
                  <span>⬇ {{ skill.installs || 0 }}</span>
                  <span>👁 {{ skill.views || 0 }}</span>
                </div>
              </div>
            </div>
          </a-card>

          <a-row :gutter="16" style="margin-top: 16px">
            <a-col :xs="24" :md="16">
              <!-- S32: 用途分类 chip 已上移至顶部 header（与 SkillCard / OccupationCard 统一） -->

              <!-- 标签 -->
              <div v-if="(skill.tags || []).length" class="detail__tags">
                <a-tag v-for="t in skill.tags" :key="t" color="blue">{{ t }}</a-tag>
              </div>

              <!-- 完整内容（SKILL.md 渲染） -->
              <a-card title="📖 详细介绍" class="detail__markdown">
                <MarkdownView v-if="skill.body" :source="skill.body" />
                <a-empty v-else description="暂无内容" />
              </a-card>

              <!-- 评分列表 -->
              <a-card title="💬 用户评价" class="detail__reviews">
                <template #extra>
                  <a-button type="primary" size="small" @click="showReviewModal = true">
                    ✏️ 写评价
                  </a-button>
                </template>
                <a-list :data-source="reviews" :loading="reviewLoading" item-layout="vertical">
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <a-list-item-meta>
                        <template #avatar>
                          <a-avatar>{{ item.userAvatar || '🙂' }}</a-avatar>
                        </template>
                        <template #title>
                          {{ item.username }}
                          <a-rate :value="item.rating" disabled :count="5" style="font-size: 12px; margin-left: 8px" />
                        </template>
                        <template #description>
                          {{ item.createTime }}
                        </template>
                      </a-list-item-meta>
                      <p style="margin-top: 8px">{{ item.comment }}</p>
                    </a-list-item>
                  </template>
                </a-list>
                <a-empty v-if="!reviews.length && !reviewLoading" description="还没有评价，来抢沙发吧" />
              </a-card>
            </a-col>

            <a-col :xs="24" :md="8">
              <!-- 下载 Skill 包 -->
              <a-card title="📦 下载 Skill 包" class="download-card">
                <p style="margin-top: 0; color: #666; font-size: 13px">
                  完整包（ZIP 格式）包含 <code>SKILL.md</code> 及 <code>scripts/</code> <code>references/</code> <code>assets/</code> 等所有资源
                </p>
                <a-button
                  type="primary"
                  size="large"
                  block
                  @click="onDownload"
                >
                  <DownloadOutlined /> 下载 {{ skill.name }}.zip
                </a-button>
                <div class="download-hint">
                  <span>💡 提示：zip 文件可解压查看完整内容</span>
                </div>
              </a-card>

              <!-- 安装信息 -->
              <a-card title="🚀 安装" style="margin-top: 16px">
                <p style="margin-top: 0">运行以下命令安装：</p>
                <pre class="install-cmd">{{ skill.installCommand || 'npx skills add ' + skill.name }}</pre>
              </a-card>

              <!-- 兼容信息 -->
              <a-card v-if="skill.allowedTools || skill.compatibility" title="🔧 兼容性" style="margin-top: 16px">
                <div v-if="skill.allowedTools" class="kv">
                  <strong>允许工具:</strong>
                  <div class="tag-list">
                    <a-tag v-for="t in skill.allowedTools.split(',')" :key="t">{{ t.trim() }}</a-tag>
                  </div>
                </div>
                <div v-if="skill.compatibility" class="kv" style="margin-top: 12px">
                  <strong>兼容客户端:</strong>
                  <div class="tag-list">
                    <a-tag v-for="t in skill.compatibility.split(',')" :key="t" color="cyan">{{ t.trim() }}</a-tag>
                  </div>
                </div>
              </a-card>

              <!-- 元信息 -->
              <a-card title="ℹ️ 元信息" style="margin-top: 16px">
                <a-descriptions :column="1" size="small">
                  <a-descriptions-item v-if="skill.authorName" label="作者">{{ skill.authorName }}</a-descriptions-item>
                  <a-descriptions-item v-if="skill.authorEmail" label="邮箱">{{ skill.authorEmail }}</a-descriptions-item>
                  <a-descriptions-item v-if="skill.authorGithub" label="GitHub">
                    <a :href="`https://github.com/${skill.authorGithub}`" target="_blank">@{{ skill.authorGithub }}</a>
                  </a-descriptions-item>
                  <a-descriptions-item v-if="skill.source" label="来源">
                    <a-tag>{{ skill.source }}</a-tag>
                  </a-descriptions-item>
                  <a-descriptions-item label="发布时间">{{ skill.createTime }}</a-descriptions-item>
                </a-descriptions>
              </a-card>
            </a-col>
          </a-row>
        </div>
      </a-spin>
    </a-layout-content>

    <!-- 评分弹窗 -->
    <a-modal v-model:open="showReviewModal" title="写评价" @ok="onSubmitReview" okText="提交" cancelText="取消">
      <a-form layout="vertical">
        <a-form-item label="评分">
          <a-rate v-model:value="reviewForm.rating" :count="5" />
        </a-form-item>
        <a-form-item label="评论（可选）">
          <a-textarea v-model:value="reviewForm.comment" :rows="4" placeholder="分享你的使用体验..." />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CopyOutlined,
  DownloadOutlined,
  LinkOutlined,
  StarFilled,
  StarOutlined
} from '@ant-design/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import UsageChip from '@/components/UsageChip.vue'
import { skillApi } from '@/api/skill'
import { reviewApi, favoriteApi } from '@/api/review'
import { useAuthStore } from '@/stores/auth'
import type { Skill, Review } from '@/types/skill'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const skill = ref<Skill | null>(null)
const reviews = ref<Review[]>([])
const reviewLoading = ref(false)
const showReviewModal = ref(false)
const reviewForm = reactive({ rating: 5, comment: '' })

async function load() {
  loading.value = true
  try {
    const slug = String(route.params.slug)
    skill.value = await skillApi.detailBySlug(slug)
    await loadReviews()
  } catch (e: any) {
    if (e?.bizResponse?.code === 50001 || e?.code === 50001) {
      router.push('/browse')
    }
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  if (!skill.value) return
  reviewLoading.value = true
  try {
    const data = await skillApi.reviews(skill.value.id, 1, 20)
    reviews.value = data.records
  } finally {
    reviewLoading.value = false
  }
}

function onInstall() {
  if (!auth.isLoggedIn) {
    message.warning('请先登录')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  message.success('安装命令已复制到剪贴板')
  navigator.clipboard?.writeText(skill.value?.installCommand || '')
}

function onCopy() {
  navigator.clipboard?.writeText(skill.value?.installCommand || '')
  message.success('已复制')
}

function onDownload() {
  if (!skill.value) return
  const slug = skill.value.slug || skill.value.name
  const url = `/api/skills/slug/${encodeURIComponent(slug)}/download`
  // 触发浏览器下载
  const a = document.createElement('a')
  a.href = url
  a.download = `${skill.value.name}.zip`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  message.success('已下载 ' + (skill.value.name || '') + '.zip')
}

async function onToggleFav() {
  console.log('[favorite] click, auth.isLoggedIn=', auth.isLoggedIn, 'skill=', skill.value)
  if (!auth.isLoggedIn) {
    message.warning('请先登录后再收藏')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!skill.value) {
    message.error('Skill 数据未加载')
    return
  }
  const wasFavorited = !!skill.value.favorited
  // 立即翻转 UI（乐观更新）
  skill.value.favorited = !wasFavorited
  try {
    if (wasFavorited) {
      await favoriteApi.remove(skill.value.id)
      message.success('已取消收藏')
    } else {
      await favoriteApi.add(skill.value.id)
      message.success('已收藏')
    }
  } catch (e: any) {
    // 失败时回滚
    skill.value.favorited = wasFavorited
    console.error('[favorite] failed:', e)
    message.error(e?.bizResponse?.message || e?.message || '操作失败')
  }
}

async function onSubmitReview() {
  if (!auth.isLoggedIn) {
    message.warning('请先登录')
    showReviewModal.value = false
    router.push({ name: 'login' })
    return
  }
  if (!skill.value) return
  try {
    await reviewApi.submit(skill.value.id, reviewForm.rating, reviewForm.comment)
    message.success('评价成功')
    showReviewModal.value = false
    reviewForm.comment = ''
    reviewForm.rating = 5
    // 刷新详情（含平均分）
    await load()
  } catch {
    /* interceptor 提示过 */
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}
.app-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  width: 100%;
}
.detail__header {
  border-radius: 12px;
  background: var(--bg-primary);
}
.header-flex {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.header-info {
  flex: 1;
  min-width: 0;
}
.title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  .badge {
    font-size: 12px;
    color: var(--text-secondary);
    background: var(--bg-secondary);
    padding: 4px 8px;
    border-radius: 4px;
  }
}
.desc {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-secondary);
  margin: 12px 0;
}
.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.meta-stats {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}
.detail__tags {
  background: var(--bg-secondary);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
/* S32+: 职业 + 用途 双 chip 同行（样式由 <UsageChip> 统一管） */
.detail__categories {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 10px;
}
.detail__markdown,
.detail__reviews {
  border-radius: 10px;
  margin-bottom: 16px;
  background: var(--bg-primary);
}
.install-cmd {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  border: 1px solid var(--border-color);
}
.download-card {
  :deep(.ant-card-body) {
    padding: 16px;
  }
  .download-hint {
    margin-top: 12px;
    padding: 8px 12px;
    background: var(--bg-tertiary);
    border-radius: 6px;
    color: var(--primary);
    font-size: 12px;
  }
}
.kv strong {
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
  color: var(--text-primary);
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
