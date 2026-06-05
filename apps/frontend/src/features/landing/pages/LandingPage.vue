<script setup lang="ts">
import {
  ArrowRightOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CloseOutlined,
  MenuOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  ThunderboltFilled,
} from '@ant-design/icons-vue'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import ThemeToggle from '../../../components/ThemeToggle.vue'

const mobileMenuOpen = ref(false)
const landingPage = ref<HTMLElement | null>(null)
const heroVisual = ref<HTMLElement | null>(null)
const heroReady = ref(false)
let revealObserver: IntersectionObserver | undefined
let revealFallback: ReturnType<typeof window.setTimeout> | undefined

const features = [
  {
    icon: RobotOutlined,
    title: 'Plan with AI',
    description: 'Turn a messy list of priorities into a realistic, focused plan in seconds.',
    accent: 'violet',
  },
  {
    icon: CalendarOutlined,
    title: 'Own your schedule',
    description: 'Balance deep work, deadlines, and meetings without rebuilding your day by hand.',
    accent: 'blue',
  },
  {
    icon: BarChartOutlined,
    title: 'See what matters',
    description: 'Spot blockers early and keep every project moving with calm, clear insights.',
    accent: 'peach',
  },
]

const outcomes = [
  'Capture tasks in plain language',
  'Get a focused plan for every day',
  'Rebalance automatically when plans change',
]

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function updateHeroTilt(event: PointerEvent) {
  if (!heroVisual.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const bounds = heroVisual.value.getBoundingClientRect()
  const x = Math.min(Math.max((event.clientX - bounds.left) / bounds.width, 0), 1)
  const y = Math.min(Math.max((event.clientY - bounds.top) / bounds.height, 0), 1)

  heroVisual.value.style.setProperty('--tilt-x', `${(0.5 - y) * 7}deg`)
  heroVisual.value.style.setProperty('--tilt-y', `${(x - 0.5) * 9}deg`)
  heroVisual.value.style.setProperty('--shift-x', `${(x - 0.5) * 12}px`)
  heroVisual.value.style.setProperty('--shift-y', `${(y - 0.5) * 10}px`)
}

function resetHeroTilt() {
  heroVisual.value?.style.removeProperty('--tilt-x')
  heroVisual.value?.style.removeProperty('--tilt-y')
  heroVisual.value?.style.removeProperty('--shift-x')
  heroVisual.value?.style.removeProperty('--shift-y')
}

onMounted(async () => {
  await nextTick()
  heroReady.value = true

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    landingPage.value?.querySelectorAll('[data-reveal]').forEach((element) => element.classList.add('is-visible'))
    return
  }

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-visible')
        revealObserver?.unobserve(entry.target)
      })
    },
    { threshold: 0.16, rootMargin: '0px 0px -50px' },
  )

  landingPage.value?.querySelectorAll('[data-reveal]').forEach((element) => revealObserver?.observe(element))

  revealFallback = window.setTimeout(() => {
    landingPage.value?.querySelectorAll('[data-reveal]').forEach((element) => element.classList.add('is-visible'))
  }, 1800)
})

onBeforeUnmount(() => {
  revealObserver?.disconnect()
  if (revealFallback) window.clearTimeout(revealFallback)
})
</script>

