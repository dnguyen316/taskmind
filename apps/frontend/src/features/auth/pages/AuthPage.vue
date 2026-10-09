<script setup lang="ts">
import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  LockOutlined,
  MailOutlined,
  SafetyOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
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
const isPasswordVisible = ref(false)
const otp = ref('')
const isVerifyingSignup = ref(false)
const verificationInput = ref<{ focus: () => void } | null>(null)
const emailInput = ref<{ focus: () => void } | null>(null)
const errorAlert = ref<HTMLElement | null>(null)
const switchTarget = computed(() => ({
  path: isSignup.value ? '/login' : '/signup',
  query: typeof route.query.redirect === 'string' ? { redirect: route.query.redirect } : {},
}))

const headingTitle = computed(() =>
  isVerifyingSignup.value
    ? 'Check your inbox'
    : isSignup.value
      ? 'Create your account'
      : 'Welcome back',
)

async function focusError() {
  await nextTick()
  errorAlert.value?.focus()
}

async function submit() {
  if (isSubmitting.value) return
  const submittedRoute = route.name

  try {
    if (isVerifyingSignup.value) {
      await authStore.verifySignup({ email: email.value.trim(), otp: otp.value.trim() })
    } else if (isSignup.value) {
      await authStore.requestSignup({
        email: email.value.trim(),
        password: password.value,
        displayName: name.value.trim(),
      })
      if (route.name !== submittedRoute) return
      isVerifyingSignup.value = true
      password.value = ''
      isPasswordVisible.value = false
      await nextTick()
      verificationInput.value?.focus()
      return
    } else {
      await authStore.loginWithPassword({ email: email.value.trim(), password: password.value })
    }

    if (route.name === submittedRoute) {
      await router.push(redirectTarget())
    }
  } catch {
    // The auth store owns user-facing authentication errors.
    if (route.name === submittedRoute) await focusError()
  }
}

async function editSignupDetails() {
  isVerifyingSignup.value = false
  otp.value = ''
  authStore.errorMessage = ''
  await nextTick()
  emailInput.value?.focus()
}

function redirectTarget() {
  return typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
}

onMounted(() => {
  authStore.errorMessage = ''
})

watch(
  () => route.name,
  () => {
    isVerifyingSignup.value = false
    otp.value = ''
    password.value = ''
    isPasswordVisible.value = false
    authStore.errorMessage = ''
  },
)
</script>

