<!--
  S40: 普通用户上传 Skill 全屏入口（/upload）
  - 壳页面：复用 SkillUploadModal 组件，open=true 常开
  - 页面顶部加 a-page-header + 返回按钮（"回到首页"）
  - 主要快捷入口在 AppHeader 顶栏用户下拉菜单里
-->
<template>
  <div class="user-skill-new" data-testid="user-skill-new">
    <a-page-header
      title="上传 Skill"
      sub-title="分享你的 Skill 到 SkillsMap 社区 → 选分类 → 立即展示"
      @back="() => $router.push('/')"
    />
    <SkillUploadModal :open="modalOpen" @update:open="(v) => (modalOpen = v)" @close="onClose" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SkillUploadModal from '@/components/SkillUploadModal.vue'

const router = useRouter()
const modalOpen = ref(true)

function onClose() {
  // 用户关闭弹窗 → 回到首页
  router.push('/')
}
</script>

<style scoped lang="scss">
.user-skill-new {
  max-width: 900px;
  margin: 0 auto;
}
</style>
