import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const username = ref('')
  const role = ref('')

  const isLoggedIn = computed(() => !!token.value && !!localStorage.getItem('admin_token'))
  const isAdmin = computed(() => role.value === 'admin')

  function restoreSession() {
    const savedToken = localStorage.getItem('admin_token')
    const savedExp = localStorage.getItem('token_exp')
    if (savedToken && savedExp && Date.now() < Number(savedExp)) {
      token.value = savedToken
      username.value = localStorage.getItem('admin_username') || 'Admin'
      role.value = localStorage.getItem('admin_role') || ''
    }
  }

  async function login(account: string, password: string, verifiCode: string, uuid: string) {
    const data = await loginApi({ account, password, verifiCode, uuid })
    if (!data?.token) {
      throw new Error('登录失败：服务器返回数据异常')
    }
    token.value = data.token
    username.value = account
    role.value = data.role || ''
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('token_exp', String(Date.now() + 24 * 3600 * 1000))
    localStorage.setItem('admin_username', account)
    localStorage.setItem('admin_role', data.role || '')
    return data
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('token_exp')
    localStorage.removeItem('admin_username')
    localStorage.removeItem('admin_role')
    router.push('/login')
  }

  return { token, username, role, isLoggedIn, isAdmin, restoreSession, login, logout }
})
