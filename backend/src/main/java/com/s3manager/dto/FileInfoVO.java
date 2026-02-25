package com.s3manager.dto;

import com.s3manager.entity.FileInfo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileInfoVO {
    private Long id;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private Integer status;
    private LocalDateTime createdAt;

    public static FileInfoVO from(FileInfo entity) {
        FileInfoVO vo = new FileInfoVO();
        vo.setId(entity.getId());
        vo.setOriginalName(entity.getOriginalName());
        vo.setContentType(entity.getContentType());
        vo.setFileSize(entity.getFileSize());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
