import { createApp } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import 'ant-design-vue/dist/reset.css'
import './style.css'

const app = createApp(App)

app.config.errorHandler = (error: unknown, instance: ComponentPublicInstance | null, info: string) => {
  console.error('Global Vue error handler:', {
    error,
    info,
    component: instance?.$options?.name ?? instance?.$options?.__name ?? 'anonymous',
  })
}

app.use(Antd).use(router).mount('#app')
