<template>
  <div class="login-page">
    <!-- Brand -->
    <div class="login-brand">
      <div class="login-brand__logo">📚</div>
      <div class="login-brand__title">IRAgent Pro</div>
      <div class="login-brand__sub">AI 备考平台 · 管理端</div>
      <div class="login-brand__desc">
        以个人知识库为中心的 AI 学习系统<br />
        管理后台 — 系统监控 · 用户管理 · 内容审核
      </div>
    </div>

    <!-- Form -->
    <div class="login-form-area">
      <div class="login-form-area__header">
        <div class="login-form-area__title">管理员登录</div>
        <div class="login-form-area__sub">请使用管理员账号登录系统</div>
      </div>

      <a-form :model="form" layout="vertical" @finish="handleLogin" autocomplete="off">
        <a-form-item label="账号" name="account" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="form.account" placeholder="admin" size="large" />
        </a-form-item>

        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" placeholder="••••••••" size="large" />
        </a-form-item>

        <div class="captcha-row">
          <a-form-item label="验证码" name="verifiCode" :rules="[{ required: true, message: '请输入验证码' }]" style="flex:1">
            <a-input v-model:value="form.verifiCode" placeholder="验证码" size="large" />
          </a-form-item>
          <div class="captcha-img" @click="refreshCaptcha">
            <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" class="captcha-img__img" />
            <span v-else>加载中…</span>
          </div>
        </div>

        <div v-if="errorMsg" class="login-error">{{ errorMsg }}</div>

        <a-button
          type="primary"
          html-type="submit"
          size="large"
          block
          :loading="loading"
          class="login-btn"
        >
          登 录
        </a-button>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  account: '',
  password: '',
  verifiCode: '',
})
const captchaUuid = ref('')       // from X-Verification-UUID header
const loading = ref(false)
const errorMsg = ref('')
const captchaUrl = ref('')

async function refreshCaptcha() {
  captchaUrl.value = ''
  captchaUuid.value = ''
  try {
    const baseURL = import.meta.env.VITE_API_BASE
    const resp = await axios.get(`${baseURL}/auth/getVerifiCodeImage`, {
      responseType: 'blob',
      params: { t: Date.now() },
    })
    // Capture UUID from response header
    const uuid = resp.headers['x-verification-uuid'] || resp.headers['X-Verification-UUID']
    if (uuid) {
      captchaUuid.value = uuid
    }
    // Display as blob URL
    const blob = new Blob([resp.data], { type: 'image/jpeg' })
    captchaUrl.value = URL.createObjectURL(blob)
  } catch {
    errorMsg.value = '验证码加载失败'
  }
}

async function handleLogin() {
  errorMsg.value = ''
  if (!captchaUuid.value) {
    errorMsg.value = '验证码已过期，请刷新后重试'
    refreshCaptcha()
    return
  }
  loading.value = true
  try {
    await userStore.login(form.account, form.password, form.verifiCode, captchaUuid.value)
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '账号或密码错误，请重试'
    errorMsg.value = msg
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

refreshCaptcha()
</script>

<style lang="less" scoped>
.login-page {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, var(--slate-50) 0%, #EEF2FF 50%, var(--slate-50) 100%);
}

.login-brand {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px;
  background: linear-gradient(160deg, var(--slate-900) 0%, var(--brand-700) 100%);
  color: #fff;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    opacity: 0.06;
    background:
      radial-gradient(circle at 30% 40%, #fff 0%, transparent 60%),
      radial-gradient(circle at 70% 60%, #818CF8 0%, transparent 50%);
  }

  > * {
    position: relative;
    z-index: 1;
  }

  &__logo {
    width: 80px;
    height: 80px;
    border-radius: 20px;
    background: linear-gradient(135deg, #fff, rgba(255, 255, 255, 0.85));
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40px;
    margin-bottom: 28px;
    box-shadow: 0 0 40px rgba(99, 102, 241, 0.4);
  }

  &__title {
    font-size: 32px;
    font-weight: 800;
    letter-spacing: -0.02em;
    margin-bottom: 8px;
  }

  &__sub {
    font-size: 15px;
    opacity: 0.7;
    font-weight: 400;
    letter-spacing: 0.02em;
  }

  &__desc {
    font-size: 13px;
    opacity: 0.45;
    margin-top: 32px;
    text-align: center;
    line-height: 1.7;
    max-width: 320px;
  }
}

.login-form-area {
  width: 480px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 64px;

  &__header {
    margin-bottom: 36px;
  }

  &__title {
    font-size: 26px;
    font-weight: 700;
    color: var(--slate-800);
  }

  &__sub {
    font-size: 13px;
    color: var(--slate-400);
    margin-top: 4px;
  }
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.captcha-img {
  height: 40px;
  width: 110px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 30px;
  background: linear-gradient(135deg, #E0E7FF, #C7D2FE);
  display: flex;
  align-items: center;
  justify-content: center;

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.login-error {
  font-size: 12px;
  color: var(--danger);
  margin-bottom: 8px;
}

.login-btn {
  margin-top: 8px;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, var(--brand-500), var(--brand-600)) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);

  &:hover {
    box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4) !important;
    transform: translateY(-1px);
  }
}

@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }
  .login-brand {
    padding: 40px 24px;
  }
  .login-form-area {
    width: 100%;
    padding: 32px 24px;
  }
}
</style>
