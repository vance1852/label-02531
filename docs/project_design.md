# AWS S3 文件管理系统 - 项目设计文档

## 1. 系统架构

```mermaid
flowchart TD
    subgraph Frontend["前端 (Vue 3 + Ant Design Vue)"]
        A[文件列表页] --> B[上传组件]
        A --> C[下载/删除操作]
        B --> D[普通上传]
        B --> E[大文件分片上传]
    end

    subgraph Backend["后端 (Spring Boot 3)"]
        F[FileController] --> G[FileService]
        G --> H[S3Service]
        H --> I[AWS S3 SDK]
    end

    subgraph Storage["存储层"]
        J[(MySQL - 文件元数据)]
        K[(AWS S3 - 文件存储)]
    end

    Frontend -->|HTTP/REST| Backend
    G --> J
    H --> K
```

## 2. ER 图

```mermaid
erDiagram
    FILE_INFO {
        bigint id PK "主键"
        varchar original_name "原始文件名"
        varchar storage_key "S3存储Key"
        varchar content_type "文件MIME类型"
        bigint file_size "文件大小(bytes)"
        varchar bucket_name "S3桶名"
        varchar upload_id "分片上传ID"
        int status "状态: 0-上传中 1-已完成 2-已删除"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
    }

    MULTIPART_RECORD {
        bigint id PK "主键"
        bigint file_id FK "关联文件ID"
        int part_number "分片序号"
        varchar etag "分片ETag"
        bigint part_size "分片大小"
        int status "状态: 0-上传中 1-已完成"
        datetime created_at "创建时间"
    }

    FILE_INFO ||--o{ MULTIPART_RECORD : "has parts"
```

## 3. 接口清单

### FileController (`/api/files`)

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/files/upload` | 普通文件上传 |
| GET | `/api/files` | 文件列表查询(分页) |
| GET | `/api/files/download/{id}` | 文件下载 |
| DELETE | `/api/files/{id}` | 文件删除 |
| POST | `/api/files/multipart/init` | 初始化分片上传 |
| GET | `/api/files/multipart/presign` | 获取分片预签名URL |
| POST | `/api/files/multipart/complete` | 完成分片上传 |
| POST | `/api/files/multipart/abort` | 取消分片上传 |

## 4. UI/UX 规范

- 主色调: `#1890ff` (Ant Design 蓝)
- 背景色: `#f0f2f5`
- 卡片圆角: `8px`
- 卡片阴影: `0 2px 8px rgba(0,0,0,0.09)`
- 间距体系: `8px / 16px / 24px`
- 字体: `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto`
- 标题字号: `20px` / 正文: `14px` / 辅助: `12px`
