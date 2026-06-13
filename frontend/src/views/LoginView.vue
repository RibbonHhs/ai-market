<template>
  <a-layout class="auth-layout">
    <div class="auth-card">
      <h1 class="auth-title">🔐 登录 SkillsMap</h1>
      <p class="auth-subtitle">管理你的收藏、评分、发布</p>
      <a-form
        layout="vertical"
        :model="form"
        :rules="rules"
        @finish="onSubmit"
      >
        <a-form-item label="用户名" name="username" :rules="rules.username">
          <a-input v-model:value="form.username" size="large" placeholder="admin" />
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="rules.password">
          <a-input-password v-model:value="form.password" size="large" placeholder="admin123" />
        </a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">登录</a-button>
      </a-form>
      <div class="auth-tip">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
      <a-alert
        type="info"
        show-icon
        style="margin-top: 16px"
        message="默认账号"
        description="admin / admin123 (管理员) 或 user / user123 (普通用户)"
      />
    </div>
  </a-layout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  loading.value = true
  try {
    await auth.loginByUsername(form.username, form.password)
    message.success(`欢迎回来，${auth.displayName}！`)
    const redirect = (route.query.redirect as string) || (auth.isAdmin ? '/admin/dashboard' : '/')
    router.push(redirect)
  } catch (e: any) {
    if (e?.bizResponse?.code === 50004 || e?.code === 50004) {
      message.error('用户名已存在')
    } else if (e?.bizResponse?.code === 40100 || e?.code === 40100) {
      message.error('用户名或密码错误')
    } else {
      // 拦截器已统一提示
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
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
