package com.s3manager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MultipartInitRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotBlank(message = "文件类型不能为空")
    private String contentType;

    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    @Min(value = 1, message = "分片数量必须大于0")
    private Integer totalParts;
}
