<script setup lang="ts">
import {
  ArrowLeftOutlined,
  CheckOutlined,
  LockOutlined,
  MailOutlined,
  SafetyOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import ThemeToggle from '../../../components/ThemeToggle.vue'
import { useAuthStore } from '../../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isSubmitting, errorMessage } = storeToRefs(authStore)

const isSignup = computed(() => route.name === 'signup')
const name = ref('')
const email = ref('')
const password = ref('')
const otp = ref('')
const isVerifyingSignup = ref(false)
const successMessage = ref('')

const headingEyebrow = computed(() => {
  if (isVerifyingSignup.value) {
    return 'CHECK YOUR EMAIL'
  }

  return isSignup.value ? 'START FOR FREE' : 'WELCOME BACK'
})

const headingTitle = computed(() => {
  if (isVerifyingSignup.value) {
    return 'Verify your email'
  }

  return isSignup.value ? 'Create your account' : 'Sign in to TaskMind'
})

const headingCopy = computed(() => {
  if (isVerifyingSignup.value) {
    return `Enter the one-time code sent to ${email.value.trim() || 'your email address'}.`
  }

  return isSignup.value
    ? 'Make space for what matters in just a few minutes.'
    : 'Continue building a calmer, clearer workday.'
})

async function submit() {
  if (isVerifyingSignup.value) {
    await verifySignup()
    return
  }

  const emailAddress = email.value.trim()

  try {
    if (isSignup.value) {
      await authStore.requestSignup({
        email: emailAddress,
        password: password.value,
        displayName: name.value.trim(),
      })
      isVerifyingSignup.value = true
      successMessage.value = 'Signup started. Enter your verification code to activate the account.'
      return
    }

    await authStore.loginWithPassword({
      email: emailAddress,
      password: password.value,
    })

    await router.push(redirectTarget())
  } catch {
    // The auth store owns user-facing authentication errors.
  }
}

async function verifySignup() {
  try {
    await authStore.verifySignup({
      email: email.value.trim(),
      otp: otp.value.trim(),
    })
    await router.push(redirectTarget())
  } catch {
    // The auth store owns user-facing authentication errors.
  }
}

function editSignupDetails() {
  isVerifyingSignup.value = false
  otp.value = ''
  successMessage.value = ''
}

function redirectTarget() {
  return typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
}

watch(
  () => route.name,
  () => {
    isVerifyingSignup.value = false
    successMessage.value = ''
    otp.value = ''
    authStore.errorMessage = ''
  },
)
</script>

<template>
  <main class="auth-page">
    <RouterLink class="back-link" to="/"><ArrowLeftOutlined /> Back to home</RouterLink>
    <ThemeToggle class="auth-theme-toggle" />
    <section class="auth-card">
      <RouterLink class="brand" to="/"
        ><span><CheckOutlined /></span>TaskMind</RouterLink
      >
      <div class="heading">
        <small>{{ headingEyebrow }}</small>
        <h1>{{ headingTitle }}</h1>
        <p>{{ headingCopy }}</p>
      </div>
      <form @submit.prevent="submit">
        <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
        <a-alert v-if="successMessage" type="success" show-icon :message="successMessage" />

        <template v-if="isVerifyingSignup">
          <label>
            Verification code
            <a-input
              v-model:value="otp"
              size="large"
              inputmode="numeric"
              placeholder="123456"
              required
              autofocus
            >
              <template #prefix><SafetyOutlined /></template>
            </a-input>
          </label>
          <a-button type="primary" size="large" html-type="submit" :loading="isSubmitting" block
            >Verify and continue</a-button
          >
          <a-button
            type="link"
            html-type="button"
            class="secondary-action"
            @click="editSignupDetails"
            >Edit signup details</a-button
          >
        </template>

        <template v-else>
          <label v-if="isSignup">
            Full name
            <a-input v-model:value="name" size="large" placeholder="Alex Morgan" required>
              <template #prefix><UserOutlined /></template>
            </a-input>
          </label>
          <label>
            Email address
            <a-input
              v-model:value="email"
              size="large"
              type="email"
              placeholder="you@example.com"
              required
            >
              <template #prefix><MailOutlined /></template>
            </a-input>
          </label>
          <label>
            Password
            <a-input-password
              v-model:value="password"
              size="large"
              placeholder="••••••••"
              required
              :minlength="isSignup ? 8 : undefined"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </label>
          <a-button type="primary" size="large" html-type="submit" :loading="isSubmitting" block>{{
            isSignup ? 'Create free account' : 'Sign in'
          }}</a-button>
        </template>
      </form>
      <div class="auth-links" v-if="!isVerifyingSignup">
        <p class="switch-copy">
          {{ isSignup ? 'Already have an account?' : 'New to TaskMind?' }}
          <RouterLink :to="isSignup ? '/login' : '/signup'">{{
            isSignup ? 'Sign in' : 'Start free'
          }}</RouterLink>
        </p>
        <RouterLink v-if="!isSignup" class="forgot-link" to="/forgot-password">
          Forgot password?
        </RouterLink>
      </div>
    </section>
    <p class="auth-note"><LockOutlined /> Your work stays private and secure.</p>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 40px 20px;
  display: grid;
  place-items: center;
  background: var(--tm-bg-gradient);
  position: relative;
}
.back-link {
  position: absolute;
  left: 34px;
  top: 30px;
  color: var(--tm-text-muted);
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
}
.auth-card {
  width: min(450px, 100%);
  padding: 42px;
  background: var(--tm-card-bg);
  border: 1px solid var(--tm-border);
  border-radius: 20px;
  box-shadow: var(--tm-shadow-lg);
}
.brand {
  width: max-content;
  margin: auto;
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--tm-text);
  text-decoration: none;
  font-size: 19px;
  font-weight: 800;
}
.brand span {
  width: 31px;
  height: 31px;
  display: grid;
  place-items: center;
  color: var(--tm-accent-contrast);
  border-radius: 9px;
  background: linear-gradient(145deg, var(--tm-primary), var(--tm-primary-hover));
}
.heading {
  text-align: center;
  margin: 30px 0;
}
.heading small {
  color: var(--tm-primary);
  font-size: 9px;
  letter-spacing: 0.15em;
  font-weight: 800;
}
.heading h1 {
  font-size: 28px;
  letter-spacing: -1px;
  margin: 10px 0;
}
.heading p {
  color: var(--tm-text-muted);
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}
form {
  display: grid;
  gap: 18px;
}
label {
  display: grid;
  gap: 7px;
  color: var(--tm-text);
  font-size: 12px;
  font-weight: 700;
}
.ant-input-affix-wrapper {
  border-radius: 9px;
}
.ant-btn {
  height: 46px;
  border-radius: 9px;
  margin-top: 4px;
  background: linear-gradient(135deg, var(--tm-primary), var(--tm-primary-hover));
}
.secondary-action {
  background: transparent !important;
}
.auth-links {
  display: grid;
  gap: 10px;
  justify-items: center;
  margin-top: 24px;
}
.switch-copy {
  text-align: center;
  color: var(--tm-text-muted);
  font-size: 12px;
  margin: 0;
}
.forgot-link,
.switch-copy a {
  color: var(--tm-primary);
  text-decoration: none;
  font-weight: 700;
  margin-left: 4px;
}
.auth-note {
  position: absolute;
  bottom: 25px;
  color: var(--tm-text-soft);
  font-size: 11px;
}
@media (max-width: 560px) {
  .auth-page {
    padding-top: 85px;
    place-items: start center;
  }
  .auth-card {
    padding: 30px 22px;
  }
  .back-link {
    left: 20px;
    top: 24px;
  }
  .auth-note {
    position: static;
    margin-top: 20px;
  }
}
.auth-theme-toggle {
  position: absolute;
  right: 34px;
  top: 24px;
}
.auth-page {
  color: var(--tm-text);
  background: var(--tm-bg-gradient);
}
.back-link {
  color: var(--tm-text-muted);
}
.auth-card {
  border-color: var(--tm-border);
  background: var(--tm-card-bg);
  box-shadow: var(--tm-shadow-lg);
}
.brand,
.heading h1 {
  color: var(--tm-text);
}
.heading small,
.forgot-link,
.switch-copy a {
  color: var(--tm-primary);
}
.heading p,
.switch-copy,
.auth-note {
  color: var(--tm-text-muted);
}
label {
  color: var(--tm-text);
}
.ant-input-affix-wrapper {
  border-color: var(--tm-border);
  color: var(--tm-text);
  background: var(--tm-surface-subtle);
}
.ant-btn {
  background: linear-gradient(135deg, var(--tm-primary), var(--tm-primary-hover));
}
@media (max-width: 560px) {
  .auth-theme-toggle {
    right: 20px;
    top: 16px;
  }
}

:deep(.ant-input-affix-wrapper .ant-input),
:deep(.ant-input-affix-wrapper .ant-input-password) {
  color: var(--tm-text);
  background: transparent;
}
:deep(.ant-input::placeholder) {
  color: var(--tm-text-soft);
}
:deep(.ant-input-prefix),
:deep(.ant-input-password-icon) {
  color: var(--tm-text-muted);
}
</style>
