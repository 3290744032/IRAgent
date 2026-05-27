import request from './request'

// ---- Dashboard ----
export interface TrendItem {
  value: number
  todayChange: number
  trend: string // "up" | "down"
  trendValue: number
}

export interface DashboardStats {
  userCount: TrendItem
  aiQuestionCount: TrendItem
  flaggedCount: TrendItem
  officialCount: TrendItem
  trendPeriod: string
}

export interface CacheStatsData {
  hitRate: string // e.g. "83.4%"
  totalRequests: number
  hits: number
  misses: number
  estimatedTokenSaved: number
  avgResponseTimeMs: string | null
  hitRateTrend: string
  hitRateTrendValue: number
  tokenTrend: string
  tokenTrendValue: number
  trendPeriod: string
}

export interface HealthItem {
  status: 'up' | 'down'
  message: string
  latencyMs: number
}

export interface HealthResult {
  postgresql: HealthItem
  redis: HealthItem
  milvus: HealthItem
  rocketmq: HealthItem
}

export function getDashboardStats(): Promise<DashboardStats> {
  return request.get('/admin/dashboard/stats')
}

export function getCacheStats(): Promise<CacheStatsData> {
  return request.get('/cache/stats')
}

export function getHealth(): Promise<HealthResult> {
  return request.get('/admin/health')
}

// ---- Users ----
export interface UserItem {
  userId: number
  account: string
  email: string
  nickname: string
  status: number // 0=禁用, 1=启用
  createTime: string
}

export interface UserListResult {
  data: UserItem[]
  total: number
}

export function getUsers(page: number, size: number, keyword?: string): Promise<UserListResult> {
  return request.get('/admin/users', { params: { page, size, keyword } })
}

export function updateUserStatus(userId: number, status: number): Promise<{ success: boolean }> {
  return request.put(`/admin/users/${userId}/status`, { status })
}

export function updateUserQuota(tenantId: string, maxConcurrent: number): Promise<{ tenantId: string; maxConcurrent: number }> {
  return request.put(`/admin/tenants/${tenantId}/quota`, { maxConcurrent })
}

// ---- API Key ----
export interface ApiKeyResult {
  doubaoChatKey: string
  doubaoChatStatus: string
  deepseekKey: string
  deepseekStatus: string
  embedKey: string
  embedStatus: string
  embedFallback: boolean
}

export function getApiKey(): Promise<ApiKeyResult> {
  return request.get('/admin/api-key')
}

export function updateApiKey(apiKey: string, type: 'chat' | 'deepseek' | 'embedding' = 'chat'): Promise<{ success: boolean; key: string; type: string }> {
  return request.put('/admin/api-key', { apiKey, type })
}

// ---- Per-Model Custom Endpoint — 待后端实现 ----
// export function saveCustomEndpoint(data: { type: string; baseUrl: string; model: string }): Promise<{ success: boolean }> {
//   return request.put('/admin/custom-endpoint', data)
// }
