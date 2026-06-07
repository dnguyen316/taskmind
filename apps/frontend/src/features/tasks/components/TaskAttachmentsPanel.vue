<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  InboxOutlined,
  PaperClipOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import {
  deleteTaskAttachment,
  downloadTaskAttachment,
  listTaskAttachments,
  uploadTaskAttachment,
} from '../api/attachmentsApi'
import type { MediaKind, TaskAttachment } from '../types'

const props = defineProps<{
  taskId: string
}>()

const MEDIA_KIND_OPTIONS: Array<{ label: string; value: MediaKind }> = [
  { label: 'Image', value: 'IMAGE' },
  { label: 'Document', value: 'DOCUMENT' },
  { label: 'Audio', value: 'AUDIO' },
  { label: 'Video', value: 'VIDEO' },
  { label: 'Other', value: 'OTHER' },
]

const attachments = ref<TaskAttachment[]>([])
const selectedFile = ref<File | null>(null)
const mediaKind = ref<MediaKind>('DOCUMENT')
const loading = ref(false)
const uploading = ref(false)
const deletingId = ref<string | null>(null)
const downloadingId = ref<string | null>(null)
const errorMessage = ref('')
const successMessage = ref('')

const canUpload = computed(() => Boolean(props.taskId && selectedFile.value && mediaKind.value))
const sortedAttachments = computed(() =>
  [...attachments.value].sort(
    (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt),
  ),
)

onMounted(() => {
  void refreshAttachments()
})

watch(
  () => props.taskId,
  () => {
    selectedFile.value = null
    void refreshAttachments()
  },
)

async function refreshAttachments() {
  if (!props.taskId) {
    attachments.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    attachments.value = await listTaskAttachments(props.taskId)
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load task attachments.'
  } finally {
    loading.value = false
  }
}

async function uploadSelectedFile() {
  if (!selectedFile.value) {
    errorMessage.value = 'Choose a file before uploading.'
    return
  }

  uploading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const uploaded = await uploadTaskAttachment(props.taskId, {
      file: selectedFile.value,
      mediaKind: mediaKind.value,
    })

    attachments.value = [
      uploaded,
      ...attachments.value.filter((attachment) => attachment.id !== uploaded.id),
    ]
    selectedFile.value = null
    resetFileInput()
    successMessage.value = 'Attachment uploaded.'
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to upload attachment.'
  } finally {
    uploading.value = false
  }
}

async function downloadAttachment(attachment: TaskAttachment) {
  downloadingId.value = attachment.id
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const download = await downloadTaskAttachment(props.taskId, attachment.id)
    const url = URL.createObjectURL(download.blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = download.fileName || attachment.fileName
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(url)
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to download attachment.'
  } finally {
    downloadingId.value = null
  }
}

async function removeAttachment(attachment: TaskAttachment) {
  deletingId.value = attachment.id
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await deleteTaskAttachment(props.taskId, attachment.id)
    attachments.value = attachments.value.filter((candidate) => candidate.id !== attachment.id)
    successMessage.value = 'Attachment deleted.'
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to delete attachment.'
  } finally {
    deletingId.value = null
  }
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
}

function resetFileInput() {
  const input = document.getElementById('task-attachment-file-input') as HTMLInputElement | null

  if (input) {
    input.value = ''
  }
}

function formatBytes(sizeBytes: number) {
  if (!Number.isFinite(sizeBytes) || sizeBytes <= 0) {
    return '0 B'
  }

  const units = ['B', 'KB', 'MB', 'GB']
  const exponent = Math.min(Math.floor(Math.log(sizeBytes) / Math.log(1024)), units.length - 1)
  const value = sizeBytes / 1024 ** exponent
  return `${value.toFixed(value >= 10 || exponent === 0 ? 0 : 1)} ${units[exponent]}`
}

function formatDate(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function toTimestamp(value: string) {
  const timestamp = new Date(value).getTime()
  return Number.isFinite(timestamp) ? timestamp : 0
}
</script>

<template>
  <a-card class="task-attachments-panel" title="Attachments">
    <template #extra>
      <a-button
        type="text"
        :loading="loading"
        title="Refresh attachments"
        @click="refreshAttachments"
      >
        <template #icon><ReloadOutlined /></template>
      </a-button>
    </template>

    <a-space direction="vertical" size="middle" style="width: 100%">
      <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
      <a-alert v-else-if="successMessage" type="success" show-icon :message="successMessage" />

      <div class="upload-row">
        <label class="file-picker" for="task-attachment-file-input">
          <InboxOutlined />
          <span>{{ selectedFile?.name ?? 'Choose file' }}</span>
          <input id="task-attachment-file-input" type="file" @change="handleFileChange" />
        </label>

        <a-select
          v-model:value="mediaKind"
          class="media-kind-select"
          aria-label="Attachment media kind"
        >
          <a-select-option
            v-for="option in MEDIA_KIND_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>

        <a-button
          type="primary"
          :loading="uploading"
          :disabled="!canUpload"
          @click="uploadSelectedFile"
        >
          Upload
        </a-button>
      </div>

      <a-spin v-if="loading" tip="Loading attachments..." />

      <a-empty v-else-if="sortedAttachments.length === 0" description="No attachments yet." />

      <a-list v-else item-layout="horizontal" :data-source="sortedAttachments">
        <template #renderItem="{ item }">
          <a-list-item :key="item.id">
            <template #actions>
              <a-button
                type="link"
                :loading="downloadingId === item.id"
                @click="downloadAttachment(item)"
              >
                <template #icon><DownloadOutlined /></template>
                Download
              </a-button>
              <a-popconfirm
                title="Delete this attachment?"
                ok-text="Delete"
                cancel-text="Cancel"
                @confirm="removeAttachment(item)"
              >
                <a-button type="link" danger :loading="deletingId === item.id">
                  <template #icon><DeleteOutlined /></template>
                  Delete
                </a-button>
              </a-popconfirm>
            </template>

            <a-list-item-meta>
              <template #avatar>
                <a-avatar class="attachment-avatar"><PaperClipOutlined /></a-avatar>
              </template>
              <template #title>
                <span class="attachment-title">{{ item.fileName }}</span>
              </template>
              <template #description>
                <span>{{ item.mediaKind }}</span>
                <span> · {{ item.contentType }}</span>
                <span> · {{ formatBytes(item.sizeBytes) }}</span>
                <span> · Uploaded {{ formatDate(item.createdAt) }}</span>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-space>
  </a-card>
</template>

<style scoped>
.task-attachments-panel {
  border-radius: 18px;
}

.upload-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.file-picker {
  display: inline-flex;
  flex: 1 1 260px;
  gap: 10px;
  align-items: center;
  min-width: 220px;
  padding: 8px 12px;
  color: var(--tm-text-muted);
  cursor: pointer;
  border: 1px dashed var(--tm-border);
  border-radius: 10px;
}

.file-picker input {
  display: none;
}

.file-picker span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-kind-select {
  width: 150px;
}

.attachment-avatar {
  color: var(--tm-accent-blue-strong);
  background: var(--tm-primary-soft);
}

.attachment-title {
  font-weight: 600;
}
</style>
