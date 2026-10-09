<script setup lang="ts">
import {
  ArrowRightOutlined,
  CheckOutlined,
  CloseOutlined,
  MenuOutlined,
} from '@ant-design/icons-vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import ThemeToggle from '../../../components/ThemeToggle.vue'

const menuOpen = ref(false)
const header = ref<HTMLElement | null>(null)
const menuButton = ref<HTMLButtonElement | null>(null)
let desktopQuery: MediaQueryList | undefined

function closeMenu() {
  menuOpen.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && menuOpen.value) {
    closeMenu()
    menuButton.value?.focus()
  }
}

function onPointerdown(event: PointerEvent) {
  if (event.target instanceof Node && !header.value?.contains(event.target)) closeMenu()
}

function onFocusout(event: FocusEvent) {
  if (event.relatedTarget instanceof Node && !header.value?.contains(event.relatedTarget))
    closeMenu()
}

onMounted(() => {
  desktopQuery = window.matchMedia('(min-width: 861px)')
  desktopQuery.addEventListener('change', closeMenu)
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('pointerdown', onPointerdown)
})

onBeforeUnmount(() => {
  desktopQuery?.removeEventListener('change', closeMenu)
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('pointerdown', onPointerdown)
})
</script>

<template>
  <header ref="header" class="landing-header" @focusout="onFocusout">
    <div class="landing-container header-inner">
      <RouterLink class="landing-brand" to="/" aria-label="TaskMind home" @click="closeMenu">
        <span class="landing-brand-mark" aria-hidden="true"><CheckOutlined /></span>
        <span>TaskMind</span>
      </RouterLink>

      <div class="header-actions">
        <ThemeToggle />
        <RouterLink class="landing-button landing-button-secondary header-sign-in" to="/login"
          >Sign in</RouterLink
        >
        <RouterLink class="landing-button landing-button-primary header-signup" to="/signup">
          Start free <ArrowRightOutlined aria-hidden="true" />
        </RouterLink>
        <button
          ref="menuButton"
          class="landing-menu-button"
          type="button"
          :aria-label="menuOpen ? 'Close navigation' : 'Open navigation'"
          :aria-expanded="menuOpen"
          aria-controls="landing-navigation"
          @click="menuOpen = !menuOpen"
        >
          <CloseOutlined v-if="menuOpen" aria-hidden="true" />
          <MenuOutlined v-else aria-hidden="true" />
        </button>
      </div>

      <nav
        id="landing-navigation"
        class="landing-nav"
        :class="{ 'is-open': menuOpen }"
        aria-label="Main navigation"
      >
        <a href="#features" @click="closeMenu">Features</a>
        <a href="#how-it-works" @click="closeMenu">How it works</a>
        <a href="#your-control" @click="closeMenu">Your control</a>
        <div class="mobile-nav-actions">
          <RouterLink class="landing-button landing-button-secondary" to="/login" @click="closeMenu"
            >Sign in</RouterLink
          >
          <RouterLink class="landing-button landing-button-primary" to="/signup" @click="closeMenu">
            Start free <ArrowRightOutlined aria-hidden="true" />
          </RouterLink>
        </div>
      </nav>
    </div>
  </header>
</template>
