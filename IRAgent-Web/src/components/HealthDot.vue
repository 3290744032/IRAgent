<template>
  <div class="health-item">
    <span :class="['health-dot', `health-dot--${status}`]" />
    <span class="health-item__name">{{ name }}</span>
    <span :class="['health-item__status', `health-item__status--${status}`]">
      {{ statusLabel }} ({{ latencyMs }}ms)
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  name: string
  status: 'up' | 'degraded' | 'down'
  latencyMs: number
}>()

const statusLabel = computed(() => {
  switch (props.status) {
    case 'up': return '正常'
    case 'degraded': return '延迟'
    case 'down': return '异常'
  }
})
</script>

<style lang="less" scoped>
.health-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--slate-50);
  border-radius: 10px;

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: var(--slate-700);
  }

  &__status {
    font-size: 11px;
    font-weight: 500;
    margin-left: auto;

    &--up { color: var(--success); }
    &--degraded { color: var(--warning); }
    &--down { color: var(--danger); }
  }
}

.health-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;

  &--up {
    background: var(--success);
    box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
  }
  &--degraded {
    background: var(--warning);
    box-shadow: 0 0 6px rgba(245, 158, 11, 0.4);
  }
  &--down {
    background: var(--danger);
    box-shadow: 0 0 6px rgba(239, 68, 68, 0.4);
  }
}
</style>