<template>
  <main ref="landingPage" class="landing-page" :class="{ 'hero-ready': heroReady }">
    <header class="site-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/" aria-label="TaskMind home" @click="closeMobileMenu">
          <span class="brand-mark"><CheckOutlined /></span>
          <span>TaskMind</span>
        </RouterLink>

        <nav :class="{ 'nav-open': mobileMenuOpen }" aria-label="Main navigation">
          <a href="#features" @click="closeMobileMenu">Features</a>
          <a href="#how-it-works" @click="closeMobileMenu">How it works</a>
          <a href="#security" @click="closeMobileMenu">Security</a>
          <div class="mobile-nav-actions">
            <RouterLink class="nav-sign-in" to="/login" @click="closeMobileMenu">Sign in</RouterLink>
            <RouterLink class="primary-button compact-button" to="/signup" @click="closeMobileMenu">
              Start free <ArrowRightOutlined />
            </RouterLink>
          </div>
        </nav>

        <div class="header-actions">
          <ThemeToggle />
          <RouterLink class="nav-sign-in" to="/login">Sign in</RouterLink>
          <RouterLink class="primary-button compact-button" to="/signup">
            Start free <ArrowRightOutlined />
          </RouterLink>
        </div>

        <ThemeToggle />

        <button
          class="mobile-menu-button"
          type="button"
          aria-label="Toggle navigation"
          :aria-expanded="mobileMenuOpen"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <CloseOutlined v-if="mobileMenuOpen" />
          <MenuOutlined v-else />
        </button>
      </div>
    </header>

    <section class="hero-section">
      <div class="hero-copy">
        <div class="eyebrow"><span>✦</span> Your AI productivity partner</div>
        <h1>Make space for<br /><em>what matters.</em></h1>
        <p class="hero-description">
          TaskMind turns your priorities into a clear daily plan, so you can spend less time organizing
          work and more time doing your best work.
        </p>
        <div class="hero-actions">
          <RouterLink class="primary-button hero-button" to="/signup">
            Start for free <ArrowRightOutlined />
          </RouterLink>
          <a class="secondary-button hero-button" href="#how-it-works">See how it works</a>
        </div>
        <p class="hero-note">
          <span><CheckOutlined /> No credit card required</span>
          <i></i>
          <span><CheckOutlined /> Free plan included</span>
        </p>
      </div>

      <div ref="heroVisual" class="hero-visual" aria-label="TaskMind daily planning preview" @pointermove="updateHeroTilt" @pointerleave="resetHeroTilt">
        <div class="visual-orb visual-orb-main"></div>
        <div class="visual-orb visual-orb-small"></div>
        <div class="dot-grid" aria-hidden="true"></div>

        <div class="app-preview">
          <aside class="preview-sidebar">
            <span class="preview-logo"><CheckOutlined /></span>
            <span class="sidebar-item sidebar-item-active"><i></i></span>
            <span class="sidebar-item"><i></i></span>
            <span class="sidebar-item"><i></i></span>
            <span class="sidebar-item"><i></i></span>
            <span class="sidebar-avatar">AM</span>
          </aside>

          <div class="preview-main">
            <div class="preview-header">
              <div>
                <small>MONDAY, OCTOBER 14</small>
                <h2>Good morning, Alex</h2>
              </div>
              <button type="button" aria-label="Notifications"><span></span></button>
            </div>

            <div class="focus-card">
              <div class="focus-card-header">
                <span class="focus-icon"><ThunderboltFilled /></span>
                <div>
                  <small>DAILY FOCUS</small>
                  <strong>Make progress on launch</strong>
                </div>
                <span class="focus-time">3h 15m</span>
              </div>
              <div class="focus-progress"><span></span></div>
              <div class="focus-progress-meta"><span>3 of 5 tasks complete</span><strong>60%</strong></div>
            </div>

            <div class="task-list-heading"><strong>Today</strong><span>4 tasks</span></div>
            <div class="preview-task preview-task-done">
              <span class="task-check"><CheckOutlined /></span>
              <div><strong>Review launch brief</strong><small>Atlas project · 9:00 AM</small></div>
              <span class="task-tag done-tag">Done</span>
            </div>
            <div class="preview-task">
              <span class="task-check"></span>
              <div><strong>Finalize product narrative</strong><small>Deep work · 10:30 AM</small></div>
              <span class="task-tag high-tag">High</span>
            </div>
            <div class="preview-task">
              <span class="task-check"></span>
              <div><strong>Prepare customer preview</strong><small>Atlas project · 2:00 PM</small></div>
            </div>
          </div>
        </div>

        <div class="floating-card nova-card">
          <span class="floating-icon"><RobotOutlined /></span>
          <div><small>NOVA SUGGESTS</small><strong>Move research to tomorrow?</strong></div>
          <button type="button">Accept</button>
        </div>
        <div class="floating-card focus-time-card">
          <span class="floating-icon"><ClockCircleOutlined /></span>
          <div><small>FOCUS TIME</small><strong>2h 45m protected</strong></div>
        </div>
      </div>
    </section>

    <section id="features" class="features-section">
      <div class="section-heading" data-reveal>
        <span>BUILT FOR FOCUS</span>
        <h2>A calmer way to get things done.</h2>
        <p>Everything you need to move from a crowded mind to a confident plan.</p>
      </div>
      <div class="feature-grid">
        <article v-for="(feature, index) in features" :key="feature.title" class="feature-card" data-reveal :style="{ '--reveal-delay': `${index * 90}ms` }">
          <span class="feature-icon" :class="`feature-icon-${feature.accent}`"><component :is="feature.icon" /></span>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
          <a href="#how-it-works">Learn more <ArrowRightOutlined /></a>
        </article>
      </div>
    </section>

    <section id="how-it-works" class="outcome-section">
      <div class="outcome-copy" data-reveal>
        <span class="section-label">ONE THOUGHT AHEAD</span>
        <h2>Your workday,<br />made manageable.</h2>
        <p>TaskMind understands your workload and helps you make the next best decision — while leaving you in control.</p>
        <ul>
          <li v-for="outcome in outcomes" :key="outcome"><span><CheckOutlined /></span>{{ outcome }}</li>
        </ul>
        <RouterLink class="dark-button" to="/signup">Build your first plan <ArrowRightOutlined /></RouterLink>
      </div>

      <div id="security" class="trust-card" data-reveal>
        <div class="trust-card-glow"></div>
        <span class="trust-icon"><SafetyCertificateOutlined /></span>
        <small>DESIGNED FOR TRUST</small>
        <h3>Your plans stay yours.</h3>
        <p>Secure by design, transparent in every AI suggestion, and always under your control.</p>
        <div class="trust-detail"><CheckOutlined /><span><strong>Private by default</strong><small>Your work is never shared.</small></span></div>
      </div>
    </section>

    <footer class="site-footer">
      <RouterLink class="brand footer-brand" to="/"><span class="brand-mark"><CheckOutlined /></span><span>TaskMind</span></RouterLink>
      <p>Plan clearly. Work calmly.</p>
      <span>© 2026 TaskMind</span>
    </footer>
  </main>
