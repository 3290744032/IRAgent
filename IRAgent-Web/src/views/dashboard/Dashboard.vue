<template>
  <div class="dashboard">
    <h2 class="page-title">系统概览</h2>

    <!-- Stat Cards -->
    <a-row :gutter="16" class="stat-grid">
      <a-col :xs="24" :sm="12" :lg="6">
        <StatCard
          label="缓存命中率"
          icon="⚡"
          icon-class="green"
          :value="cacheStats.hitRateNum"
          suffix="%"
          :trend="cacheStats.hitRateTrendValue"
          :trend-dir="cacheStats.hitRateTrend"
          trend-label="较昨日"
        />
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <StatCard
          label="Token 节省"
          icon="💎"
          icon-class="purple"
          :value="cacheStats.tokenSavedK"
          suffix="K"
          :trend="cacheStats.tokenTrendValue"
          :trend-dir="cacheStats.tokenTrend"
          trend-label="较昨日"
        />
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <StatCard
          label="活跃用户"
          icon="👤"
          icon-class="blue"
          :value="dashStats.userCount"
          :trend="dashStats.userCountTrend"
          :trend-dir="dashStats.userCountDir"
          trend-label="较昨日"
        />
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <StatCard
          label="AI 题目"
          icon="🤖"
          icon-class="amber"
          :value="dashStats.aiQuestions"
          :flagged="dashStats.flaggedCount"
          flagged-label="待审核"
        />
      </a-col>
    </a-row>

    <!-- Charts -->
    <a-row :gutter="16" class="chart-grid">
      <a-col :xs="24" :lg="15">
        <div class="chart-card">
          <div class="chart-card__title">Token 消耗趋势（近7天）</div>
          <div ref="tokenChartRef" class="chart-box"></div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="9">
        <div class="chart-card">
          <div class="chart-card__title">服务健康状态</div>
          <div class="health-grid">
            <HealthDot
              v-for="item in healthItems"
              :key="item.name"
              :name="item.name"
              :status="item.status"
              :latency-ms="item.latencyMs"
            />
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getCacheStats, getHealth } from '@/api/admin'
import type { HealthResult } from '@/api/admin'
import { useAppStore } from '@/stores/app'
import StatCard from '@/components/StatCard.vue'
import HealthDot from '@/components/HealthDot.vue'

const appStore = useAppStore()

// Cache stats (from /cache/stats — ApiResponse wrapped)
const cacheStats = reactive({
  hitRateNum: 83.4,
  hitRateTrendValue: 2.1,
  hitRateTrend: 'up' as string,
  tokenSavedK: 81,
  tokenTrendValue: 15,
  tokenTrend: 'down' as string,
})

// Dashboard stats (from /admin/dashboard/stats — bare Map)
const dashStats = reactive({
  userCount: 12,
  userCountTrend: 3,
  userCountDir: 'up' as string,
  aiQuestions: 156,
  flaggedCount: 5,
})

interface HealthItemUI {
  name: string
  status: 'up' | 'degraded' | 'down'
  latencyMs: number
}

const healthItems = ref<HealthItemUI[]>([
  { name: 'PostgreSQL', status: 'up', latencyMs: 2 },
  { name: 'Redis', status: 'up', latencyMs: 1 },
  { name: 'Milvus', status: 'up', latencyMs: 15 },
  { name: 'RocketMQ', status: 'up', latencyMs: 12 },
])

const tokenChartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

function healthStatus(latencyMs: number): 'up' | 'degraded' | 'down' {
  if (latencyMs < 50) return 'up'
  if (latencyMs <= 200) return 'degraded'
  return 'down'
}

function initChart() {
  if (!tokenChartRef.value) return
  chartInstance = echarts.init(tokenChartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 16, bottom: 24 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisLabel: { color: '#94A3B8', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#F1F5F9' } },
      axisLabel: { color: '#94A3B8', fontSize: 11 },
    },
    series: [
      {
        name: '输入Token',
        type: 'line',
        data: [1200, 1450, 1320, 1680, 1520, 980, 1100],
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#6366F1', width: 2.5 },
        itemStyle: { color: '#6366F1' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99,102,241,.15)' },
            { offset: 1, color: 'rgba(99,102,241,0)' },
          ]),
        },
      },
      {
        name: '输出Token',
        type: 'line',
        data: [3800, 4200, 3900, 5100, 4600, 2900, 3400],
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#8B5CF6', width: 2.5 },
        itemStyle: { color: '#8B5CF6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139,92,246,.15)' },
            { offset: 1, color: 'rgba(139,92,246,0)' },
          ]),
        },
      },
    ],
  })
}

async function fetchData() {
  try {
    const [dashData, cacheData, healthData] = await Promise.all([
      getDashboardStats(),
      getCacheStats(),
      getHealth(),
    ])

    // Dashboard stats — bare Map from AdminController
    if (dashData) {
      dashStats.userCount = dashData.userCount?.value ?? dashStats.userCount
      dashStats.userCountTrend = dashData.userCount?.trendValue ?? 0
      dashStats.userCountDir = dashData.userCount?.trend ?? 'up'
      dashStats.aiQuestions = dashData.aiQuestionCount?.value ?? dashStats.aiQuestions
      dashStats.flaggedCount = dashData.flaggedCount?.value ?? dashStats.flaggedCount
      appStore.setFlaggedCount(dashStats.flaggedCount)
    }

    // Cache stats — ApiResponse wrapped, interceptor already unwrapped to CacheStatsData
    if (cacheData) {
      cacheStats.hitRateNum = parseFloat(cacheData.hitRate) || 0
      cacheStats.hitRateTrendValue = cacheData.hitRateTrendValue ?? 0
      cacheStats.hitRateTrend = cacheData.hitRateTrend ?? 'up'
      cacheStats.tokenSavedK = Math.round((cacheData.estimatedTokenSaved ?? 0) / 1000)
      cacheStats.tokenTrendValue = cacheData.tokenTrendValue ?? 0
      cacheStats.tokenTrend = cacheData.tokenTrend ?? 'up'
    }

    // Health — bare Map from AdminController
    if (healthData) {
      const h = healthData as HealthResult
      healthItems.value = [
        { name: 'PostgreSQL', status: healthStatus(h.postgresql?.latencyMs ?? 0), latencyMs: h.postgresql?.latencyMs ?? 0 },
        { name: 'Redis', status: healthStatus(h.redis?.latencyMs ?? 0), latencyMs: h.redis?.latencyMs ?? 0 },
        { name: 'Milvus', status: healthStatus(h.milvus?.latencyMs ?? 0), latencyMs: h.milvus?.latencyMs ?? 0 },
        { name: 'RocketMQ', status: healthStatus(h.rocketmq?.latencyMs ?? 0), latencyMs: h.rocketmq?.latencyMs ?? 0 },
      ]
    }
  } catch {
    // keep mock defaults on fetch failure
  }
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await nextTick()
  initChart()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style lang="less" scoped>
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--slate-800);
  margin-bottom: 20px;
}

.stat-grid {
  margin-bottom: 24px;
}

.chart-grid {
  margin-bottom: 24px;
}

.chart-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--slate-100);

  &__title {
    font-size: 14px;
    font-weight: 700;
    color: var(--slate-700);
    margin-bottom: 16px;
  }
}

.chart-box {
  width: 100%;
  min-height: 240px;
}

.health-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
