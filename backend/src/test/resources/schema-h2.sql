DROP TABLE IF EXISTS multipart_record;
DROP TABLE IF EXISTS file_info;

CREATE TABLE file_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name   VARCHAR(500)  NOT NULL,
    storage_key     VARCHAR(1000) NOT NULL,
    content_type    VARCHAR(200)  DEFAULT 'application/octet-stream',
    file_size       BIGINT        DEFAULT 0,
    bucket_name     VARCHAR(200)  NOT NULL,
    upload_id       VARCHAR(500)  DEFAULT NULL,
    status          TINYINT       DEFAULT 0,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE multipart_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id         BIGINT        NOT NULL,
    part_number     INT           NOT NULL,
    etag            VARCHAR(500)  DEFAULT NULL,
    part_size       BIGINT        DEFAULT 0,
    status          TINYINT       DEFAULT 0,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_id FOREIGN KEY (file_id) REFERENCES file_info(id) ON DELETE CASCADE
);
