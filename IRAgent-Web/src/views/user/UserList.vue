<template>
  <div class="user-page">
    <h2 class="page-title">用户管理</h2>

    <!-- Toolbar -->
    <div class="toolbar">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索账号或邮箱..."
        allow-clear
        style="max-width: 320px"
        @search="onSearch"
        @change="onSearch"
      />
      <a-button type="primary" @click="onSearch">查询</a-button>
      <a-button @click="resetSearch">重置</a-button>
      <span class="toolbar__count">
        共 <strong>{{ total }}</strong> 条
      </span>
    </div>

    <!-- Table -->
    <a-table
      :columns="columns"
      :data-source="users"
      :loading="loading"
      :pagination="pagination"
      row-key="userId"
      @change="onTableChange"
      class="user-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'error'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openQuotaModal(record)">配额</a>
            <a-switch
              :checked="record.status === 1"
              checked-children="启用"
              un-checked-children="禁用"
              size="small"
              @change="(checked: boolean) => toggleStatus(record, checked)"
            />
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Quota Modal -->
    <a-modal
      v-model:open="quotaModalOpen"
      title="编辑用户配额"
      @ok="saveQuota"
      @cancel="quotaModalOpen = false"
      ok-text="确定"
      cancel-text="取消"
    >
      <p style="margin-bottom: 16px; font-weight: 600;">
        {{ editingUser?.account }} — {{ editingUser?.nickname }}
      </p>
      <a-slider v-model:value="quotaValue" :min="1" :max="20" :marks="quotaMarks" />
      <div style="text-align: center; font-size: 28px; font-weight: 800; color: var(--brand-500); margin-top: 12px;">
        {{ quotaValue }}
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getUsers, updateUserStatus, updateUserQuota } from '@/api/admin'
import type { UserItem } from '@/api/admin'

const keyword = ref('')
const users = ref<UserItem[]>([])
const total = ref(0)
const loading = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '账号', dataIndex: 'account', key: 'account', width: 140 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 130 },
  { title: '操作', key: 'action', width: 160 },
]

const quotaModalOpen = ref(false)
const quotaValue = ref(5)
const editingUser = ref<UserItem | null>(null)
const quotaMarks: Record<number, string> = { 1: '1', 5: '5', 10: '10', 15: '15', 20: '20' }

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers(pagination.current - 1, pagination.pageSize, keyword.value || undefined)
    users.value = res.data ?? []
    total.value = res.total ?? 0
    pagination.total = res.total ?? 0
  } catch {
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.current = 1
  fetchUsers()
}

function resetSearch() {
  keyword.value = ''
  pagination.current = 1
  fetchUsers()
}

function onTableChange(pag: { current: number; pageSize: number }) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchUsers()
}

async function toggleStatus(record: UserItem, checked: boolean) {
  try {
    await updateUserStatus(record.userId, checked ? 1 : 0)
    record.status = checked ? 1 : 0
    message.success(`用户「${record.account}」已${checked ? '启用' : '禁用'}`)
  } catch {
    message.error('操作失败')
  }
}

function openQuotaModal(record: UserItem) {
  editingUser.value = record
  quotaValue.value = 5
  quotaModalOpen.value = true
}

async function saveQuota() {
  if (!editingUser.value) return
  try {
    await updateUserQuota(`tenant-${editingUser.value.userId}`, quotaValue.value)
    message.success(`用户「${editingUser.value.account}」并发配额已更新为 ${quotaValue.value}`)
    quotaModalOpen.value = false
  } catch {
    message.error('配额更新失败')
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style lang="less" scoped>
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--slate-800);
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;

  &__count {
    font-size: 12px;
    color: var(--slate-400);
    margin-left: auto;

    strong {
      color: var(--slate-700);
    }
  }
}

.user-table {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--slate-100);
}
</style>
