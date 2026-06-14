/**
 * Vue Router 配置
 * - meta.requiresAuth / meta.requiresAdmin
 * - 前置守卫：未登录跳 /login，ADMIN-only 受保护
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/browse',
    name: 'browse',
    component: () => import('@/views/BrowseView.vue'),
    meta: { title: '浏览 Skills' }
  },
  {
    path: '/skills/:slug',
    name: 'skill-detail',
    component: () => import('@/views/SkillDetailView.vue'),
    meta: { title: 'Skill 详情' }
  },
  {
    path: '/occupations',
    name: 'occupations',
    component: () => import('@/views/CategoryView.vue'),
    meta: { title: '职业技能', dim: 'SOC' }
  },
  {
    path: '/categories',
    name: 'categories',
    component: () => import('@/views/CategoryView.vue'),
    meta: { title: '用途分类', dim: 'USAGE' }
  },
  {
    path: '/categories/:slug',
    name: 'category-browse',
    component: () => import('@/views/BrowseView.vue'),
    meta: { title: '分类浏览' }
  },
  {
    path: '/api-guide',
    name: 'api-guide',
    component: () => import('@/views/ApiGuideView.vue'),
    meta: { title: 'API 接入' }
  },
  {
    path: '/newbie-guide',
    name: 'newbie-guide',
    component: () => import('@/views/NewbieGuideView.vue'),
    meta: { title: '新手指引' }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/me',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/upload',
    name: 'upload-skill',
    // S40: 普通用户上传 Skill（首页 Hero CTA 入口）
    component: () => import('@/views/UploadSkillView.vue'),
    meta: { title: '上传 Skill', requiresAuth: true }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, layout: 'admin' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: 'Dashboard' }
      },
      {
        path: 'skills',
        name: 'admin-skills',
        component: () => import('@/views/admin/AdminSkillListView.vue'),
        meta: { title: 'Skill 管理' }
      },
      {
        path: 'skills/new',
        name: 'admin-skill-new',
        // S38: 一段式上传页（drag + 表单 → 立即发布）
        component: () => import('@/views/admin/AdminSkillsNewView.vue'),
        meta: { title: '上传 Skill' }
      },
      {
        path: 'skills/:id/edit',
        name: 'admin-skill-edit',
        component: () => import('@/views/admin/AdminSkillEditView.vue'),
        meta: { title: '编辑 Skill' }
      },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/views/admin/AdminCategoryView.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'categories/usage',
        name: 'admin-categories-usage',
        component: () => import('@/views/admin/AdminUsageCategoryView.vue'),
        meta: { title: '用途分类管理' }
      },
      {
        path: 'tags',
        name: 'admin-tags',
        component: () => import('@/views/admin/AdminTagView.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUserView.vue'),
        meta: { title: '用户管理' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // S22: 锚点深链支持（如 /api-guide#examples 自动滚到示例区）
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth', top: 24 }
    }
    return { top: 0 }
  },
  routes
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  document.title = `${to.meta.title || 'SkillsMap'} · SkillsMap`

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    next({ name: 'home' })
    return
  }
  next()
})

export default router