</template>

<style scoped>
.landing-page {
  --ink: var(--tm-text);
  --muted: var(--tm-text-muted);
  --purple: var(--tm-primary);
  --purple-dark: var(--tm-primary-hover);
  --line: var(--tm-border);
  min-height: 100vh;
  overflow: hidden;
  color: var(--ink);
  background: var(--tm-bg);
  transition: color 180ms ease, background 180ms ease;
}

.site-header {
  position: relative;
  z-index: 20;
  border-bottom: 1px solid rgba(232, 233, 241, 0.7);
  background: color-mix(in srgb, var(--tm-bg) 86%, transparent);
  backdrop-filter: blur(18px);
}

.header-inner {
  width: min(1240px, calc(100% - 48px));
  height: 76px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 50px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--ink);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.7px;
  text-decoration: none;
}

.brand-mark {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  color: #fff;
  border-radius: 10px;
  background: linear-gradient(145deg, #7466ec, #4b3bc4);
  box-shadow: 0 8px 20px rgba(89, 72, 207, 0.25);
}

nav {
  display: flex;
  align-items: center;
  gap: 32px;
}

nav > a,
.nav-sign-in {
  color: #626b80;
  font-size: 13px;
  font-weight: 650;
  text-decoration: none;
  transition: color 160ms ease;
}

nav > a:hover,
.nav-sign-in:hover {
  color: var(--purple);
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 24px;
}

.mobile-nav-actions,
.mobile-menu-button {
  display: none;
}

.primary-button,
.secondary-button,
.dark-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 0;
  border-radius: 11px;
  font-size: 13px;
  font-weight: 750;
  text-decoration: none;
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.primary-button:hover,
.secondary-button:hover,
.dark-button:hover {
  transform: translateY(-2px);
}

.primary-button {
  color: #fff;
  background: linear-gradient(135deg, #6b5ce3, #5142cb);
  box-shadow: 0 12px 26px rgba(84, 67, 202, 0.24);
}

.compact-button {
  min-height: 42px;
  padding: 0 18px;
}

.hero-section {
  width: min(1240px, calc(100% - 48px));
  min-height: 680px;
  margin: 0 auto;
  padding: 76px 0 74px;
  display: grid;
  grid-template-columns: 0.88fr 1.12fr;
  align-items: center;
  gap: 56px;
}

.hero-copy {
  position: relative;
  z-index: 5;
  padding-bottom: 12px;
}

.eyebrow {
  width: max-content;
  padding: 8px 13px;
  display: flex;
  align-items: center;
  gap: 7px;
  border: 1px solid #dedafa;
  border-radius: 999px;
  color: #5d50cf;
  background: #f5f3ff;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.04em;
}

.eyebrow span {
  font-size: 12px;
}

.hero-copy h1 {
  margin: 25px 0 21px;
  color: var(--ink);
  font-size: clamp(56px, 5.3vw, 72px);
  line-height: 1.03;
  letter-spacing: -4.8px;
}

.hero-copy h1 em {
  color: #5e4fd5;
  font-style: normal;
}

.hero-description {
  max-width: 520px;
  margin: 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.72;
}

.hero-actions {
  margin-top: 31px;
  display: flex;
  gap: 12px;
}

.hero-button {
  min-height: 52px;
  padding: 0 24px;
}

.secondary-button {
  border: 1px solid var(--tm-border);
  color: var(--tm-text);
  background: var(--tm-surface);
  box-shadow: 0 6px 15px rgba(29, 35, 64, 0.04);
}

.hero-note {
  margin: 19px 0 0;
  display: flex;
  align-items: center;
  gap: 11px;
  color: #9299a9;
  font-size: 11px;
}

.hero-note span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.hero-note :deep(svg) {
  color: var(--purple);
}

.hero-note i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #b3b8c5;
}

.hero-visual {
  position: relative;
  min-height: 530px;
}

.visual-orb {
  position: absolute;
  border-radius: 50%;
}

.visual-orb-main {
  width: 560px;
  height: 560px;
  top: -16px;
  right: -108px;
  background: linear-gradient(145deg, #e8e4ff, #f7f6ff 76%);
}

.visual-orb-small {
  width: 240px;
  height: 240px;
  bottom: -12px;
  left: -38px;
  background: #eaf5ff;
}

.dot-grid {
  position: absolute;
  top: 12px;
  left: 1px;
  width: 92px;
  height: 76px;
  opacity: 0.42;
  background-image: radial-gradient(#9c93dc 1.1px, transparent 1.1px);
  background-size: 12px 12px;
}

.app-preview {
  position: absolute;
  z-index: 3;
  top: 42px;
  left: 32px;
  width: 600px;
  height: 410px;
  display: flex;
  overflow: hidden;
  border: 1px solid rgba(221, 223, 236, 0.9);
  border-radius: 18px;
  background: var(--tm-surface);
  box-shadow: 0 36px 80px rgba(44, 39, 105, 0.16);
  transform: rotate(1.3deg);
}

.preview-sidebar {
  width: 64px;
  padding: 18px 0 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  border-right: 1px solid var(--tm-border-soft);
  background: var(--tm-surface-subtle);
}

.preview-logo {
  width: 29px;
  height: 29px;
  margin-bottom: 15px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--purple);
  font-size: 11px;
}

.sidebar-item {
  width: 30px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 7px;
}

.sidebar-item i {
  width: 14px;
  height: 5px;
  border-radius: 5px;
  background: #d7d9e3;
}

.sidebar-item-active {
  background: #eeecfc;
}

.sidebar-item-active i {
  background: #6859dc;
}

.sidebar-avatar {
  width: 26px;
  height: 26px;
  margin-top: auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #25304a;
  font-size: 7px;
  font-weight: 700;
}

.preview-main {
  flex: 1;
  padding: 24px 26px;
}

.preview-header {
  margin-bottom: 21px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.preview-header small {
  color: #a1a7b4;
  font-size: 7px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.preview-header h2 {
  margin: 5px 0 0;
  font-size: 17px;
  letter-spacing: -0.6px;
}

.preview-header button {
  position: relative;
  width: 29px;
  height: 29px;
  border: 1px solid var(--tm-border-soft);
  border-radius: 8px;
  background: var(--tm-surface);
}

.preview-header button::before {
  content: '';
  position: absolute;
  inset: 9px;
  border: 1.5px solid #abb0bc;
  border-radius: 50% 50% 42% 42%;
}

.preview-header button span {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 5px;
  height: 5px;
  border: 1px solid #fff;
  border-radius: 50%;
  background: #e46c6c;
}

.focus-card {
  padding: 15px 16px 13px;
  border: 1px solid #e3e0f8;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--tm-surface-raised), var(--tm-primary-soft));
}

.focus-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.focus-icon {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--purple);
  font-size: 12px;
}

.focus-card-header div {
  display: grid;
  gap: 2px;
}

.focus-card-header small,
.floating-card small {
  color: #8075d8;
  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.focus-card-header strong {
  font-size: 11px;
}

.focus-time {
  margin-left: auto;
  color: #9a9fac;
  font-size: 8px;
}

.focus-progress {
  height: 5px;
  margin-top: 14px;
  overflow: hidden;
  border-radius: 10px;
  background: #e5e2f3;
}

.focus-progress span {
  width: 60%;
  height: 100%;
  display: block;
  border-radius: inherit;
  background: var(--purple);
}

.focus-progress-meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  color: #9da2af;
  font-size: 7px;
}

.focus-progress-meta strong {
  color: var(--purple);
}

.task-list-heading {
  margin: 20px 1px 7px;
  display: flex;
  justify-content: space-between;
}

.task-list-heading strong {
  font-size: 11px;
}

.task-list-heading span {
  color: #a0a5b1;
  font-size: 8px;
}

.preview-task {
  min-height: 51px;
  padding: 10px 2px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid var(--tm-border-soft);
}

.task-check {
  width: 17px;
  height: 17px;
  flex: 0 0 17px;
  display: grid;
  place-items: center;
  border: 1.5px solid #cdd1dc;
  border-radius: 50%;
  font-size: 7px;
}

.preview-task div {
  display: grid;
  gap: 3px;
}

.preview-task strong {
  font-size: 9px;
}

.preview-task small {
  color: #a0a5b1;
  font-size: 7px;
}

.preview-task-done .task-check {
  border-color: var(--purple);
  color: #fff;
  background: var(--purple);
}

.preview-task-done strong {
  color: #9da2ae;
  text-decoration: line-through;
}

.task-tag {
  margin-left: auto;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 7px;
  font-weight: 700;
}

.done-tag {
  color: #43866b;
  background: #ebf7f1;
}

.high-tag {
  color: #be6842;
  background: #fff0e9;
}

.floating-card {
  position: absolute;
  z-index: 5;
  padding: 12px 13px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--tm-border);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 38px rgba(42, 37, 103, 0.14);
  backdrop-filter: blur(10px);
}

.floating-icon {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: var(--purple);
  background: #eeebff;
}

.floating-card div {
  display: grid;
  gap: 3px;
}

.floating-card strong {
  font-size: 9px;
}

.nova-card {
  bottom: 29px;
  left: -1px;
}

.nova-card button {
  padding: 7px 10px;
  border: 0;
  border-radius: 7px;
  color: #fff;
  background: var(--purple);
  font-size: 7px;
  font-weight: 700;
}

.focus-time-card {
  top: 68px;
  right: -23px;
}

.features-section {
  padding: 102px 24px 108px;
  background: var(--tm-surface);
}

.section-heading {
  max-width: 620px;
  margin: 0 auto 47px;
  text-align: center;
}

.section-heading > span,
.section-label {
  color: var(--purple);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.15em;
}

.section-heading h2,
.outcome-copy h2 {
  margin: 12px 0 10px;
  color: var(--ink);
  font-size: 42px;
  line-height: 1.14;
  letter-spacing: -2.3px;
}

.section-heading p,
.outcome-copy > p,
.trust-card > p {
  color: #747d90;
  line-height: 1.7;
}

.section-heading p {
  margin: 0;
  font-size: 14px;
}

.feature-grid {
  width: min(1160px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  min-height: 245px;
  padding: 30px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--tm-surface);
  box-shadow: 0 16px 42px rgba(28, 35, 64, 0.045);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 22px 52px rgba(28, 35, 64, 0.08);
}

.feature-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  font-size: 19px;
}

.feature-icon-violet {
  color: #5d4fd3;
  background: var(--tm-primary-soft);
}

.feature-icon-blue {
  color: #3e73bd;
  background: #eaf4ff;
}

.feature-icon-peach {
  color: #ba6c46;
  background: #fff1e9;
}

.feature-card h3 {
  margin: 21px 0 8px;
  font-size: 17px;
  letter-spacing: -0.3px;
}

.feature-card p {
  min-height: 66px;
  margin: 0 0 16px;
  color: #747d90;
  font-size: 13px;
  line-height: 1.68;
}

.feature-card a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #5b4cd0;
  font-size: 11px;
  font-weight: 750;
  text-decoration: none;
}

