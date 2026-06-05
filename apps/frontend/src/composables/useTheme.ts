import { computed, ref } from 'vue'

type Theme = 'light' | 'dark'

const STORAGE_KEY = 'taskmind-theme'
const theme = ref<Theme>('light')
let initialized = false

function preferredTheme(): Theme {
  if (typeof window === 'undefined') return 'light'

  const savedTheme = window.localStorage.getItem(STORAGE_KEY)
  if (savedTheme === 'light' || savedTheme === 'dark') return savedTheme

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(nextTheme: Theme) {
  theme.value = nextTheme
  if (typeof document !== 'undefined') document.documentElement.dataset.theme = nextTheme
}

export function useTheme() {
  if (!initialized) {
    applyTheme(preferredTheme())
    initialized = true
  }

  const isDark = computed(() => theme.value === 'dark')

  function toggleTheme() {
    const nextTheme: Theme = isDark.value ? 'light' : 'dark'
    applyTheme(nextTheme)
    window.localStorage.setItem(STORAGE_KEY, nextTheme)
  }

  return { theme, isDark, toggleTheme }
}