<template>
  <main class="auth-page">
    <header class="auth-header">
      <RouterLink class="back-link" to="/"><ArrowLeftOutlined /> Back to home</RouterLink>
      <ThemeToggle />
    </header>
    <div class="auth-content">
      <RouterLink class="brand" to="/" aria-label="TaskMind home">
        <span aria-hidden="true"><CheckOutlined /></span>TaskMind
      </RouterLink>
      <section class="auth-card" aria-labelledby="auth-title">
        <ol v-if="isSignup" class="signup-steps" aria-label="Account setup progress">
          <li :aria-current="!isVerifyingSignup ? 'step' : undefined" class="step-active">
            <span aria-hidden="true"
              ><CheckOutlined v-if="isVerifyingSignup" /><template v-else>1</template></span
            >
            Your details
          </li>
          <li
            :aria-current="isVerifyingSignup ? 'step' : undefined"
            :class="{ 'step-active': isVerifyingSignup }"
          >
            <span aria-hidden="true">2</span> Verify email
          </li>
        </ol>
        <div class="heading">
          <span v-if="isVerifyingSignup" class="verification-icon" aria-hidden="true"
            ><MailOutlined
          /></span>
          <p v-else class="eyebrow">
            {{ isSignup ? 'GET STARTED WITH TASKMIND' : 'SIGN IN TO TASKMIND' }}
          </p>
          <h1 id="auth-title">{{ headingTitle }}</h1>
          <p v-if="isVerifyingSignup" class="heading-copy">
            Enter the code we sent to <strong>{{ email.trim() }}</strong> to finish setting up your
            account.
          </p>
          <p v-else class="heading-copy">
            {{
              isSignup
                ? 'One place for your tasks, ideas, and a little more clarity.'
                : 'Sign in to TaskMind and pick up where you left off.'
            }}
          </p>
        </div>
        <form @submit.prevent="submit" :aria-busy="isSubmitting">
          <div v-if="errorMessage" ref="errorAlert" tabindex="-1" class="error-alert">
            <a-alert type="error" show-icon :message="errorMessage" />
          </div>

          <template v-if="isVerifyingSignup">
            <div class="field">
              <label for="auth-otp">Verification code</label>
              <a-input
                id="auth-otp"
                ref="verificationInput"
                v-model:value="otp"
                name="otp"
                size="large"
                inputmode="numeric"
                autocomplete="one-time-code"
                placeholder="Enter your code"
                required
                :disabled="isSubmitting"
                aria-describedby="otp-help"
              >
                <template #prefix><SafetyOutlined aria-hidden="true" /></template>
              </a-input>
              <p id="otp-help" class="field-help">
                Can't find the email? Check your spam or junk folder.
              </p>
            </div>
            <a-button
              class="submit-button"
              type="primary"
              size="large"
              html-type="submit"
              :loading="isSubmitting"
              :disabled="isSubmitting"
              block
              >{{ isSubmitting ? 'Verifying…' : 'Verify and continue'
              }}<ArrowRightOutlined v-if="!isSubmitting" aria-hidden="true"
            /></a-button>
            <a-button
              class="secondary-action"
              type="link"
              html-type="button"
              :disabled="isSubmitting"
              @click="editSignupDetails"
              >Use a different email</a-button
            >
          </template>

          <template v-else>
            <div v-if="isSignup" class="field">
              <label for="auth-name">Full name</label>
              <a-input
                id="auth-name"
                v-model:value="name"
                name="name"
                autocomplete="name"
                size="large"
                placeholder="Alex Morgan"
                required
                :disabled="isSubmitting"
              >
                <template #prefix><UserOutlined aria-hidden="true" /></template>
              </a-input>
            </div>
            <div class="field">
              <label for="auth-email">Email address</label>
              <a-input
                id="auth-email"
                ref="emailInput"
                v-model:value="email"
                name="email"
                autocomplete="email"
                :autocapitalize="'none'"
                :spellcheck="false"
                size="large"
                type="email"
                placeholder="you@example.com"
                required
                :disabled="isSubmitting"
              >
                <template #prefix><MailOutlined aria-hidden="true" /></template>
              </a-input>
            </div>
            <div class="field">
              <div class="field-label-row">
                <label for="auth-password">Password</label>
                <RouterLink v-if="!isSignup" class="forgot-link" to="/forgot-password"
                  >Forgot password?</RouterLink
                >
              </div>
              <a-input
                id="auth-password"
                v-model:value="password"
                name="password"
                :type="isPasswordVisible ? 'text' : 'password'"
                :autocomplete="isSignup ? 'new-password' : 'current-password'"
                size="large"
                :placeholder="isSignup ? 'Create a password' : 'Enter your password'"
                required
                :minlength="isSignup ? 8 : undefined"
                :disabled="isSubmitting"
                :aria-describedby="isSignup ? 'password-help' : undefined"
              >
                <template #prefix><LockOutlined aria-hidden="true" /></template>
                <template #suffix>
                  <button
                    type="button"
                    class="password-toggle"
                    :aria-label="isPasswordVisible ? 'Hide password' : 'Show password'"
                    :aria-pressed="isPasswordVisible"
                    :disabled="isSubmitting"
                    @click="isPasswordVisible = !isPasswordVisible"
                  >
                    <EyeInvisibleOutlined v-if="isPasswordVisible" aria-hidden="true" />
                    <EyeOutlined v-else aria-hidden="true" />
                  </button>
                </template>
              </a-input>
              <p v-if="isSignup" id="password-help" class="field-help">
                Use at least 8 characters. You'll verify your email next.
              </p>
            </div>
            <a-button
              class="submit-button"
              type="primary"
              size="large"
              html-type="submit"
              :loading="isSubmitting"
              :disabled="isSubmitting"
              block
            >
              {{
                isSubmitting
                  ? isSignup
                    ? 'Creating account…'
                    : 'Signing in…'
                  : isSignup
                    ? 'Create account'
                    : 'Sign in'
              }}<ArrowRightOutlined v-if="!isSubmitting" aria-hidden="true" />
            </a-button>
          </template>
        </form>
        <p v-if="!isVerifyingSignup" class="switch-copy">
          {{ isSignup ? 'Already have an account?' : 'New to TaskMind?' }}
          <RouterLink :to="switchTarget">{{
            isSignup ? 'Sign in' : 'Create an account'
          }}</RouterLink>
        </p>
      </section>
      <p class="auth-note">
        <LockOutlined aria-hidden="true" />
        {{
          isSignup
            ? 'A small first step toward a more organized day.'
            : 'Your tasks, ideas, and plans. All in one place.'
        }}
      </p>
    </div>
  </main>
