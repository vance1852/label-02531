CREATE DATABASE IF NOT EXISTS s3_file_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE s3_file_manager;

DROP TABLE IF EXISTS multipart_record;
DROP TABLE IF EXISTS file_info;

CREATE TABLE file_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name   VARCHAR(500)  NOT NULL COMMENT '原始文件名',
    storage_key     VARCHAR(1000) NOT NULL COMMENT 'S3存储Key',
    content_type    VARCHAR(200)  DEFAULT 'application/octet-stream' COMMENT 'MIME类型',
    file_size       BIGINT        DEFAULT 0 COMMENT '文件大小(bytes)',
    bucket_name     VARCHAR(200)  NOT NULL COMMENT 'S3桶名',
    upload_id       VARCHAR(500)  DEFAULT NULL COMMENT '分片上传ID',
    status          TINYINT       DEFAULT 0 COMMENT '0-上传中 1-已完成 2-已删除',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

CREATE TABLE multipart_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id         BIGINT        NOT NULL COMMENT '关联文件ID',
    part_number     INT           NOT NULL COMMENT '分片序号',
    etag            VARCHAR(500)  DEFAULT NULL COMMENT '分片ETag',
    part_size       BIGINT        DEFAULT 0 COMMENT '分片大小',
    status          TINYINT       DEFAULT 0 COMMENT '0-上传中 1-已完成',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id),
    CONSTRAINT fk_file_id FOREIGN KEY (file_id) REFERENCES file_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分片上传记录表';
