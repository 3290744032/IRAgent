<template>
  <div class="stat-card">
    <div class="stat-card__header">
      <span class="stat-card__label">{{ label }}</span>
      <div :class="['stat-card__icon', `stat-card__icon--${iconClass}`]">{{ icon }}</div>
    </div>
    <div class="stat-card__value">
      {{ value }}<span v-if="suffix" class="stat-card__suffix">{{ suffix }}</span>
    </div>
    <div v-if="trend !== undefined" :class="['stat-card__trend', trendDir === 'down' ? 'stat-card__trend--down' : 'stat-card__trend--up']">
      {{ trendDir === 'down' ? '↓' : '↑' }} {{ trend }}<span v-if="trendSuffix">{{ trendSuffix }}</span>
      <span class="stat-card__trend-label" v-if="trendLabel">{{ trendLabel }}</span>
    </div>
    <div v-if="flagged !== undefined" class="stat-card__trend" style="color: var(--danger);">
      {{ flagged }} <span class="stat-card__trend-label">{{ flaggedLabel }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  icon: string
  iconClass: 'green' | 'purple' | 'blue' | 'amber'
  value: number
  suffix?: string
  trend?: number
  trendDir?: string
  trendSuffix?: string
  trendLabel?: string
  flagged?: number
  flaggedLabel?: string
}>(), {
  trendDir: 'up',
})
</script>

<style lang="less" scoped>
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--slate-100);
  transition: all 0.2s cubic-bezier(0, 0, 0.2, 1);
  cursor: default;

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.06), 0 2px 4px -1px rgba(0, 0, 0, 0.04);
    transform: translateY(-2px);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  &__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--slate-400);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;

    &--green { background: #ECFDF5; color: var(--success); }
    &--purple { background: #EEF2FF; color: var(--brand-500); }
    &--blue { background: #EFF6FF; color: var(--info); }
    &--amber { background: #FFFBEB; color: var(--warning); }
  }

  &__value {
    font-size: 32px;
    font-weight: 800;
    color: var(--slate-800);
    letter-spacing: -0.02em;
    line-height: 1;
  }

  &__suffix {
    font-size: 16px;
    color: var(--slate-400);
    font-weight: 600;
  }

  &__trend {
    font-size: 12px;
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 4px;

    &--up { color: var(--success); }
    &--down { color: var(--danger); }
  }

  &__trend-label {
    color: var(--slate-400);
    margin-left: 4px;
  }
}
</style>
