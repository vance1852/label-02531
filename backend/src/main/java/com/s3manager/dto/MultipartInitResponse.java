package com.s3manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MultipartInitResponse {
    private Long fileId;
    private String uploadId;
    private String storageKey;
}