.outcome-section {
  width: min(1160px, calc(100% - 48px));
  margin: 0 auto;
  padding: 112px 0 116px;
  display: grid;
  grid-template-columns: 1fr 0.82fr;
  align-items: center;
  gap: 100px;
}

.outcome-copy > p {
  max-width: 520px;
  margin: 0;
  font-size: 14px;
}

.outcome-copy ul {
  margin: 26px 0 31px;
  padding: 0;
  display: grid;
  gap: 14px;
  list-style: none;
}

.outcome-copy li {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #4d566b;
  font-size: 13px;
}

.outcome-copy li > span {
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--purple);
  background: var(--tm-primary-soft);
  font-size: 8px;
}

.dark-button {
  min-height: 49px;
  padding: 0 21px;
  color: var(--tm-inverse);
  background: var(--tm-inverse-surface);
  box-shadow: var(--tm-shadow-md);
}

.trust-card {
  position: relative;
  overflow: hidden;
  padding: 43px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 24px;
  color: #fff;
  background: linear-gradient(145deg, #1b2540, #2c3760);
  box-shadow: 0 30px 64px rgba(23, 33, 58, 0.2);
}

.trust-card-glow {
  position: absolute;
  top: -100px;
  right: -90px;
  width: 250px;
  height: 250px;
  border-radius: 50%;
  background: rgba(117, 102, 237, 0.22);
  filter: blur(10px);
}

.trust-icon {
  position: relative;
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.09);
  font-size: 23px;
}

