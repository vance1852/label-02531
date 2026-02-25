import request from './request'

export function uploadFile(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function getFileList(params) {
  return request.get('/api/files', { params })
}

export function getDownloadUrl(id) {
  return request.get(`/api/files/download/${id}`)
}

export function deleteFile(id) {
  return request.delete(`/api/files/${id}`)
}

// 分片上传
export function initMultipartUpload(data) {
  return request.post('/api/files/multipart/init', data)
}

export function getPresignedPartUrl(fileId, partNumber) {
  return request.get('/api/files/multipart/presign', {
    params: { fileId, partNumber }
  })
}

export function completeMultipartUpload(data) {
  return request.post('/api/files/multipart/complete', data)
}

export function abortMultipartUpload(fileId) {
  return request.post('/api/files/multipart/abort', null, {
    params: { fileId }
  })
}
