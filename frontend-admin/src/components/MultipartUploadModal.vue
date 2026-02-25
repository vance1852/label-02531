<template>
  <a-modal
    :open="open"
    title="大文件分片上传"
    :footer="null"
    :mask-closable="false"
    @cancel="handleClose"
    width="600px"
  >
    <div class="multipart-upload">
      <!-- 选择文件 -->
      <div v-if="!selectedFile" class="select-area">
        <a-upload-dragger :before-upload="onSelectFile" :show-upload-list="false">
          <p class="ant-upload-drag-icon">
            <cloud-upload-outlined />
          </p>
          <p class="ant-upload-text">选择大文件进行分片上传</p>
          <p class="ant-upload-hint">文件将被自动分片，通过预签名URL直传S3</p>
        </a-upload-dragger>
      </div>

      <!-- 上传进度 -->
      <div v-else class="progress-area">
        <div class="file-summary card-inner">
          <div class="summary-row">
            <span class="label">文件名</span>
            <span class="value">{{ selectedFile.name }}</span>
          </div>
          <div class="summary-row">
            <span class="label">文件大小</span>
            <span class="value">{{ formatSize(selectedFile.size) }}</span>
          </div>
          <div class="summary-row">
            <span class="label">分片数量</span>
            <span class="value">{{ totalParts }} 片 (每片 {{ formatSize(PART_SIZE) }})</span>
          </div>
        </div>

        <div class="overall-progress">
          <div class="progress-header">
            <span>总进度</span>
            <span>{{ completedParts }} / {{ totalParts }}</span>
          </div>
          <a-progress
            :percent="overallPercent"
            :status="overallStatus"
            :stroke-color="{ '0%': '#1890ff', '100%': '#52c41a' }"
          />
        </div>

        <div class="part-list" v-if="parts.length > 0">
          <div class="part-item" v-for="part in visibleParts" :key="part.number">
            <span class="part-label">分片 {{ part.number }}</span>
            <a-progress
              :percent="part.percent"
              :status="part.status"
              size="small"
              style="flex: 1; margin: 0 12px"
            />
            <span class="part-size">{{ formatSize(part.size) }}</span>
          </div>
          <div v-if="parts.length > 5" class="more-hint">
            ... 共 {{ parts.length }} 个分片
          </div>
        </div>

        <div class="action-bar">
          <a-button
            v-if="!uploading && overallStatus !== 'success'"
            type="primary"
            @click="startUpload"
            :loading="initializing"
          >
            开始上传
          </a-button>
          <a-button
            v-if="uploading"
            danger
            @click="handleAbort"
          >
            取消上传
          </a-button>
          <a-button v-if="overallStatus === 'success'" @click="handleClose">
            完成
          </a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { CloudUploadOutlined } from '@ant-design/icons-vue'
import axios from 'axios'
import {
  initMultipartUpload,
  getPresignedPartUrl,
  completeMultipartUpload,
  abortMultipartUpload
} from '../api/file'

const PART_SIZE = 10 * 1024 * 1024 // 10MB
const CONCURRENCY = 3

const props = defineProps({ open: Boolean })
const emit = defineEmits(['update:open', 'success'])

const selectedFile = ref(null)
const totalParts = ref(0)
const parts = ref([])
const uploading = ref(false)
const initializing = ref(false)
const aborted = ref(false)
const fileId = ref(null)

const completedParts = computed(() => parts.value.filter(p => p.status === 'success').length)
const overallPercent = computed(() =>
  totalParts.value > 0 ? Math.round((completedParts.value / totalParts.value) * 100) : 0
)
const overallStatus = computed(() => {
  if (parts.value.some(p => p.status === 'exception')) return 'exception'
  if (completedParts.value === totalParts.value && totalParts.value > 0) return 'success'
  return 'active'
})
const visibleParts = computed(() => parts.value.slice(0, 5))

function onSelectFile(file) {
  selectedFile.value = file
  totalParts.value = Math.ceil(file.size / PART_SIZE)
  parts.value = Array.from({ length: totalParts.value }, (_, i) => ({
    number: i + 1,
    percent: 0,
    status: 'normal',
    etag: null,
    size: Math.min(PART_SIZE, file.size - i * PART_SIZE)
  }))
  return false
}

