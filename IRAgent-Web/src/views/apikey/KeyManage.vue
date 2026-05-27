<template>
  <div class="key-page">
    <h2 class="page-title">API Key 管理</h2>

    <a-alert class="banner" type="info" show-icon :message="null">
      <template #description>
        默认使用 <strong>火山方舟</strong>（预设提供商）。每个 Key 更新后立即生效，无需重启。
      </template>
    </a-alert>

    <KeyCard
      title="Doubao Chat"
      usage="对话模型 — 聊天 / 流式 / 苏格拉底教学"
      :badge="presetBadge"
      :keyValue="doubaoChatKey"
      :status="doubaoChatStatus"
      :statusColor="doubaoChatStatusColor"
      :statusLabel="doubaoChatStatusLabel"
      :newKey="doubaoChatNewKey"
      :updating="doubaoChatUpdating"
      keyPlaceholder="输入新的 Doubao Chat API Key"
      :embedFallback="false"
      @update:newKey="doubaoChatNewKey = $event"
      @update-key="handleUpdate('chat')"
    />

    <KeyCard
      style="margin-top: 16px;"
      title="DeepSeek"
      usage="诊断 / 时间轴 — DAG 诊断、时间轴生成、视频流"
      :badge="presetBadge"
      :keyValue="deepseekKey"
      :status="deepseekStatus"
      :statusColor="deepseekStatusColor"
      :statusLabel="deepseekStatusLabel"
      :newKey="deepseekNewKey"
      :updating="deepseekUpdating"
      keyPlaceholder="输入新的 DeepSeek API Key"
      :embedFallback="false"
      @update:newKey="deepseekNewKey = $event"
      @update-key="handleUpdate('deepseek')"
    />

    <KeyCard
      style="margin-top: 16px;"
      title="Doubao Embedding"
      usage="向量模型 — 语义搜索、向量化存储"
      :badge="presetBadge"
      :keyValue="embedKey"
      :status="embedStatus"
      :statusColor="embedStatusColor"
      :statusLabel="embedStatusLabel"
      :newKey="embedNewKey"
      :updating="embedUpdating"
      keyPlaceholder="输入新的 Embedding API Key（留空则 fallback 到 Doubao Chat Key）"
      :embedFallback="embedFallback"
      @update:newKey="embedNewKey = $event"
      @update-key="handleUpdate('embedding')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import KeyCard from '@/components/KeyCard.vue'
import { getApiKey, updateApiKey } from '@/api/admin'

const presetBadge = { label: '预设', color: '#1677ff', bg: '#e8f4fd' }

type ModelType = 'chat' | 'deepseek' | 'embedding'

function modelState() {
  return { key: ref(''), status: ref('未设置'), newKey: ref(''), updating: ref(false) }
}
const doubaoChat = modelState()
const deepseek = modelState()
const embed = { ...modelState(), fallback: ref(false) }

const doubaoChatStatusColor = computed(() => doubaoChat.status.value === '正常' ? 'success' : 'error')
const doubaoChatStatusLabel = computed(() => (doubaoChat.status.value === '正常' ? '🟢 ' : '🔴 ') + doubaoChat.status.value)
const deepseekStatusColor = computed(() => deepseek.status.value === '正常' ? 'success' : 'error')
const deepseekStatusLabel = computed(() => (deepseek.status.value === '正常' ? '🟢 ' : '🔴 ') + deepseek.status.value)
const embedStatusColor = computed(() => {
  if (embed.status.value === '正常') return 'success'
  if (embed.status.value === '使用 Chat Key') return 'warning'
  return 'error'
})
const embedStatusLabel = computed(() => {
  if (embed.status.value === '正常') return '🟢 ' + embed.status.value
  if (embed.status.value === '使用 Chat Key') return '🟡 ' + embed.status.value
  return '🔴 ' + embed.status.value
})

const doubaoChatKey = computed(() => doubaoChat.key.value)
const doubaoChatStatus = computed(() => doubaoChat.status.value)
const doubaoChatNewKey = computed({ get: () => doubaoChat.newKey.value, set: (v: string) => doubaoChat.newKey.value = v })
const doubaoChatUpdating = computed(() => doubaoChat.updating.value)

const deepseekKey = computed(() => deepseek.key.value)
const deepseekStatus = computed(() => deepseek.status.value)
const deepseekNewKey = computed({ get: () => deepseek.newKey.value, set: (v: string) => deepseek.newKey.value = v })
const deepseekUpdating = computed(() => deepseek.updating.value)

const embedKey = computed(() => embed.key.value)
const embedStatus = computed(() => embed.status.value)
const embedNewKey = computed({ get: () => embed.newKey.value, set: (v: string) => embed.newKey.value = v })
const embedUpdating = computed(() => embed.updating.value)
const embedFallback = computed(() => embed.fallback.value)

async function fetchKey() {
  try {
    const res = await getApiKey()
    if (res) {
      doubaoChat.key.value = res.doubaoChatKey || '未设置'
      doubaoChat.status.value = res.doubaoChatStatus || '未设置'
      deepseek.key.value = res.deepseekKey || '未设置'
      deepseek.status.value = res.deepseekStatus || '未设置'
      embed.key.value = res.embedKey || '未设置'
      embed.status.value = res.embedStatus || '未设置'
      embed.fallback.value = res.embedFallback ?? false
    }
  } catch {
    // keep defaults
  }
}

async function handleUpdate(type: ModelType) {
  const st = type === 'chat' ? doubaoChat : type === 'deepseek' ? deepseek : embed
  const key = st.newKey.value.trim()
  if (!key) { message.warning('请输入 API Key'); return }
  if (key.length < 10) { message.warning('API Key 格式不正确'); return }
  st.updating.value = true
  try {
    const res = await updateApiKey(key, type)
    st.key.value = res?.key ?? key.slice(0, 4) + '••••••••••••' + key.slice(-4)
    st.status.value = '正常'
    st.newKey.value = ''
    const label = type === 'chat' ? 'Doubao Chat' : type === 'deepseek' ? 'DeepSeek' : 'Embedding'
    message.success(`${label} Key 已更新，立即生效`)
    fetchKey()
  } catch {
    message.error('更新失败')
  } finally {
    st.updating.value = false
  }
}

onMounted(() => fetchKey())
</script>

<style lang="less" scoped>
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--slate-800);
  margin-bottom: 20px;
}

.provider-banner {
  max-width: 640px;
  margin-bottom: 24px;
  border-radius: 12px;

  :deep(.ant-alert-message) {
    display: none;
  }

  &__text {
    font-size: 13px;
    line-height: 1.8;
    color: var(--slate-600);
  }
}
</style>
