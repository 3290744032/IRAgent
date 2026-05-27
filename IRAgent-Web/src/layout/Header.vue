<template>
  <a-layout-header class="header">
    <div class="header__breadcrumb">
      🏠 <span class="header__breadcrumb-home">首页</span>
      <span class="header__breadcrumb-sep">›</span>
      <span>{{ appStore.breadcrumbTitle }}</span>
    </div>
    <div class="header__spacer" />
    <span class="header__time">{{ currentTime }}</span>
    <a-button size="small" class="header__btn" @click="$router.push('/dashboard')">
      📊 看板
    </a-button>
    <a-button size="small" class="header__btn header__btn--danger" @click="handleLogout">
      退出登录
    </a-button>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import dayjs from 'dayjs'

const userStore = useUserStore()
const appStore = useAppStore()
const currentTime = ref('')
let timer: ReturnType<typeof setInterval>

function updateClock() {
  currentTime.value = dayjs().format('YYYY-MM-DD HH:mm')
}

function handleLogout() {
  userStore.logout()
}

onMounted(() => {
  updateClock()
  timer = setInterval(updateClock, 30000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style lang="less" scoped>
.header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--slate-100);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
  flex-shrink: 0;
  line-height: 56px;

  &__breadcrumb {
    font-size: 13px;
    color: var(--slate-400);
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__breadcrumb-home {
    color: var(--slate-400);
  }

  &__breadcrumb-sep {
    color: var(--slate-300);
    margin: 0 4px;
  }

  &__spacer {
    flex: 1;
  }

  &__time {
    font-size: 12px;
    color: var(--slate-400);
    font-family: 'JetBrains Mono', 'SF Mono', monospace;
  }

  &__btn {
    font-size: 12px;
    font-weight: 600;
    height: 30px;
    border-radius: 6px;
    border: none;
    background: var(--slate-100);
    color: var(--slate-600);

    &:hover {
      background: var(--slate-200) !important;
      color: var(--slate-600) !important;
    }

    &--danger {
      background: #fef2f2;
      color: var(--danger);

      &:hover {
        background: #fee2e2 !important;
        color: var(--danger) !important;
      }
    }
  }
}
</style>