async function startUpload() {
  initializing.value = true
  aborted.value = false
  try {
    const res = await initMultipartUpload({
      fileName: selectedFile.value.name,
      contentType: selectedFile.value.type || 'application/octet-stream',
      fileSize: selectedFile.value.size,
      totalParts: totalParts.value
    })
    fileId.value = res.data.fileId
    uploading.value = true
    initializing.value = false
    await uploadAllParts()
  } catch {
    message.error('初始化分片上传失败')
    initializing.value = false
  }
}

async function uploadAllParts() {
  const queue = [...parts.value]
  const workers = []

  for (let i = 0; i < CONCURRENCY; i++) {
    workers.push(processQueue(queue))
  }

  await Promise.all(workers)

  if (aborted.value) return

  if (parts.value.every(p => p.status === 'success')) {
    await doComplete()
  }
  uploading.value = false
}

async function processQueue(queue) {
  while (queue.length > 0 && !aborted.value) {
    const part = queue.shift()
    if (!part) break
    await uploadPart(part)
  }
}

async function uploadPart(part) {
  const start = (part.number - 1) * PART_SIZE
  const end = Math.min(start + PART_SIZE, selectedFile.value.size)
  const blob = selectedFile.value.slice(start, end)

  part.status = 'active'
  part.percent = 0

  try {
    const urlRes = await getPresignedPartUrl(fileId.value, part.number)
    const presignedUrl = urlRes.data

    const response = await axios.put(presignedUrl, blob, {
      headers: { 'Content-Type': 'application/octet-stream' },
      onUploadProgress: (e) => {
        if (e.total > 0) {
          part.percent = Math.round((e.loaded / e.total) * 100)
        }
      }
    })

    part.etag = response.headers.etag || response.headers.ETag
    part.status = 'success'
    part.percent = 100
  } catch (err) {
    part.status = 'exception'
    console.error(`Part ${part.number} upload failed:`, err)
  }
}

async function doComplete() {
  try {
    const partInfos = parts.value
      .filter(p => p.status === 'success')
      .map(p => ({ partNumber: p.number, etag: p.etag }))

    await completeMultipartUpload({
      fileId: fileId.value,
      parts: partInfos
    })
    message.success('大文件上传完成')
    emit('success')
  } catch {
    message.error('合并分片失败')
  }
}

async function handleAbort() {
  aborted.value = true
  uploading.value = false
  if (fileId.value) {
    try {
      await abortMultipartUpload(fileId.value)
      message.info('已取消上传')
    } catch {
      message.error('取消上传失败')
    }
  }
  resetState()
}

function handleClose() {
  if (!uploading.value) {
    resetState()
    emit('update:open', false)
  }
}

function resetState() {
  selectedFile.value = null
  totalParts.value = 0
  parts.value = []
  fileId.value = null
  aborted.value = false
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}
</script>

<style scoped lang="scss">
.multipart-upload {
  padding: 8px 0;
}

.card-inner {
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  padding: 16px;
  margin-bottom: 16px;
}

.file-summary {
  .summary-row {
    display: flex;
    justify-content: space-between;
    padding: 6px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child { border-bottom: none; }

    .label {
      color: #8c8c8c;
      font-size: 13px;
    }
    .value {
      color: #333;
      font-weight: 500;
      font-size: 13px;
      max-width: 360px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.overall-progress {
  margin-bottom: 16px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 13px;
    color: #595959;
  }
}

.part-list {
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;

  .part-item {
    display: flex;
    align-items: center;
    padding: 4px 0;

    .part-label {
      font-size: 12px;
      color: #8c8c8c;
      min-width: 56px;
    }
    .part-size {
      font-size: 12px;
      color: #bfbfbf;
      min-width: 60px;
      text-align: right;
    }
  }

  .more-hint {
    text-align: center;
    color: #bfbfbf;
    font-size: 12px;
    padding-top: 8px;
  }
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
