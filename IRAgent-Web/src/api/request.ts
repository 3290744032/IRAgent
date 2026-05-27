import axios from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 30000,
})

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers['token'] = token
  }
  return config
})

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    if (!body) return body
    // ApiResponse wrapper: { success, code, data }
    if (typeof body === 'object' && 'success' in body) {
      if (body.success === false) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data ?? body
    }
    // bare Map — pass through directly
    return body
  },
  (error) => {
    if (error.response?.status === 401) {
      const currentPath = window.location.hash.replace('#', '') || '/'
      if (currentPath !== '/login') {
        sessionStorage.setItem('redirect', currentPath)
      }
      localStorage.removeItem('admin_token')
      localStorage.removeItem('token_exp')
      window.location.hash = '#/login'
    }
    return Promise.reject(error)
  },
)

export default service
