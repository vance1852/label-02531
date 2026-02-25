<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1>S3 文件管理系统</h1>
        <div class="subtitle">Spring Boot + AWS S3 · 文件上传 / 下载 / 删除 / 大文件分片上传</div>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <a-space>
          <a-input-search
            v-model:value="fileStore.keyword"
            placeholder="搜索文件名..."
            style="width: 280px"
            allow-clear
            @search="fileStore.fetchFiles()"
          />
        </a-space>
        <a-space>
          <a-button type="primary" @click="showUploadModal = true">
            <template #icon><upload-outlined /></template>
            普通上传
          </a-button>
          <a-button @click="showMultipartModal = true">
            <template #icon><cloud-upload-outlined /></template>
            大文件上传
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="fileStore.files"
        :loading="fileStore.loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'originalName'">
            <a-tooltip :title="record.originalName">
              <span class="file-name">{{ record.originalName }}</span>
            </a-tooltip>
          </template>
          <template v-if="column.key === 'fileSize'">
            <span class="file-size">{{ formatSize(record.fileSize) }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">
              {{ statusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                :disabled="record.status !== 1"
                :loading="downloadingId === record.id"
                @click="handleDownload(record)"
              >
                下载
              </a-button>
              <a-popconfirm
                title="确定删除该文件？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 普通上传弹窗 -->
    <UploadModal v-model:open="showUploadModal" @success="fileStore.fetchFiles()" />

    <!-- 大文件分片上传弹窗 -->
    <MultipartUploadModal v-model:open="showMultipartModal" @success="fileStore.fetchFiles()" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, CloudUploadOutlined } from '@ant-design/icons-vue'
import { useFileStore } from '../stores/file'
import { getDownloadUrl } from '../api/file'
import UploadModal from '../components/UploadModal.vue'
import MultipartUploadModal from '../components/MultipartUploadModal.vue'

const fileStore = useFileStore()
const showUploadModal = ref(false)
const showMultipartModal = ref(false)
const downloadingId = ref(null)

const columns = [
  { title: '文件名', key: 'originalName', dataIndex: 'originalName', ellipsis: true },
  { title: '类型', dataIndex: 'contentType', width: 160, ellipsis: true },
  { title: '大小', key: 'fileSize', dataIndex: 'fileSize', width: 120 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '上传时间', key: 'createdAt', dataIndex: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const pagination = computed(() => ({
  current: fileStore.currentPage,
  pageSize: fileStore.pageSize,
  total: fileStore.total,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 个文件`
}))

function handleTableChange(pag) {
  fileStore.currentPage = pag.current
  fileStore.pageSize = pag.pageSize
  fileStore.fetchFiles()
}

async function handleDownload(record) {
  downloadingId.value = record.id
  try {
    const res = await getDownloadUrl(record.id)
    window.open(res.data, '_blank')
  } catch {
    message.error('获取下载链接失败')
  } finally {
    downloadingId.value = null
  }
}

async function handleDelete(id) {
  try {
    await fileStore.removeFile(id)
    message.success('删除成功')
  } catch {
    message.error('删除失败')
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}

function statusColor(status) {
  return { 0: 'orange', 1: 'green', 2: 'red' }[status] || 'default'
}

function statusText(status) {
  return { 0: '上传中', 1: '已完成', 2: '已删除' }[status] || '未知'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fileStore.fetchFiles()
})
</script>

<style scoped lang="scss">
.file-name {
  max-width: 300px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>
