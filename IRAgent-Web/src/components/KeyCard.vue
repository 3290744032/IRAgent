<template>
  <div class="key-card">
    <div class="key-card__header">
      <div>
        <span class="key-card__badge" :style="{ color: badge.color, background: badge.bg }">{{ badge.label }}</span>
        <span class="key-card__title">{{ title }}</span>
      </div>
      <div class="key-card__usage">{{ usage }}</div>
    </div>

    <div class="key-display">
      <span>{{ keyValue }}</span>
      <a-tag :color="statusColor" class="key-display__tag">{{ statusLabel }}</a-tag>
    </div>

    <a-form layout="vertical">
      <a-form-item :label="'更新 ' + title + ' Key'">
        <div style="display: flex; gap: 12px; align-items: flex-start;">
          <a-input-password
            :value="newKey"
            @update:value="$emit('update:newKey', $event)"
            :placeholder="keyPlaceholder"
            size="large"
            style="flex: 1;"
          />
          <a-button
            type="primary"
            size="large"
            :loading="updating"
            @click="$emit('update-key')"
          >
            更新
          </a-button>
        </div>
      </a-form-item>
    </a-form>

    <div v-if="embedFallback" class="key-card__fallback-hint">
      <a-tag color="warning">Fallback: 使用 Doubao Chat Key</a-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  usage: string
  badge: { label: string; color: string; bg: string }
  keyValue: string
  status: string
  statusColor: string
  statusLabel: string
  newKey: string
  updating: boolean
  keyPlaceholder: string
  embedFallback: boolean
}>()

defineEmits<{
  'update:newKey': [value: string]
  'update-key': []
}>()
</script>

<style lang="less" scoped>
.key-card {
  background: #fff;
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--slate-100);
  max-width: 640px;

  &__header {
    margin-bottom: 12px;
  }

  &__badge {
    display: inline-block;
    font-size: 11px;
    font-weight: 600;
    padding: 2px 10px;
    border-radius: 20px;
    line-height: 20px;
    margin-right: 8px;
  }

  &__title {
    font-size: 15px;
    font-weight: 700;
    color: var(--slate-800);
  }

  &__usage {
    font-size: 12px;
    color: var(--slate-400);
    margin-top: 4px;
  }

  &__fallback-hint {
    margin-bottom: 4px;
  }
}

.key-display {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: var(--slate-50);
  border-radius: 10px;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 18px;
  color: var(--slate-700);
  margin-bottom: 16px;
  letter-spacing: 0.03em;

  &__tag {
    margin-left: auto;
  }
}
</style>