</template>

<style scoped>
@font-face {
  font-family: 'TaskMind Auth';
  src: url('/fonts/InterVariable.woff2') format('woff2');
  font-weight: 100 900;
  font-style: normal;
  font-display: swap;
}
.auth-page {
  --auth-accent: #6554d9;
  --auth-button-ink: #fff;
  min-height: 100vh;
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  padding: 24px 36px 32px;
  color: var(--tm-text);
  background: var(--tm-bg-gradient);
  font-family: 'TaskMind Auth', Inter, sans-serif;
}
:global(:root[data-theme='dark'] .auth-page) {
  --auth-accent: #b3a8ff;
  --auth-button-ink: #171a2c;
}
.auth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--tm-text-muted);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
}
.auth-content {
  width: min(460px, 100%);
  margin: auto;
  padding-top: 32px;
}
.brand {
  display: flex;
  width: max-content;
  align-items: center;
  gap: 10px;
  margin: 0 auto 28px;
  color: var(--tm-text);
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.6px;
  text-decoration: none;
}
.brand span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 11px;
  color: var(--auth-button-ink);
  background: var(--auth-accent);
}
.auth-card {
  padding: 36px;
  border: 1px solid var(--tm-border-soft);
  border-radius: 20px;
  background: var(--tm-card-bg);
  box-shadow: var(--tm-shadow-md);
}
.signup-steps {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 0 24px;
  margin: 0 0 28px;
  border-bottom: 1px solid var(--tm-border-soft);
  list-style: none;
}
.signup-steps li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--tm-text-muted);
  font-size: 12px;
  font-weight: 550;
}
.signup-steps li + li::before {
  content: '';
  width: 26px;
  height: 1px;
  margin-right: 8px;
  background: var(--tm-border);
}
.signup-steps li > span {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  background: var(--tm-surface-muted);
}
.signup-steps .step-active {
  color: var(--auth-accent);
}
.signup-steps .step-active > span {
  color: var(--auth-button-ink);
  background: var(--auth-accent);
}
.heading {
  margin-bottom: 28px;
}
.eyebrow {
  margin: 0 0 12px;
  color: var(--auth-accent);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}
.heading h1 {
  margin: 0 0 10px;
  color: var(--tm-text);
  font-size: 30px;
  font-weight: 650;
  letter-spacing: -1.1px;
  line-height: 1.2;
}
.heading-copy {
  margin: 0;
  color: var(--tm-text-muted);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}
.heading-copy strong {
  color: var(--tm-text);
  font-weight: 550;
}
.verification-icon {
  display: grid;
  width: 48px;
  height: 48px;
  margin-bottom: 20px;
  place-items: center;
  border-radius: 14px;
  color: var(--auth-accent);
  background: var(--tm-primary-soft);
  font-size: 22px;
}
form {
  display: grid;
  gap: 20px;
}
.field {
  display: grid;
  gap: 8px;
}
label {
  color: var(--tm-text);
  font-size: 13px;
  font-weight: 550;
}
.field-label-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}
.forgot-link,
.switch-copy a {
  color: var(--auth-accent);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}
