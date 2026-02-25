package com.s3manager.dto;

import com.s3manager.entity.FileInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO 转换测试")
class DtoTest {

    @Test
    @DisplayName("FileInfoVO.from() - 应正确映射所有字段")
    void fileInfoVO_from_shouldMapAllFields() {
        FileInfo entity = new FileInfo();
        entity.setId(1L);
        entity.setOriginalName("test.pdf");
        entity.setContentType("application/pdf");
        entity.setFileSize(2048L);
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.of(2026, 2, 25, 10, 30, 0));

        FileInfoVO vo = FileInfoVO.from(entity);

        assertEquals(1L, vo.getId());
        assertEquals("test.pdf", vo.getOriginalName());
        assertEquals("application/pdf", vo.getContentType());
        assertEquals(2048L, vo.getFileSize());
        assertEquals(1, vo.getStatus());
        assertEquals(LocalDateTime.of(2026, 2, 25, 10, 30, 0), vo.getCreatedAt());
    }

    @Test
    @DisplayName("MultipartInitResponse.builder() - 应正确构建")
    void multipartInitResponse_builder() {
        MultipartInitResponse resp = MultipartInitResponse.builder()
                .fileId(10L)
                .uploadId("upload-abc")
                .storageKey("2026/02/25/key.zip")
                .build();

        assertEquals(10L, resp.getFileId());
        assertEquals("upload-abc", resp.getUploadId());
        assertEquals("2026/02/25/key.zip", resp.getStorageKey());
    }

    @Test
    @DisplayName("MultipartCompleteRequest.PartInfo - getter/setter")
    void partInfo_getterSetter() {
        MultipartCompleteRequest.PartInfo part = new MultipartCompleteRequest.PartInfo();
        part.setPartNumber(3);
        part.setEtag("\"abc123\"");

        assertEquals(3, part.getPartNumber());
        assertEquals("\"abc123\"", part.getEtag());
    }
}
