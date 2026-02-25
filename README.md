## How to Run

### 前置条件
- Docker & Docker Compose

### 配置环境变量

项目根目录的 `.env` 文件用于配置 S3 存储。支持两种模式：

#### 模式一：本地 MinIO（默认，无需额外配置）

项目已内置 MinIO 服务，开箱即用。默认 `.env` 如下：

```env
AWS_REGION=us-east-1
AWS_S3_BUCKET=my-file-bucket
AWS_ACCESS_KEY=minioadmin
AWS_SECRET_KEY=minioadmin
AWS_S3_ENDPOINT=http://minio:9000
```

> MinIO 控制台：http://localhost:9001（用户名/密码：minioadmin/minioadmin）
> 
> 分片上传通过 nginx 反向代理（端口 9002）访问 MinIO，自动添加 CORS 头（暴露 ETag），无需额外配置。

#### 模式二：真实 AWS S3

将 `.env` 修改为你的 AWS 凭证，并清空 `AWS_S3_ENDPOINT`：

```env
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-bucket-name
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_S3_ENDPOINT=
```

> 使用真实 AWS S3 时，需在 AWS 控制台为 Bucket 配置 CORS 策略（允许 PUT、暴露 ETag），否则大文件分片上传会失败。

### 启动项目

```bash
docker compose up --build -d
```

### 停止项目

```bash
docker compose down
```

## Services

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端管理界面 | http://localhost:8081 | Vue 3 + Ant Design Vue |
| 后端 API | http://localhost:8080 | Spring Boot 3 |
| MySQL | localhost:3306 | 数据库 |
| MinIO API | http://localhost:9000 | S3 兼容存储（本地模式） |
| MinIO 控制台 | http://localhost:9001 | 存储管理界面（本地模式） |
| MinIO 代理 | http://localhost:9002 | 预签名 URL 代理，自动添加 CORS 头 |

## 测试账号

本项目无需登录认证，直接访问即可使用。

## 运行测试

```bash
docker build -f backend/Dockerfile.test -t s3fm-test backend/
docker run --rm s3fm-test
```

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
- **存储**: MinIO（本地）/ AWS S3（生产）
- **部署**: Docker Compose
