<script setup lang="ts">
import { ArrowLeftOutlined, CheckOutlined, LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons-vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isSignup = computed(() => route.name === 'signup')
const name = ref('')
const email = ref('')
const password = ref('')

function submit() {
  router.push('/dashboard')
}
</script>

<template>
  <main class="auth-page">
    <RouterLink class="back-link" to="/"><ArrowLeftOutlined /> Back to home</RouterLink>
    <section class="auth-card">
      <RouterLink class="brand" to="/"><span><CheckOutlined /></span>TaskMind</RouterLink>
      <div class="heading">
        <small>{{ isSignup ? 'START FOR FREE' : 'WELCOME BACK' }}</small>
        <h1>{{ isSignup ? 'Create your account' : 'Sign in to TaskMind' }}</h1>
        <p>{{ isSignup ? 'Make space for what matters in just a few minutes.' : 'Continue building a calmer, clearer workday.' }}</p>
      </div>
      <form @submit.prevent="submit">
        <label v-if="isSignup">Full name<a-input v-model:value="name" size="large" placeholder="Alex Morgan"><template #prefix><UserOutlined /></template></a-input></label>
        <label>Email address<a-input v-model:value="email" size="large" type="email" placeholder="you@example.com" required><template #prefix><MailOutlined /></template></a-input></label>
        <label>Password<a-input-password v-model:value="password" size="large" placeholder="••••••••" required><template #prefix><LockOutlined /></template></a-input-password></label>
        <a-button type="primary" size="large" html-type="submit" block>{{ isSignup ? 'Create free account' : 'Sign in' }}</a-button>
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
</style>
