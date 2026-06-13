<template>
  <a-layout class="auth-layout">
    <div class="auth-card">
      <h1 class="auth-title">🌱 注册账号</h1>
      <p class="auth-subtitle">加入 SkillsMap 社区</p>
      <a-form layout="vertical" @finish="onSubmit">
        <a-form-item label="用户名" required>
          <a-input v-model:value="form.username" size="large" placeholder="字母数字下划线，3-20 位" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="form.email" size="large" placeholder="可选" />
        </a-form-item>
        <a-form-item label="显示名">
          <a-input v-model:value="form.displayName" size="large" placeholder="可选" />
        </a-form-item>
        <a-form-item label="密码" required>
          <a-input-password v-model:value="form.password" size="large" placeholder="至少 6 位" />
        </a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">注册</a-button>
      </a-form>
      <div class="auth-tip">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </a-layout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', email: '', displayName: '', password: '' })

async function onSubmit() {
  if (form.password.length < 6) {
    message.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await auth.register(form.username, form.password, form.email, form.displayName)
    // 自动登录
    await auth.loginByUsername(form.username, form.password)
    message.success('注册成功，已自动登录')
    router.push('/')
  } catch (e: any) {
    if (e?.bizResponse?.code === 50004 || e?.code === 50004) {
      message.error('用户名已存在')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #722ed1 0%, #1677ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.auth-card {
  background: #fff;
  padding: 40px;
  border-radius: 16px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.auth-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  text-align: center;
}
.auth-subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.auth-tip {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #666;
  a {
    color: #1677ff;
    text-decoration: none;
  }
}
</style>
