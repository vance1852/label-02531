import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFileList, deleteFile as apiDeleteFile } from '../api/file'

export const useFileStore = defineStore('file', () => {
  const files = ref([])
  const total = ref(0)
  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')

  async function fetchFiles() {
    loading.value = true
    try {
      const res = await getFileList({
        page: currentPage.value,
        size: pageSize.value,
        keyword: keyword.value || undefined
      })
      files.value = res.data.records
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function removeFile(id) {
    await apiDeleteFile(id)
    await fetchFiles()
  }

  return {
    files, total, loading, currentPage, pageSize, keyword,
    fetchFiles, removeFile
  }
})
