<template>
  <a-modal
    :open="open"
    title="文件上传"
    :footer="null"
    :mask-closable="false"
    @cancel="handleClose"
    width="520px"
  >
    <div class="upload-area">
      <a-upload-dragger
        :before-upload="beforeUpload"
        :show-upload-list="false"
        :disabled="uploading"
      >
        <p class="ant-upload-drag-icon">
          <inbox-outlined />
        </p>
        <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
        <p class="ant-upload-hint">支持单个文件上传，最大 100MB</p>
      </a-upload-dragger>

      <div v-if="currentFile" class="upload-progress">
        <div class="file-info-row">
          <span class="file-label">{{ currentFile.name }}</span>
          <span class="file-size-label">{{ formatSize(currentFile.size) }}</span>
        </div>
        <a-progress :percent="uploadPercent" :status="uploadStatus" />
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { uploadFile } from '../api/file'

const props = defineProps({ open: Boolean })
const emit = defineEmits(['update:open', 'success'])

const uploading = ref(false)
const currentFile = ref(null)
const uploadPercent = ref(0)
const uploadStatus = ref('active')

function beforeUpload(file) {
  currentFile.value = file
  uploadPercent.value = 0
  uploadStatus.value = 'active'
  doUpload(file)
  return false
}

async function doUpload(file) {
  uploading.value = true
  try {
    await uploadFile(file, (e) => {
      if (e.total > 0) {
        uploadPercent.value = Math.round((e.loaded / e.total) * 100)
      }
    })
    uploadStatus.value = 'success'
    message.success('上传成功')
    emit('success')
    setTimeout(() => handleClose(), 800)
  } catch {
    uploadStatus.value = 'exception'
    message.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function handleClose() {
  if (!uploading.value) {
    currentFile.value = null
    uploadPercent.value = 0
    emit('update:open', false)
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}
</script>

<style scoped lang="scss">
.upload-area {
  padding: 8px 0;
}

.upload-progress {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.file-info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;

  .file-label {
    font-weight: 500;
    color: #333;
    max-width: 320px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .file-size-label {
    color: #8c8c8c;
    font-size: 12px;
  }
}
</style>
