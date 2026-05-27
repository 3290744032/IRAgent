import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const breadcrumbTitle = ref('系统概览')
  const flaggedCount = ref(0)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setBreadcrumb(title: string) {
    breadcrumbTitle.value = title
  }

  function setFlaggedCount(count: number) {
    flaggedCount.value = count
  }

  return { sidebarCollapsed, breadcrumbTitle, flaggedCount, toggleSidebar, setBreadcrumb, setFlaggedCount }
})
