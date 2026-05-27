<template>
  <a-layout class="admin-layout">
    <Sidebar />
    <a-layout class="main-area">
      <AppHeader />
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import AppHeader from './Header.vue'
</script>

<style lang="less" scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.main-area {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
  background: var(--slate-50);
}
</style>
