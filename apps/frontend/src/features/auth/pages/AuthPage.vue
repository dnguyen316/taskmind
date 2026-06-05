<script setup lang="ts">
import { ArrowLeftOutlined, CheckOutlined, LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons-vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ThemeToggle from '../../../components/ThemeToggle.vue'
import { saveAuthTokens } from '../../../lib/authToken'
import { login, signupEmail } from '../api/authApi'

const route = useRoute()
const router = useRouter()
const isSignup = computed(() => route.name === 'signup')
const name = ref('')
const email = ref('')
const password = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')

async function submit() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const emailAddress = email.value.trim()
    const tokens = isSignup.value
      ? await signupEmail({
          email: emailAddress,
          password: password.value,
          displayName: name.value.trim(),
        })
      : await login({
          email: emailAddress,
          password: password.value,
        })

    saveAuthTokens(tokens)
    const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirectTarget)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Authentication failed. Please try again.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <RouterLink class="back-link" to="/"><ArrowLeftOutlined /> Back to home</RouterLink>
    <ThemeToggle class="auth-theme-toggle" />
    <section class="auth-card">
      <RouterLink class="brand" to="/"><span><CheckOutlined /></span>TaskMind</RouterLink>
      <div class="heading">
        <small>{{ isSignup ? 'START FOR FREE' : 'WELCOME BACK' }}</small>
        <h1>{{ isSignup ? 'Create your account' : 'Sign in to TaskMind' }}</h1>
        <p>{{ isSignup ? 'Make space for what matters in just a few minutes.' : 'Continue building a calmer, clearer workday.' }}</p>
      </div>
      <form @submit.prevent="submit">
        <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
        <label v-if="isSignup">Full name<a-input v-model:value="name" size="large" placeholder="Alex Morgan" required><template #prefix><UserOutlined /></template></a-input></label>
        <label>Email address<a-input v-model:value="email" size="large" type="email" placeholder="you@example.com" required><template #prefix><MailOutlined /></template></a-input></label>
        <label>Password<a-input-password v-model:value="password" size="large" placeholder="••••••••" required :minlength="isSignup ? 8 : undefined"><template #prefix><LockOutlined /></template></a-input-password></label>
        <a-button type="primary" size="large" html-type="submit" :loading="isSubmitting" block>{{ isSignup ? 'Create free account' : 'Sign in' }}</a-button>
      </form>
      <p class="switch-copy">
        {{ isSignup ? 'Already have an account?' : 'New to TaskMind?' }}
        <RouterLink :to="isSignup ? '/login' : '/signup'">{{ isSignup ? 'Sign in' : 'Start free' }}</RouterLink>
      </p>
    </section>
    <p class="auth-note"><LockOutlined /> Your work stays private and secure.</p>
  </main>
</template>

<style scoped>
.auth-page{min-height:100vh;padding:40px 20px;display:grid;place-items:center;background:radial-gradient(circle at 50% 0,#eeecff 0,#f8f9fc 40%,#f5f7fa 100%);position:relative}.back-link{position:absolute;left:34px;top:30px;color:#697187;text-decoration:none;font-size:13px;font-weight:600}.auth-card{width:min(450px,100%);padding:42px;background:#fff;border:1px solid #e4e6ee;border-radius:20px;box-shadow:0 25px 60px rgba(35,35,88,.1)}.brand{width:max-content;margin:auto;display:flex;align-items:center;gap:9px;color:#17213a;text-decoration:none;font-size:19px;font-weight:800}.brand span{width:31px;height:31px;display:grid;place-items:center;color:#fff;border-radius:9px;background:linear-gradient(145deg,#6657e8,#4635c8)}.heading{text-align:center;margin:30px 0}.heading small{color:#6254d7;font-size:9px;letter-spacing:.15em;font-weight:800}.heading h1{font-size:28px;letter-spacing:-1px;margin:10px 0}.heading p{color:#7c8497;font-size:13px;line-height:1.6;margin:0}form{display:grid;gap:18px}label{display:grid;gap:7px;color:#3b4357;font-size:12px;font-weight:700}.ant-input-affix-wrapper{border-radius:9px}.ant-btn{height:46px;border-radius:9px;margin-top:4px;background:linear-gradient(135deg,#6657e8,#4d3fcf)}.switch-copy{text-align:center;color:#858c9d;font-size:12px;margin:24px 0 0}.switch-copy a{color:#5c4ed3;text-decoration:none;font-weight:700;margin-left:4px}.auth-note{position:absolute;bottom:25px;color:#939aaa;font-size:11px}@media(max-width:560px){.auth-page{padding-top:85px;place-items:start center}.auth-card{padding:30px 22px}.back-link{left:20px;top:24px}.auth-note{position:static;margin-top:20px}}
.auth-theme-toggle{position:absolute;right:34px;top:24px}.auth-page{color:var(--tm-text);background:var(--tm-bg-gradient)}.back-link{color:var(--tm-text-muted)}.auth-card{border-color:var(--tm-border);background:var(--tm-surface);box-shadow:var(--tm-shadow-lg)}.brand,.heading h1{color:var(--tm-text)}.heading small,.switch-copy a{color:var(--tm-primary)}.heading p,.switch-copy,.auth-note{color:var(--tm-text-muted)}label{color:var(--tm-text)}.ant-input-affix-wrapper{border-color:var(--tm-border);color:var(--tm-text);background:var(--tm-surface-subtle)}.ant-btn{background:linear-gradient(135deg,var(--tm-primary),var(--tm-primary-hover))}@media(max-width:560px){.auth-theme-toggle{right:20px;top:16px}}

:deep(.ant-input-affix-wrapper .ant-input),:deep(.ant-input-affix-wrapper .ant-input-password){color:var(--tm-text);background:transparent}:deep(.ant-input::placeholder){color:var(--tm-text-soft)}:deep(.ant-input-prefix),:deep(.ant-input-password-icon){color:var(--tm-text-muted)}

</style>