.field-help {
  margin: 0;
  color: var(--tm-text-muted);
  font-size: 12px;
  line-height: 1.6;
}
:deep(.ant-input-affix-wrapper) {
  min-height: 48px;
  padding: 10px 14px;
  border-color: var(--tm-border);
  border-radius: 10px;
  color: var(--tm-text);
  background: var(--tm-card-bg);
  font-size: 14px;
}
:deep(.ant-input-affix-wrapper:hover),
:deep(.ant-input-affix-wrapper-focused) {
  border-color: var(--auth-accent);
}
:deep(.ant-input-affix-wrapper-focused) {
  box-shadow: 0 0 0 3px var(--tm-primary-soft);
}
:deep(.ant-input-affix-wrapper .ant-input) {
  color: var(--tm-text);
  background: transparent;
  font-family: inherit;
  font-size: 14px;
  font-weight: 400;
}
:deep(.ant-input::placeholder) {
  color: var(--tm-text-soft);
}
:deep(.ant-input-prefix) {
  margin-right: 10px;
}
:deep(.ant-input-prefix),
:deep(.ant-input-password-icon) {
  color: var(--tm-text-muted);
}
.password-toggle {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  margin: -4px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--tm-text-muted);
  cursor: pointer;
}
.password-toggle:hover {
  color: var(--auth-accent);
  background: var(--tm-primary-soft);
}
.password-toggle:disabled {
  cursor: not-allowed;
}
.submit-button {
  height: 48px;
  margin-top: 4px;
  border-radius: 10px;
  border-color: var(--auth-accent);
  color: var(--auth-button-ink);
  background: var(--auth-accent);
  box-shadow: none;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
}
.submit-button:not(:disabled):hover {
  color: var(--auth-button-ink);
  background: var(--auth-accent);
  border-color: var(--auth-accent);
  filter: brightness(0.94);
}
.submit-button:disabled {
  color: var(--auth-button-ink);
  background: var(--auth-accent);
  opacity: 0.7;
}
.secondary-action {
  color: var(--auth-accent);
  min-height: 44px;
  font-size: 13px;
}
.switch-copy {
  margin: 28px 0 0;
  padding-top: 24px;
  border-top: 1px solid var(--tm-border-soft);
  text-align: center;
  color: var(--tm-text-muted);
  font-size: 13px;
  line-height: 1.7;
}
.switch-copy a {
  margin-left: 4px;
  font-size: inherit;
}
.auth-note {
  display: flex;
  justify-content: center;
  align-items: baseline;
  gap: 7px;
  margin: 24px 0 0;
  text-align: center;
  color: var(--tm-text-muted);
  font-size: 12px;
  line-height: 1.6;
}
.auth-page :is(a, button):focus-visible,
.error-alert:focus {
  outline: 3px solid var(--auth-accent);
  outline-offset: 4px;
  border-radius: 4px;
}
.auth-page a:hover {
  color: var(--auth-accent);
}
.forgot-link:hover,
.switch-copy a:hover {
  text-decoration: underline;
}
@media (max-width: 560px) {
  .auth-page {
    padding: 16px 20px 28px;
  }
  .auth-content {
    padding-top: 28px;
  }
  .brand {
    margin-bottom: 24px;
  }
  .auth-card {
    padding: 28px 24px;
    border-radius: 16px;
  }
  .heading h1 {
    font-size: 27px;
  }
  .signup-steps {
    gap: 10px;
  }
  .signup-steps li + li::before {
    width: 12px;
    margin-right: 0;
  }
}
@media (max-width: 360px) {
  .auth-page {
    padding-inline: 12px;
  }
  .auth-card {
    padding-inline: 20px;
  }
  .signup-steps {
    gap: 8px;
  }
}
</style>
