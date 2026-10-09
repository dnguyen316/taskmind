<script setup lang="ts">
import { CheckOutlined, RobotOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { computed, ref } from 'vue'

const tasks = ref([
  { id: 'brief', title: 'Review launch brief', context: 'Atlas project · 9:00 AM', done: true },
  {
    id: 'narrative',
    title: 'Finalize product narrative',
    context: 'Deep work · 10:30 AM',
    done: false,
  },
  {
    id: 'preview',
    title: 'Prepare customer preview',
    context: 'Atlas project · 2:00 PM',
    done: false,
  },
])
const rescheduled = ref(false)
const announcement = ref('')
const todayTasks = computed(() =>
  tasks.value.filter((task) => !rescheduled.value || task.id !== 'preview'),
)
const completedCount = computed(() => todayTasks.value.filter((task) => task.done).length)
const progress = computed(() => Math.round((completedCount.value / todayTasks.value.length) * 100))

function toggleTask(id: string) {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return
  task.done = !task.done
  announcement.value = `${task.title} marked ${task.done ? 'complete' : 'incomplete'}. ${completedCount.value} of ${todayTasks.value.length} tasks complete.`
}

function toggleSuggestion() {
  rescheduled.value = !rescheduled.value
  announcement.value = rescheduled.value
    ? 'Customer preview moved to tomorrow in this example. You can undo this change.'
    : 'Customer preview restored to today in this example.'
}
</script>

<template>
  <div class="preview-stage">
    <section class="landing-preview" aria-label="Interactive example daily plan">
      <div class="preview-toolbar"><span>Interactive preview</span><span>Example day</span></div>
      <div class="preview-content">
        <p class="preview-day">Today</p>
        <h2>Your day, with a little more focus.</h2>
        <div class="preview-focus">
          <ThunderboltOutlined aria-hidden="true" /><strong>Launch the next chapter</strong>
        </div>
        <div class="preview-progress-label" aria-hidden="true">
          <span>{{ completedCount }} of {{ todayTasks.length }} tasks complete</span
          ><span>{{ progress }}%</span>
        </div>
        <progress
          :value="completedCount"
          :max="todayTasks.length"
          aria-label="Example daily task completion"
        >
          {{ progress }}%
        </progress>

        <div class="preview-tasks">
          <label
            v-for="task in todayTasks"
            :key="task.id"
            class="preview-task"
            :class="{ 'is-done': task.done }"
          >
            <input
              type="checkbox"
              :checked="task.done"
              :aria-label="task.title"
              @change="toggleTask(task.id)"
            />
            <span class="preview-check" aria-hidden="true"><CheckOutlined v-if="task.done" /></span>
            <span class="preview-task-copy"
              ><strong>{{ task.title }}</strong
              ><small>{{ task.context }}</small></span
            >
          </label>
        </div>

        <div v-if="rescheduled" class="preview-tomorrow">
          <span>Tomorrow</span><strong>Prepare customer preview</strong>
        </div>

        <div class="preview-suggestion">
          <RobotOutlined class="suggestion-icon" aria-hidden="true" />
          <div>
            <span>Nova suggests</span>
            <p>
              {{
                rescheduled
                  ? 'Customer preview moved to tomorrow.'
                  : 'Move customer preview to tomorrow?'
              }}
            </p>
          </div>
          <button class="preview-suggestion-button" type="button" @click="toggleSuggestion">
            {{ rescheduled ? 'Undo' : 'Try it' }}
          </button>
        </div>
      </div>
    </section>
    <p class="preview-hint">Try checking a task or accepting a suggestion.</p>
    <p class="landing-sr-only" role="status" aria-live="polite" aria-atomic="true">
      {{ announcement }}
    </p>
  </div>
</template>
