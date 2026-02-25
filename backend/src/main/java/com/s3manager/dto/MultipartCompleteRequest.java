package com.s3manager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MultipartCompleteRequest {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    @NotEmpty(message = "分片列表不能为空")
    private List<PartInfo> parts;

    @Data
    public static class PartInfo {
        private Integer partNumber;
        private String etag;
    }
}
