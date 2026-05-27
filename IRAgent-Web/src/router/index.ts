import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '系统概览', icon: 'DashboardOutlined' },
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'UserOutlined' },
      },
      {
        path: 'question-review',
        name: 'QuestionReview',
        component: () => import('@/views/question/Review.vue'),
        meta: { title: '题目审核', icon: 'SafetyOutlined' },
      },
      {
        path: 'api-key',
        name: 'ApiKey',
        component: () => import('@/views/apikey/KeyManage.vue'),
        meta: { title: 'API Key', icon: 'KeyOutlined' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')
  const tokenExp = localStorage.getItem('token_exp')
  const now = Date.now()

  if (to.meta.requiresAuth !== false) {
    if (token && tokenExp && now < Number(tokenExp)) {
      next()
    } else {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('token_exp')
      const redirect = to.path !== '/login' ? to.path : ''
      next({ path: '/login', query: redirect ? { redirect } : {} })
    }
  } else {
    if (token && tokenExp && now < Number(tokenExp) && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
