<template>
  <div class="review-page">
    <div class="review-header">
      <h2 class="page-title">题目审核</h2>
      <span class="review-count">共 <strong>{{ questions.length }}</strong> 条待审核</span>
    </div>

    <a-spin :spinning="loading">
      <div v-if="!loading && questions.length === 0" class="empty-state">
        <div class="empty-state__icon">🎉</div>
        <div class="empty-state__title">暂无待审核题目</div>
        <div class="empty-state__desc">所有题目已处理完毕，干得漂亮！</div>
      </div>

      <transition-group name="review-list" tag="div" class="review-grid">
        <QuestionCard
          v-for="q in questions"
          :key="q.id"
          :question="q"
          @approve="handleReview(q.id, 'approve')"
          @reject="handleReview(q.id, 'reject')"
        />
      </transition-group>
    </a-spin>

    <div v-if="total > questions.length" class="pagination-row">
      <a-pagination
        v-model:current="page"
        v-model:page-size="size"
        :total="total"
        show-size-changer
        @change="fetchQuestions"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getFlaggedQuestions, reviewQuestion } from '@/api/question'
import type { FlaggedQuestion } from '@/api/question'
import { useAppStore } from '@/stores/app'
import QuestionCard from '@/components/QuestionCard.vue'

const appStore = useAppStore()
const questions = ref<FlaggedQuestion[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function fetchQuestions() {
  loading.value = true
  try {
    const res = await getFlaggedQuestions(page.value - 1, size.value)
    questions.value = res.data ?? []
    total.value = res.total ?? 0
    appStore.setFlaggedCount(res.total ?? 0)
  } catch {
    message.error('获取待审核题目失败')
  } finally {
    loading.value = false
  }
}

async function handleReview(id: string, action: 'approve' | 'reject') {
  try {
    const res = await reviewQuestion(id, action)
    questions.value = questions.value.filter((q) => q.id !== id)
    appStore.setFlaggedCount(questions.value.length)
    const label = action === 'approve' ? '已通过' : '已驳回'
    message.success(`题目${label}（状态：${res.status}）`)
  } catch {
    message.error('操作失败')
  }
}

onMounted(() => {
  fetchQuestions()
})
</script>

<style lang="less" scoped>
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--slate-800);
  margin: 0;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.review-count {
  font-size: 13px;
  color: var(--slate-400);

  strong {
    color: var(--slate-700);
  }
}

.review-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-list-enter-active {
  transition: all 0.3s cubic-bezier(0, 0, 0.2, 1);
}

.review-list-leave-active {
  transition: all 0.3s cubic-bezier(0, 0, 0.2, 1);
}

.review-list-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.review-list-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--slate-400);

  &__icon { font-size: 48px; margin-bottom: 12px; }
  &__title { font-size: 15px; font-weight: 600; color: var(--slate-500); margin-bottom: 4px; }
  &__desc { font-size: 12px; }
}

.pagination-row {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
