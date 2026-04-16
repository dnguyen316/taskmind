<script setup lang="ts">
import { Alert, Button, Result } from 'ant-design-vue'
import { onErrorCaptured, ref } from 'vue'

const hasError = ref(false)
const errorMessage = ref('')

onErrorCaptured((error, instance, info) => {
  hasError.value = true
  errorMessage.value = error instanceof Error ? error.message : 'Unexpected rendering error.'

  console.error('Captured UI error in AppErrorBoundary.', {
    error,
    info,
    component: instance?.type,
  })

  return false
})

function resetBoundary() {
  hasError.value = false
  errorMessage.value = ''
}
</script>

<template>
  <Result
    v-if="hasError"
    status="error"
    title="Something went wrong"
    sub-title="An unexpected UI error occurred. You can retry or refresh the page."
  >
    <template #extra>
      <Button type="primary" @click="resetBoundary">Try again</Button>
    </template>

    <Alert
      v-if="errorMessage"
      style="margin-top: 16px"
      type="error"
      :message="errorMessage"
      show-icon
    />
  </Result>

  <slot v-else />
</template>
