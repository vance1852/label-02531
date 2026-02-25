## How to Run

### 前置条件
- Docker & Docker Compose
- AWS S3 Bucket（或兼容 S3 协议的对象存储，如 MinIO）

### 配置 AWS 凭证

在项目根目录创建 `.env` 文件：

```env
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-bucket-name
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_S3_ENDPOINT=
```

> 如使用 MinIO，设置 `AWS_S3_ENDPOINT=http://minio:9000`

### 启动项目

```bash
docker-compose up --build -d
```

### 停止项目

```bash
docker-compose down
```

## Services

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端管理界面 | http://localhost:8081 | Vue 3 + Ant Design Vue |
| 后端 API | http://localhost:8080 | Spring Boot 3 |
| MySQL | localhost:3306 | 数据库 |

## 测试账号

本项目无需登录认证，直接访问即可使用。

## 题目内容

创建一个 Spring 集成 AWS S3 使用文件上传、下载、删除、大文件上传功能的项目。

### 功能清单

- **普通文件上传**：支持拖拽上传，实时进度显示，最大 100MB
- **大文件分片上传**：自动分片（10MB/片），预签名 URL 直传 S3，支持并发上传（3 并发），可取消
- **文件下载**：通过 S3 预签名 URL 下载
- **文件删除**：软删除，同时清理 S3 存储
- **文件列表**：分页查询，关键字搜索

### 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + MyBatis-Plus + AWS SDK v2
- **前端**: Vue 3 + Vite + Ant Design Vue + Pinia + Axios
- **数据库**: MySQL 8.0
- **部署**: Docker Compose