.trust-card > small {
  display: block;
  margin-top: 31px;
  color: #bbb6fa;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.trust-card h3 {
  margin: 9px 0 9px;
  font-size: 27px;
  letter-spacing: -1px;
}

.trust-card > p {
  margin: 0 0 24px;
  color: #c2c8d5;
  font-size: 13px;
}

.trust-detail {
  padding-top: 19px;
  display: flex;
  align-items: center;
  gap: 11px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: #b9b2fa;
}

.trust-detail > span {
  display: grid;
  gap: 2px;
}

.trust-detail strong {
  color: #fff;
  font-size: 11px;
}

.trust-detail small {
  color: #aeb5c5;
  font-size: 9px;
}

.site-footer {
  width: min(1160px, calc(100% - 48px));
  margin: 0 auto;
  padding: 31px 0;
  display: flex;
  align-items: center;
  gap: 28px;
  border-top: 1px solid var(--line);
  color: #8b93a4;
  font-size: 11px;
}

.footer-brand {
  font-size: 15px;
}

.footer-brand .brand-mark {
  width: 26px;
  height: 26px;
  border-radius: 8px;
}

.site-footer p {
  margin-right: auto;
}

@media (max-width: 1120px) {
  .hero-section {
    grid-template-columns: 1fr;
    padding-top: 66px;
  }

  .hero-copy {
    text-align: center;
  }

  .eyebrow,
  .hero-description {
    margin-left: auto;
    margin-right: auto;
  }

  .hero-actions,
  .hero-note {
    justify-content: center;
  }

  .hero-visual {
    width: min(680px, 100%);
    margin: -4px auto 0;
  }

  .app-preview {
    left: 50%;
  }

  .outcome-section {
    gap: 56px;
  }
}

