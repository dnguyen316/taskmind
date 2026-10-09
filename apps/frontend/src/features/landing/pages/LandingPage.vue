<script setup lang="ts">
import {
  ArrowRightOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons-vue'
import LandingHeader from '../components/LandingHeader.vue'
import LandingPreview from '../components/LandingPreview.vue'
import '../styles/landing.css'

const features = [
  {
    icon: RobotOutlined,
    title: 'Plan with AI',
    description: 'Turn a messy list of priorities into a focused plan you can review and refine.',
    link: 'Start with a thought',
    target: '#capture-a-thought',
  },
  {
    icon: CalendarOutlined,
    title: 'Own your schedule',
    description:
      'Make room for deep work, deadlines, and the unexpected. Adjust as your day changes.',
    link: 'Shape your day',
    target: '#make-room',
  },
  {
    icon: BarChartOutlined,
    title: 'See what matters',
    description: 'Keep tasks and projects in view, so your next step is always easy to find.',
    link: 'Keep moving forward',
    target: '#keep-going',
  },
]
const steps = [
  {
    id: 'capture-a-thought',
    title: 'Capture a thought',
    description:
      'Add a task in your own words. Give it a little context, then decide what to keep.',
  },
  {
    id: 'make-room',
    title: 'Make room for what matters',
    description: 'Review priorities and build a daily plan that fits the time you have.',
  },
  {
    id: 'keep-going',
    title: 'Adjust, then keep going',
    description: 'Check off progress, review suggestions, and choose how your plan changes.',
  },
]
const year = new Date().getFullYear()
</script>

<template>
  <div class="landing-page">
    <a class="landing-skip-link" href="#main-content">Skip to content</a>
    <LandingHeader />
    <main id="main-content" tabindex="-1">
      <section class="landing-container landing-hero" aria-labelledby="landing-heading">
        <div class="hero-copy">
          <h1 id="landing-heading">Make space for<br /><span>what matters.</span></h1>
          <p class="hero-description">
            Turn your priorities into a clear daily plan. Spend less time organizing work and more
            time doing your best work.
          </p>
          <div class="hero-actions">
            <RouterLink class="landing-button landing-button-primary" to="/signup"
              >Start for free <ArrowRightOutlined aria-hidden="true"
            /></RouterLink>
            <a class="landing-button landing-button-secondary" href="#how-it-works"
              >See how it works</a
            >
          </div>
          <p class="hero-note">Your next step, made clearer.</p>
        </div>
        <LandingPreview />
      </section>
      <section
        id="features"
        class="landing-features landing-container"
        aria-labelledby="features-heading"
      >
        <div class="landing-section-heading">
          <h2 id="features-heading">Less organizing. More momentum.</h2>
          <p>One place to capture your thoughts, shape your day, and see your progress.</p>
        </div>
        <div class="landing-feature-columns">
          <article v-for="feature in features" :key="feature.title" class="landing-feature">
            <component :is="feature.icon" class="landing-feature-icon" aria-hidden="true" />
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
            <a :href="feature.target"
              >{{ feature.link }} <ArrowRightOutlined aria-hidden="true"
            /></a>
          </article>
        </div>
      </section>
      <div class="landing-workflow-band">
        <section
          id="how-it-works"
          class="landing-container landing-workflow"
          aria-labelledby="workflow-heading"
        >
          <div>
            <div class="workflow-heading">
              <h2 id="workflow-heading">
                From a crowded mind<br class="desktop-break" />
                to a clear plan.
              </h2>
              <p>Start small. Make a plan. Stay in control.</p>
            </div>
            <ol class="landing-steps">
              <li v-for="(step, index) in steps" :id="step.id" :key="step.id" tabindex="-1">
                <span class="step-number" aria-hidden="true">0{{ index + 1 }}</span>
                <div>
                  <h3>{{ step.title }}</h3>
                  <p>{{ step.description }}</p>
                </div>
              </li>
            </ol>
          </div>
          <aside
            id="your-control"
            class="landing-trust"
            aria-labelledby="trust-heading"
            tabindex="-1"
          >
            <SafetyCertificateOutlined class="trust-icon" aria-hidden="true" />
            <h2 id="trust-heading">A little help. Your final say.</h2>
            <p>
              AI suggestions are a starting point. Review them, make them yours, and decide what
              happens next.
            </p>
            <ul>
              <li>
                <CheckCircleOutlined aria-hidden="true" />
                <div>
                  <h3>Review before you act</h3>
                  <p>Suggestions stay suggestions until you choose.</p>
                </div>
              </li>
              <li>
                <CheckCircleOutlined aria-hidden="true" />
                <div>
                  <h3>Change your mind</h3>
                  <p>Edit the plan as your priorities change.</p>
                </div>
              </li>
            </ul>
          </aside>
        </section>
        <section class="landing-container landing-final-cta" aria-labelledby="final-cta-heading">
          <h2 id="final-cta-heading">Make room for your next good idea.</h2>
          <RouterLink class="landing-button landing-button-primary" to="/signup"
            >Build your first plan <ArrowRightOutlined aria-hidden="true"
          /></RouterLink>
        </section>
      </div>
    </main>
    <footer class="landing-container landing-footer">
      <RouterLink class="landing-brand" to="/" aria-label="TaskMind home"
        ><span class="landing-brand-mark" aria-hidden="true"><CheckOutlined /></span
        ><span>TaskMind</span></RouterLink
      >
      <p>Plan clearly. Work calmly.</p>
      <div>
        <RouterLink to="/login">Sign in</RouterLink><span>© {{ year }} TaskMind</span>
      </div>
    </footer>
  </div>
</template>
