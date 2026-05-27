<template>
  <div class="review-card" :class="{ 'review-card--removing': removing }">
    <div class="review-card__header">
      <span class="review-card__topic">📐 {{ question.topic || '未分类' }}</span>
      <span class="review-card__source">{{ question.source === 'ai-generated' ? 'AI 生成' : question.source }}</span>
    </div>

    <div class="review-card__question">{{ question.questionText }}</div>

    <div class="review-card__answer">
      <strong>AI 答案：</strong>{{ question.correctAnswer }}
    </div>

    <div v-if="!removing" class="review-card__actions">
      <a-button size="small" type="primary" @click="$emit('approve')">✓ 通过</a-button>
      <a-button size="small" danger @click="$emit('reject')">✗ 驳回</a-button>
    </div>

    <div v-else class="review-card__actions">
      <span class="review-card__result" :style="{ color: resultColor }">{{ resultLabel }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FlaggedQuestion } from '@/api/question'

defineProps<{
  question: FlaggedQuestion
}>()

defineEmits<{
  approve: []
  reject: []
}>()

const removing = ref(false)
const resultColor = ref('')
const resultLabel = ref('')

defineExpose({ removing, resultColor, resultLabel })
</script>

<style lang="less" scoped>
.review-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--slate-100);
  transition: all 0.3s cubic-bezier(0, 0, 0.2, 1);

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.06), 0 2px 4px -1px rgba(0, 0, 0, 0.04);
  }

  &--removing {
    opacity: 0.5;
    transform: scale(0.98);
    pointer-events: none;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  &__topic {
    font-size: 12px;
    font-weight: 700;
    color: var(--brand-500);
  }

  &__source {
    font-size: 11px;
    color: var(--slate-400);
  }

  &__question {
    font-size: 14px;
    color: var(--slate-700);
    line-height: 1.6;
    margin-bottom: 8px;
    white-space: pre-wrap;
  }

  &__answer {
    font-size: 13px;
    color: var(--slate-600);
    margin-bottom: 14px;
    padding: 10px 14px;
    background: var(--slate-50);
    border-radius: 6px;
    border-left: 3px solid var(--brand-300);

    strong { color: var(--slate-700); }
  }

  &__actions {
    display: flex;
    gap: 8px;
  }

  &__result {
    font-size: 12px;
    font-weight: 600;
  }
}
</style>