@media (max-width: 820px) {
  .header-inner {
    width: min(100% - 32px, 1240px);
    height: 70px;
    gap: 14px;
  }

  .header-actions {
    display: none;
  }

  .mobile-menu-button {
    width: 40px;
    height: 40px;
    margin-left: auto;
    display: grid;
    place-items: center;
    border: 1px solid var(--tm-border);
    border-radius: 10px;
    color: var(--ink);
    background: var(--tm-surface);
  }

  nav {
    position: absolute;
    top: 62px;
    left: 16px;
    right: 16px;
    padding: 19px;
    display: none;
    align-items: stretch;
    flex-direction: column;
    gap: 17px;
    border: 1px solid var(--tm-border);
    border-radius: 14px;
    background: var(--tm-surface);
    box-shadow: 0 22px 45px rgba(31, 37, 68, 0.12);
  }

  nav.nav-open {
    display: flex;
  }

  .mobile-nav-actions {
    padding-top: 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-top: 1px solid var(--tm-border-soft);
  }

  .feature-grid,
  .outcome-section {
    grid-template-columns: 1fr;
  }

  .feature-card p {
    min-height: 0;
  }

  .outcome-section {
    gap: 48px;
  }
}

@media (max-width: 660px) {
  .brand {
    font-size: 18px;
  }

  .hero-section {
    width: calc(100% - 36px);
    min-height: auto;
    padding: 52px 0 48px;
    gap: 38px;
  }

  .hero-copy h1 {
    margin-top: 20px;
    font-size: 47px;
    letter-spacing: -3.4px;
  }

  .hero-description {
    font-size: 15px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-note {
    flex-wrap: wrap;
  }

  .hero-note i {
    display: none;
  }

  .hero-visual {
    min-height: 330px;
  }

  .visual-orb-main {
    width: 350px;
    height: 350px;
    right: -80px;
  }

  .visual-orb-small,
  .dot-grid,
  .floating-card {
    display: none;
  }

  .app-preview {
    top: 6px;
    width: 600px;
    transform-origin: top center;
  }

  .features-section {
    padding: 76px 18px 80px;
  }

  .section-heading h2,
  .outcome-copy h2 {
    font-size: 34px;
    letter-spacing: -1.8px;
  }

  .feature-card {
    min-height: 0;
    padding: 25px;
  }

  .outcome-section {
    width: calc(100% - 36px);
    padding: 78px 0 82px;
  }

  .trust-card {
    padding: 30px;
  }

  .site-footer {
    width: calc(100% - 36px);
    flex-wrap: wrap;
    gap: 14px;
  }

  .site-footer p {
    width: 100%;
    margin: 0;
    order: 3;
  }
}

/* The composition follows the supplied landing design; these overrides only map it to the shared dark theme. */
:global(:root[data-theme='dark']) .site-header {
  border-color: var(--tm-border-soft);
  background: color-mix(in srgb, var(--tm-bg) 86%, transparent);
}

:global(:root[data-theme='dark']) .brand,
:global(:root[data-theme='dark']) .hero-copy h1,
:global(:root[data-theme='dark']) .section-heading h2,
:global(:root[data-theme='dark']) .outcome-copy h2,
:global(:root[data-theme='dark']) .feature-card h3,
:global(:root[data-theme='dark']) .app-preview,
:global(:root[data-theme='dark']) .preview-header h2,
:global(:root[data-theme='dark']) .preview-task strong,
:global(:root[data-theme='dark']) .floating-card strong {
  color: var(--tm-text);
}

:global(:root[data-theme='dark']) nav > a,
:global(:root[data-theme='dark']) .nav-sign-in,
:global(:root[data-theme='dark']) .hero-description,
:global(:root[data-theme='dark']) .section-heading p,
:global(:root[data-theme='dark']) .outcome-copy > p,
:global(:root[data-theme='dark']) .feature-card p,
:global(:root[data-theme='dark']) .outcome-copy li,
:global(:root[data-theme='dark']) .site-footer {
  color: var(--tm-text-muted);
}

:global(:root[data-theme='dark']) .secondary-button,
:global(:root[data-theme='dark']) .feature-card,
:global(:root[data-theme='dark']) .app-preview,
:global(:root[data-theme='dark']) .floating-card,
:global(:root[data-theme='dark']) .preview-header button,
:global(:root[data-theme='dark']) nav.nav-open {
  border-color: var(--tm-border);
  color: var(--tm-text);
  background: var(--tm-surface-raised);
  box-shadow: var(--tm-shadow-md);
}

:global(:root[data-theme='dark']) .features-section,
:global(:root[data-theme='dark']) .preview-main {
  background: var(--tm-surface);
}

:global(:root[data-theme='dark']) .preview-sidebar {
  border-color: var(--tm-border-soft);
  background: var(--tm-surface-subtle);
}

:global(:root[data-theme='dark']) .focus-card {
  border-color: var(--tm-primary-soft-border);
  background: linear-gradient(135deg, var(--tm-surface-raised), var(--tm-primary-soft));
}

:global(:root[data-theme='dark']) .preview-task,
:global(:root[data-theme='dark']) .trust-detail,
:global(:root[data-theme='dark']) .site-footer,
:global(:root[data-theme='dark']) .mobile-nav-actions {
  border-color: var(--tm-border-soft);
}

:global(:root[data-theme='dark']) .visual-orb-main {
  background: linear-gradient(145deg, #282750, #151a30 76%);
}

:global(:root[data-theme='dark']) .visual-orb-small {
  background: #14283d;
}

:global(:root[data-theme='dark']) .feature-icon-violet,
:global(:root[data-theme='dark']) .floating-icon,
:global(:root[data-theme='dark']) .outcome-copy li > span,
:global(:root[data-theme='dark']) .sidebar-item-active {
  color: var(--tm-primary);
  background: var(--tm-primary-soft);
}

:global(:root[data-theme='dark']) .feature-icon-blue {
  color: #82b8f0;
  background: #19324e;
}

:global(:root[data-theme='dark']) .feature-icon-peach {
  color: var(--tm-warning);
  background: var(--tm-warning-soft);
}

:global(:root[data-theme='dark']) .done-tag {
  color: var(--tm-success);
  background: var(--tm-success-soft);
}

:global(:root[data-theme='dark']) .high-tag {
  color: var(--tm-warning);
  background: var(--tm-warning-soft);
}

:global(:root[data-theme='dark']) .trust-card {
  border-color: var(--tm-border);
  background: linear-gradient(145deg, #171d31, #252a4f);
}

.header-inner > .theme-toggle {
  display: none;
}

@media (max-width: 820px) {
  .header-inner > .theme-toggle {
    margin-left: auto;
    display: grid;
  }

  .mobile-menu-button {
    margin-left: 0;
  }
}

/* Motion is progressive enhancement: layout and content remain complete without it. */
.hero-copy > *,
.hero-visual {
  opacity: 0;
}

.hero-ready .hero-copy > * {
  animation: hero-copy-in 720ms cubic-bezier(.2,.75,.25,1) both;
}

.hero-ready .hero-copy > :nth-child(2) { animation-delay: 80ms; }
.hero-ready .hero-copy > :nth-child(3) { animation-delay: 150ms; }
.hero-ready .hero-copy > :nth-child(4) { animation-delay: 220ms; }
.hero-ready .hero-copy > :nth-child(5) { animation-delay: 290ms; }

.hero-ready .hero-visual {
  animation: hero-visual-in 900ms 120ms cubic-bezier(.16,.82,.24,1) both;
}

.hero-visual {
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  --shift-x: 0px;
  --shift-y: 0px;
  perspective: 1400px;
  transform-style: preserve-3d;
}

.app-preview {
  --preview-offset-x: 0%;
  --preview-scale: 1;
  transform: translateX(var(--preview-offset-x)) perspective(1400px) rotateX(var(--tilt-x)) rotateY(var(--tilt-y)) rotateZ(1.3deg) scale(var(--preview-scale));
  transform-origin: center;
  transform-style: preserve-3d;
  transition: transform 220ms cubic-bezier(.2,.75,.25,1), box-shadow 220ms ease;
  will-change: transform;
}

.hero-visual:hover .app-preview {
  box-shadow: 0 44px 95px rgba(44, 39, 105, 0.23);
}

.preview-sidebar,
.preview-main {
  transform: translateZ(10px);
}

.focus-card {
  transform: translateZ(20px);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--tm-primary) 10%, transparent);
}

.floating-card {
  transition: transform 240ms cubic-bezier(.2,.75,.25,1), box-shadow 200ms ease;
  will-change: transform;
}

.nova-card {
  animation: card-float 4.8s ease-in-out infinite;
  transform: translate3d(calc(var(--shift-x) * -0.6), calc(var(--shift-y) * -0.5), 42px);
}

.focus-time-card {
  animation: card-float-delayed 5.2s ease-in-out infinite;
  transform: translate3d(calc(var(--shift-x) * 0.75), calc(var(--shift-y) * 0.65), 55px);
}

.visual-orb-main { animation: orb-drift 9s ease-in-out infinite alternate; }
.visual-orb-small { animation: orb-drift-small 7s ease-in-out infinite alternate; }
.dot-grid { animation: dots-breathe 4s ease-in-out infinite; }
.preview-logo,
.brand-mark { transition: transform 180ms ease, box-shadow 180ms ease; }
.brand:hover .brand-mark { transform: rotate(-7deg) scale(1.08); box-shadow: 0 12px 26px rgba(89,72,207,.34); }
.focus-progress span { transform-origin: left; animation: progress-grow 1.2s 550ms cubic-bezier(.2,.75,.25,1) both; }
.preview-header button span { animation: notification-pulse 2.2s ease-out infinite; }

[data-reveal] {
  opacity: 0;
  transform: translateY(28px);
  transition: opacity 680ms cubic-bezier(.2,.75,.25,1), transform 680ms cubic-bezier(.2,.75,.25,1);
  transition-delay: var(--reveal-delay, 0ms);
}

[data-reveal].is-visible {
  opacity: 1;
  transform: translateY(0);
}

.feature-card {
  transform-style: preserve-3d;
}

.feature-card:hover {
  transform: translateY(-7px) rotateX(1.5deg);
}

.feature-card:hover .feature-icon {
  transform: translateY(-3px) rotate(-5deg) scale(1.08);
}

.feature-icon,
.feature-card a :deep(svg),
.primary-button :deep(svg),
.dark-button :deep(svg) {
  transition: transform 180ms ease;
}

.feature-card:hover a :deep(svg),
.primary-button:hover :deep(svg),
.dark-button:hover :deep(svg) {
  transform: translateX(4px);
}

.trust-card {
  transition: transform 260ms cubic-bezier(.2,.75,.25,1), box-shadow 260ms ease;
}

.trust-card:hover {
  transform: translateY(-6px) rotateY(-1.5deg);
  box-shadow: 0 38px 76px rgba(23,33,58,.27);
}

.trust-card:hover .trust-icon {
  transform: rotate(-6deg) scale(1.06);
}

.trust-icon { transition: transform 200ms ease; }
.trust-card-glow { animation: trust-glow 6s ease-in-out infinite alternate; }

.primary-button:focus-visible,
.secondary-button:focus-visible,
.dark-button:focus-visible,
.feature-card a:focus-visible,
nav a:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--tm-primary) 45%, transparent);
  outline-offset: 4px;
}

