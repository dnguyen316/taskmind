import { createApp } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import { onSessionExpired } from './lib/authSession'
import { pinia } from './stores/pinia'
import { useAuthStore } from './stores/auth'
import 'ant-design-vue/dist/reset.css'
import './style.css'

const app = createApp(App)

app.config.errorHandler = (
  error: unknown,
  instance: ComponentPublicInstance | null,
  info: string,
) => {
  console.error('Global Vue error handler:', {
    error,
    info,
    component: instance?.$options?.name ?? instance?.$options?.__name ?? 'anonymous',
  })
}

app.use(pinia)

useAuthStore(pinia).initializeSession()

onSessionExpired(() => {
  const currentRoute = router.currentRoute.value
  useAuthStore(pinia).markUnauthenticated()

  if (currentRoute.name === 'login' || currentRoute.name === 'signup') {
    return
  }

  void router.push({
    name: 'login',
    query: currentRoute.fullPath === '/' ? undefined : { redirect: currentRoute.fullPath },
  })
})

app.use(Antd).use(router).mount('#app')
