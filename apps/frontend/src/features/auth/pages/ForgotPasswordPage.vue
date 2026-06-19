<script setup lang="ts">
import { ArrowLeftOutlined, CheckOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { computed, ref } from 'vue'
import { ApiError } from '../../../lib/apiError'
import ThemeToggle from '../../../components/ThemeToggle.vue'
import { requestPasswordReset } from '../api/authApi'

const email = ref('')
const isSubmitting = ref(false)
const acceptedEmail = ref('')
const errorMessage = ref('')

const isAccepted = computed(() => Boolean(acceptedEmail.value))

async function submit() {
  const emailAddress = email.value.trim()
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await requestPasswordReset({ email: emailAddress })
    acceptedEmail.value = emailAddress
  } catch (error: unknown) {
    errorMessage.value = passwordFlowErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function requestAgain() {
  acceptedEmail.value = ''
  errorMessage.value = ''
}

function passwordFlowErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 404) {
    return 'Password recovery is not available yet. Please contact your workspace admin for help signing in.'
  }

  return error instanceof Error
    ? error.message
    : 'Unable to request password recovery. Please try again.'
}
</script>

<template>
  <main class="forgot-password-page">
    <RouterLink class="back-link" to="/"><ArrowLeftOutlined /> Back to home</RouterLink>
    <ThemeToggle class="auth-theme-toggle" />

    <section class="recovery-card">
      <RouterLink class="brand" to="/"
        ><span><CheckOutlined /></span>TaskMind</RouterLink
      >

      <div class="heading">
        <small>ACCOUNT RECOVERY</small>
        <h1>Reset your password</h1>
        <p>
          Enter your account email and we’ll request the TaskMind password recovery flow for you.
        </p>
      </div>

      <a-result
        v-if="isAccepted"
        status="success"
        title="Recovery request accepted"
        :sub-title="`If ${acceptedEmail} belongs to a TaskMind account, you’ll receive the next recovery step shortly.`"
      >
        <template #extra>
          <a-button type="primary" @click="requestAgain">Request another email</a-button>
          <RouterLink to="/login"><a-button>Back to sign in</a-button></RouterLink>
        </template>
      </a-result>

      <form v-else @submit.prevent="submit">
        <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
        <a-alert
          type="info"
          show-icon
          message="Password recovery sends a generic response to protect account privacy."
        />

        <label>
          Email address
          <a-input
            v-model:value="email"
            size="large"
            type="email"
            placeholder="you@example.com"
            required
            autofocus
          >
            <template #prefix><MailOutlined /></template>
          </a-input>
        </label>

        <a-button type="primary" size="large" html-type="submit" :loading="isSubmitting" block>
          Send recovery instructions
        </a-button>
      </form>

      <p class="switch-copy">
        Remembered your password?
        <RouterLink to="/login">Sign in</RouterLink>
      </p>
    </section>

    <p class="auth-note"><LockOutlined /> Your work stays private and secure.</p>
  </main>
</template>

<style scoped>
.forgot-password-page {
  min-height: 100vh;
  padding: 40px 20px;
  display: grid;
  place-items: center;
  background: var(--tm-bg-gradient);
  color: var(--tm-text);
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
.auth-theme-toggle {
  position: absolute;
  right: 34px;
  top: 24px;
}
.recovery-card {
  width: min(470px, 100%);
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
  color: var(--tm-text);
  font-size: 28px;
  letter-spacing: -1px;
  margin: 10px 0;
}
.heading p,
.switch-copy,
.auth-note {
  color: var(--tm-text-muted);
}
.heading p {
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
  border-color: var(--tm-border);
  border-radius: 9px;
  color: var(--tm-text);
  background: var(--tm-surface-subtle);
}
.ant-btn-primary {
  height: 46px;
  border-radius: 9px;
  margin-top: 4px;
  background: linear-gradient(135deg, var(--tm-primary), var(--tm-primary-hover));
}
.switch-copy {
  text-align: center;
  font-size: 12px;
  margin: 24px 0 0;
}
.switch-copy a {
  color: var(--tm-primary);
  text-decoration: none;
  font-weight: 700;
  margin-left: 4px;
}
.auth-note {
  position: absolute;
  bottom: 25px;
  font-size: 11px;
}
:deep(.ant-result-title) {
  color: var(--tm-text);
}
:deep(.ant-result-subtitle),
:deep(.ant-input-prefix) {
  color: var(--tm-text-muted);
}
:deep(.ant-input) {
  color: var(--tm-text);
  background: transparent;
}
:deep(.ant-input::placeholder) {
  color: var(--tm-text-soft);
}
@media (max-width: 560px) {
  .forgot-password-page {
    padding-top: 85px;
    place-items: start center;
  }
  .recovery-card {
    padding: 30px 22px;
  }
  .back-link {
    left: 20px;
    top: 24px;
  }
  .auth-theme-toggle {
    right: 20px;
    top: 16px;
  }
  .auth-note {
    position: static;
    margin-top: 20px;
  }
}
</style>