@keyframes hero-copy-in {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes hero-visual-in {
  from { opacity: 0; transform: translate3d(30px, 18px, 0) scale(.97); }
  to { opacity: 1; transform: translate3d(0, 0, 0) scale(1); }
}

@keyframes card-float {
  0%, 100% { margin-top: 0; }
  50% { margin-top: -8px; }
}

@keyframes card-float-delayed {
  0%, 100% { margin-top: -3px; }
  50% { margin-top: 6px; }
}

@keyframes orb-drift {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(-12px, 9px, 0) scale(1.025); }
}

@keyframes orb-drift-small {
  from { transform: translate3d(0, 5px, 0); }
  to { transform: translate3d(10px, -6px, 0); }
}

@keyframes dots-breathe {
  0%, 100% { opacity: .28; transform: translateY(0); }
  50% { opacity: .58; transform: translateY(-4px); }
}

@keyframes progress-grow {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@keyframes notification-pulse {
  0%, 55% { box-shadow: 0 0 0 0 rgba(228,108,108,.4); }
  100% { box-shadow: 0 0 0 7px rgba(228,108,108,0); }
}

@keyframes trust-glow {
  from { transform: translate3d(0, 0, 0) scale(1); opacity: .7; }
  to { transform: translate3d(-18px, 18px, 0) scale(1.12); opacity: 1; }
}

@media (max-width: 1120px) {
  .app-preview { --preview-offset-x: -50%; }
}

@media (max-width: 660px) {
  .app-preview { --preview-scale: .64; }
  .hero-copy { text-align: center; }
  .hero-description { max-width: 410px; }
  .section-heading { max-width: 350px; }
  .section-heading h2 { text-wrap: balance; }
}

@media (prefers-reduced-motion: reduce) {
  .hero-copy > *,
  .hero-visual,
  [data-reveal] {
    opacity: 1 !important;
    transform: none !important;
    animation: none !important;
    transition-duration: 1ms !important;
  }

  .app-preview {
    transform: translateX(var(--preview-offset-x)) rotateZ(1.3deg) scale(var(--preview-scale)) !important;
  }

  .floating-card,
  .visual-orb,
  .dot-grid,
  .focus-progress span,
  .preview-header button span,
  .trust-card-glow {
    animation: none !important;
  }
}

</style>
