<template>
  <a-layout-sider
    :width="240"
    :collapsed-width="64"
    collapsible
    breakpoint="lg"
    class="sidebar"
    theme="dark"
    trigger=""
  >
    <!-- Logo -->
    <div class="sidebar__logo">
      <div class="sidebar__logo-icon">📚</div>
      <span class="sidebar__logo-text">IRAgent Pro</span>
      <span class="sidebar__logo-ver">v2.0</span>
    </div>

    <!-- Navigation -->
    <a-menu
      v-model:selectedKeys="selectedKeys"
      mode="inline"
      theme="dark"
      class="sidebar__menu"
      @click="onMenuClick"
    >
      <a-menu-item key="dashboard">
        <template #icon><DashboardOutlined /></template>
        <span>系统概览</span>
      </a-menu-item>
      <a-menu-item key="users">
        <template #icon><UserOutlined /></template>
        <span>用户管理</span>
      </a-menu-item>
      <a-menu-item key="question-review">
        <template #icon><SafetyOutlined /></template>
        <span>题目审核</span>
        <a-badge v-if="appStore.flaggedCount > 0" :count="appStore.flaggedCount" :overflow-count="99" class="sidebar__badge" />
      </a-menu-item>
      <a-menu-item key="api-key">
        <template #icon><KeyOutlined /></template>
        <span>API Key</span>
      </a-menu-item>
    </a-menu>

    <!-- Footer -->
    <div class="sidebar__footer">
      <div class="sidebar__footer-avatar">A</div>
      <div class="sidebar__footer-info">
        <div class="sidebar__footer-name">Admin</div>
        <div class="sidebar__footer-role">系统管理员</div>
      </div>
    </div>
  </a-layout-sider>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { DashboardOutlined, UserOutlined, SafetyOutlined, KeyOutlined } from '@ant-design/icons-vue'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const selectedKeys = ref<string[]>([String(route.name).toLowerCase().replace(/([A-Z])/g, '-$1').toLowerCase()])

watch(() => route.name, (name) => {
  if (name) {
    const key = String(name).replace(/([A-Z])/g, '-$1').toLowerCase().replace(/^-/, '')
    selectedKeys.value = [key]
    const titles: Record<string, string> = {
      'dashboard': '系统概览',
      'users': '用户管理',
      'question-review': '题目审核',
      'api-key': 'API Key 管理',
    }
    appStore.setBreadcrumb(titles[key] || '')
  }
})

function onMenuClick({ key }: { key: string }) {
  router.push(`/${key}`)
}
</script>

<style lang="less" scoped>
.sidebar {
  background: var(--slate-900) !important;

  :deep(.ant-layout-sider-children) {
    display: flex;
    flex-direction: column;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 20px 20px 12px;
    color: #fff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  &__logo-icon {
    width: 36px;
    height: 36px;
    border-radius: 6px;
    background: linear-gradient(135deg, var(--brand-500), var(--brand-400));
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }

  &__logo-text {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }

  &__logo-ver {
    font-size: 10px;
    opacity: 0.4;
    margin-left: auto;
  }

  &__menu {
    flex: 1;
    padding: 8px 0;
    background: transparent !important;
    border-inline-end: none !important;

    :deep(.ant-menu-item) {
      margin: 2px 8px;
      border-radius: 6px;
      width: auto;
      color: var(--slate-300);
      transition: all 0.15s cubic-bezier(0, 0, 0.2, 1);

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.05) !important;
      }
    }

    :deep(.ant-menu-item-selected) {
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.25), rgba(99, 102, 241, 0.1)) !important;
      color: #fff;

      &::after {
        display: none;
      }

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 6px;
        bottom: 6px;
        width: 3px;
        background: var(--brand-400);
        border-radius: 0 2px 2px 0;
      }
    }
  }

  &__badge {
    margin-left: auto;
  }

  &__footer {
    padding: 12px 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--slate-400);
    font-size: 12px;
  }

  &__footer-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--brand-500), var(--brand-400));
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 13px;
    font-weight: 700;
  }

  &__footer-name {
    color: #fff;
    font-size: 12px;
    font-weight: 600;
  }

  &__footer-role {
    font-size: 10px;
    opacity: 0.5;
  }
}
</style>
