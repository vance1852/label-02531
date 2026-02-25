package com.s3manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * 应用启动后检查 Bucket 是否存在
 * CORS 由 minio-proxy (nginx) 处理，无需在此配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3BucketInitializer {

    private final S3Client s3Client;
    private final S3Config s3Config;

    @EventListener(ApplicationReadyEvent.class)
    public void checkBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .build());
            log.info("Bucket verified: {}", s3Config.getBucketName());
        } catch (Exception e) {
            log.warn("Bucket check failed ({}): {}", s3Config.getBucketName(), e.getMessage());
        }
    }
}
