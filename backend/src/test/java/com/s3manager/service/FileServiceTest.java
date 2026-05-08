package com.s3manager.service;

import com.s3manager.common.BizException;
import com.s3manager.config.S3Config;
import com.s3manager.dto.FileInfoVO;
import com.s3manager.dto.MultipartCompleteRequest;
import com.s3manager.dto.MultipartInitRequest;
import com.s3manager.dto.MultipartInitResponse;
import com.s3manager.entity.FileInfo;
import com.s3manager.mapper.FileInfoMapper;
import com.s3manager.mapper.MultipartRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 单元测试")
class FileServiceTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private S3Config s3Config;

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private MultipartRecordMapper multipartRecordMapper;

    @InjectMocks
    private FileService fileService;

    private FileInfo createFileInfo(Long id, String uploadId, Integer status) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(id);
        fileInfo.setOriginalName("test.txt");
        fileInfo.setStorageKey("2026/05/08/abc123.txt");
        fileInfo.setContentType("text/plain");
        fileInfo.setFileSize(1024L);
        fileInfo.setBucketName("test-bucket");
        fileInfo.setUploadId(uploadId);
        fileInfo.setStatus(status);
        return fileInfo;
    }

    @Nested
    @DisplayName("upload() 普通上传测试")
    class UploadTests {

        @Test
        @DisplayName("正常上传非空文件 - 验证 S3 上传和状态更新")
        void upload_ShouldUpdateStatusToCompleted_WhenUploadSucceeds() throws IOException {
            MultipartFile multipartFile = mock(MultipartFile.class);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(multipartFile.getContentType()).thenReturn("text/plain");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

            when(s3Config.getBucketName()).thenReturn("test-bucket");
            doNothing().when(s3Service).uploadFile(anyString(), any(), anyLong(), anyString());
            when(fileInfoMapper.insert(any(FileInfo.class))).thenAnswer(invocation -> {
                FileInfo fileInfo = invocation.getArgument(0);
                fileInfo.setId(1L);
                return 1;
            });

            FileInfoVO result = fileService.upload(multipartFile);

            assertNotNull(result);
            assertEquals(1L, result.getId());

            verify(fileInfoMapper).insert(any(FileInfo.class));

            ArgumentCaptor<FileInfo> updateCaptor = ArgumentCaptor.forClass(FileInfo.class);
            verify(fileInfoMapper).updateById(updateCaptor.capture());
            assertEquals(1, updateCaptor.getValue().getStatus());

            verify(s3Service).uploadFile(anyString(), any(), eq(1024L), eq("text/plain"));
        }

        @Test
        @DisplayName("上传空文件 - 抛出 400 BizException")
        void upload_ShouldThrowBizException_WhenFileIsEmpty() {
            MultipartFile multipartFile = mock(MultipartFile.class);
            when(multipartFile.isEmpty()).thenReturn(true);

            BizException exception = assertThrows(BizException.class, () -> fileService.upload(multipartFile));

            assertEquals(400, exception.getCode());
            assertEquals("上传文件不能为空", exception.getMessage());
            verify(fileInfoMapper, never()).insert(any());
            verify(s3Service, never()).uploadFile(anyString(), any(), anyLong(), anyString());
            verify(fileInfoMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("S3 上传失败 - 不更新文件状态")
        void upload_ShouldNotUpdateStatus_WhenS3UploadFails() throws IOException {
            MultipartFile multipartFile = mock(MultipartFile.class);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(multipartFile.getContentType()).thenReturn("text/plain");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

            when(s3Config.getBucketName()).thenReturn("test-bucket");
            when(fileInfoMapper.insert(any(FileInfo.class))).thenAnswer(invocation -> {
                FileInfo fi = invocation.getArgument(0);
                fi.setId(1L);
                return 1;
            });
            doThrow(new RuntimeException("S3 upload failed")).when(s3Service).uploadFile(anyString(), any(), anyLong(), anyString());

            RuntimeException exception = assertThrows(RuntimeException.class, () -> fileService.upload(multipartFile));

            assertTrue(exception.getMessage().contains("S3 upload failed"));
            verify(s3Service).uploadFile(anyString(), any(), anyLong(), anyString());
            verify(fileInfoMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("getDownloadUrl() 下载 URL 测试")
    class GetDownloadUrlTests {

        @Test
        @DisplayName("文件不存在时 - 抛出 404 BizException")
        void getDownloadUrl_ShouldThrow404_WhenFileNotExists() {
            when(fileInfoMapper.selectById(1L)).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> fileService.getDownloadUrl(1L));

            assertEquals(404, exception.getCode());
            assertEquals("文件不存在", exception.getMessage());
        }

        @Test
        @DisplayName("文件已删除 (status=2) - 抛出 404 BizException")
        void getDownloadUrl_ShouldThrow404_WhenFileIsDeleted() {
            FileInfo deletedFile = createFileInfo(1L, null, 2);
            when(fileInfoMapper.selectById(1L)).thenReturn(deletedFile);

            BizException exception = assertThrows(BizException.class, () -> fileService.getDownloadUrl(1L));

            assertEquals(404, exception.getCode());
            assertEquals("文件不存在", exception.getMessage());
        }

        @Test
        @DisplayName("文件存在时 - 返回 S3 预签名 URL")
        void getDownloadUrl_ShouldReturnPresignedUrl_WhenFileExists() {
            FileInfo existingFile = createFileInfo(1L, null, 1);
            when(fileInfoMapper.selectById(1L)).thenReturn(existingFile);
            when(s3Service.generatePresignedDownloadUrl(anyString(), anyString(), anyString()))
                    .thenReturn("https://s3.example.com/presigned-url");

            String result = fileService.getDownloadUrl(1L);

            assertEquals("https://s3.example.com/presigned-url", result);
            verify(s3Service).generatePresignedDownloadUrl(
                    eq("test-bucket"),
                    eq("2026/05/08/abc123.txt"),
                    eq("test.txt")
            );
        }
    }

    @Nested
    @DisplayName("deleteFile() 删除文件测试")
    class DeleteFileTests {

        @Test
        @DisplayName("正常删除 - 调用 S3 删除并更新状态为 2")
        void deleteFile_ShouldCallS3DeleteAndUpdateStatus() {
            FileInfo existingFile = createFileInfo(1L, null, 1);
            when(fileInfoMapper.selectById(1L)).thenReturn(existingFile);
            doNothing().when(s3Service).deleteFile(anyString(), anyString());

            fileService.deleteFile(1L);

            verify(s3Service).deleteFile(eq("test-bucket"), eq("2026/05/08/abc123.txt"));
            ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
            verify(fileInfoMapper).updateById(captor.capture());
            assertEquals(2, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("文件不存在时 - 抛出 404 BizException")
        void deleteFile_ShouldThrow404_WhenFileNotExists() {
            when(fileInfoMapper.selectById(1L)).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> fileService.deleteFile(1L));

            assertEquals(404, exception.getCode());
            assertEquals("文件不存在", exception.getMessage());
            verify(s3Service, never()).deleteFile(anyString(), anyString());
            verify(fileInfoMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("initMultipartUpload() 分片上传初始化测试")
    class InitMultipartUploadTests {

        @Test
        @DisplayName("正常初始化 - 返回包含 fileId 和 uploadId 的响应")
        void initMultipartUpload_ShouldReturnResponseWithFileIdAndUploadId() {
            MultipartInitRequest request = new MultipartInitRequest();
            request.setFileName("large-file.zip");
            request.setContentType("application/zip");
            request.setFileSize(10485760L);
            request.setTotalParts(10);

            when(s3Config.getBucketName()).thenReturn("test-bucket");
            when(s3Service.initiateMultipartUpload(anyString(), anyString())).thenReturn("upload-123-abc");
            when(fileInfoMapper.insert(any(FileInfo.class))).thenAnswer(invocation -> {
                FileInfo fi = invocation.getArgument(0);
                fi.setId(100L);
                return 1;
            });

            MultipartInitResponse response = fileService.initMultipartUpload(request);

            assertNotNull(response);
            assertEquals(100L, response.getFileId());
            assertEquals("upload-123-abc", response.getUploadId());
            assertNotNull(response.getStorageKey());
        }
    }

    @Nested
    @DisplayName("分片上传操作 - uploadId 为 null 时的异常测试")
    class MultipartUploadWithNullUploadIdTests {

        private FileInfo normalFile;
        private FileInfo multipartFile;

        @BeforeEach
        void setUp() {
            normalFile = createFileInfo(1L, null, 1);
            multipartFile = createFileInfo(2L, "upload-123", 0);
        }

        @Test
        @DisplayName("getPresignedPartUrl - uploadId 为 null 时抛出 400")
        void getPresignedPartUrl_ShouldThrow400_WhenUploadIdIsNull() {
            when(fileInfoMapper.selectById(1L)).thenReturn(normalFile);

            BizException exception = assertThrows(BizException.class,
                    () -> fileService.getPresignedPartUrl(1L, 1));

            assertEquals(400, exception.getCode());
            assertEquals("该文件不是分片上传类型", exception.getMessage());
            verify(s3Service, never()).generatePresignedUploadPartUrl(anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("completeMultipartUpload - uploadId 为 null 时抛出 400")
        void completeMultipartUpload_ShouldThrow400_WhenUploadIdIsNull() {
            when(fileInfoMapper.selectById(1L)).thenReturn(normalFile);

            MultipartCompleteRequest request = new MultipartCompleteRequest();
            request.setFileId(1L);
            request.setParts(List.of());

            BizException exception = assertThrows(BizException.class,
                    () -> fileService.completeMultipartUpload(request));

            assertEquals(400, exception.getCode());
            assertEquals("该文件不是分片上传类型", exception.getMessage());
            verify(s3Service, never()).completeMultipartUpload(anyString(), anyString(), any());
            verify(fileInfoMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("abortMultipartUpload - uploadId 为 null 时抛出 400")
        void abortMultipartUpload_ShouldThrow400_WhenUploadIdIsNull() {
            when(fileInfoMapper.selectById(1L)).thenReturn(normalFile);

            BizException exception = assertThrows(BizException.class,
                    () -> fileService.abortMultipartUpload(1L));

            assertEquals(400, exception.getCode());
            assertEquals("该文件不是分片上传类型", exception.getMessage());
            verify(s3Service, never()).abortMultipartUpload(anyString(), anyString());
            verify(fileInfoMapper, never()).updateById(any());
        }
    }
}
