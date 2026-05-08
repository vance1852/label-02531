package com.s3manager.service;

import com.s3manager.common.BizException;
import com.s3manager.config.S3Config;
import com.s3manager.dto.*;
import com.s3manager.entity.FileInfo;
import com.s3manager.mapper.FileInfoMapper;
import com.s3manager.mapper.MultipartRecordMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockFile = mock(MultipartFile.class);
        lenient().when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        lenient().when(mockFile.getContentType()).thenReturn("text/plain");
        lenient().when(mockFile.getSize()).thenReturn(1024L);
        lenient().when(s3Config.getBucketName()).thenReturn("test-bucket");
    }

    @Nested
    @DisplayName("upload - 普通文件上传")
    class UploadTests {

        @Test
        @DisplayName("正常上传非空文件，验证 S3Service.uploadFile 被调用且状态更新为已完成")
        void uploadNormalFile_shouldCallS3AndUpdateStatus() throws IOException {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("hello".getBytes()));
            when(fileInfoMapper.insert(any(FileInfo.class))).thenAnswer(invocation -> {
                FileInfo fi = invocation.getArgument(0);
                fi.setId(1L);
                return 1;
            });

            FileInfoVO result = fileService.upload(mockFile);

            assertThat(result).isNotNull();
            verify(s3Service).uploadFile(anyString(), any(), eq(1024L), eq("text/plain"));
            ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
            verify(fileInfoMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("上传空文件抛 BizException")
        void uploadEmptyFile_shouldThrowBizException() {
            when(mockFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> fileService.upload(mockFile))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> {
                        BizException biz = (BizException) ex;
                        assertThat(biz.getCode()).isEqualTo(400);
                    });

            verify(s3Service, never()).uploadFile(anyString(), any(), anyLong(), anyString());
            verify(fileInfoMapper, never()).insert(any(FileInfo.class));
        }

        @Test
        @DisplayName("S3Service.uploadFile 抛异常时，FileInfoMapper.updateById 不会被调用")
        void uploadWhenS3Fails_shouldNotUpdateStatus() throws IOException {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("hello".getBytes()));
            when(fileInfoMapper.insert(any(FileInfo.class))).thenAnswer(invocation -> {
                FileInfo fi = invocation.getArgument(0);
                fi.setId(1L);
                return 1;
            });
            doThrow(new RuntimeException("S3 error")).when(s3Service)
                    .uploadFile(anyString(), any(), anyLong(), anyString());

            assertThatThrownBy(() -> fileService.upload(mockFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("S3 error");

            verify(fileInfoMapper, never()).updateById(any(FileInfo.class));
        }
    }

    @Nested
    @DisplayName("getDownloadUrl - 获取下载预签名 URL")
    class GetDownloadUrlTests {

        @Test
        @DisplayName("文件不存在时抛 404 BizException")
        void getDownloadUrl_fileNotExist_shouldThrow404() {
            when(fileInfoMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> fileService.getDownloadUrl(999L))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(404));

            verify(s3Service, never()).generatePresignedDownloadUrl(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("文件 status=2 时抛 404 BizException")
        void getDownloadUrl_deletedFile_shouldThrow404() {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(1L);
            fileInfo.setStatus(2);
            fileInfo.setBucketName("test-bucket");
            fileInfo.setStorageKey("key");
            fileInfo.setOriginalName("deleted.txt");
            when(fileInfoMapper.selectById(1L)).thenReturn(fileInfo);

            assertThatThrownBy(() -> fileService.getDownloadUrl(1L))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(404));

            verify(s3Service, never()).generatePresignedDownloadUrl(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("文件存在时返回 S3 预签名 URL")
        void getDownloadUrl_fileExists_shouldReturnPresignedUrl() {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(1L);
            fileInfo.setStatus(1);
            fileInfo.setBucketName("test-bucket");
            fileInfo.setStorageKey("2024/01/01/abc.txt");
            fileInfo.setOriginalName("test.txt");
            when(fileInfoMapper.selectById(1L)).thenReturn(fileInfo);
            when(s3Service.generatePresignedDownloadUrl("test-bucket", "2024/01/01/abc.txt", "test.txt"))
                    .thenReturn("https://s3.example.com/presigned-url");

            String url = fileService.getDownloadUrl(1L);

            assertThat(url).isEqualTo("https://s3.example.com/presigned-url");
            verify(s3Service).generatePresignedDownloadUrl("test-bucket", "2024/01/01/abc.txt", "test.txt");
        }
    }

    @Nested
    @DisplayName("deleteFile - 删除文件")
    class DeleteFileTests {

        @Test
        @DisplayName("正常删除时调用 S3Service.deleteFile 并把状态更新为 2")
        void deleteFile_normal_shouldDeleteAndUpdateStatus() {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(1L);
            fileInfo.setStatus(1);
            fileInfo.setBucketName("test-bucket");
            fileInfo.setStorageKey("key");
            fileInfo.setOriginalName("test.txt");
            when(fileInfoMapper.selectById(1L)).thenReturn(fileInfo);

            fileService.deleteFile(1L);

            verify(s3Service).deleteFile("test-bucket", "key");
            ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
            verify(fileInfoMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(2);
        }

        @Test
        @DisplayName("文件不存在时抛 404 BizException")
        void deleteFile_notExist_shouldThrow404() {
            when(fileInfoMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> fileService.deleteFile(999L))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(404));

            verify(s3Service, never()).deleteFile(anyString(), anyString());
            verify(fileInfoMapper, never()).updateById(any(FileInfo.class));
        }
    }

    @Nested
    @DisplayName("initMultipartUpload - 初始化分片上传")
    class InitMultipartUploadTests {

        @Test
        @DisplayName("返回包含 fileId 和 uploadId 的响应")
        void initMultipartUpload_shouldReturnFileIdAndUploadId() {
            MultipartInitRequest request = new MultipartInitRequest();
            request.setFileName("bigfile.zip");
            request.setContentType("application/zip");
            request.setFileSize(100L * 1024 * 1024);
            request.setTotalParts(10);

            when(s3Service.initiateMultipartUpload(anyString(), eq("application/zip")))
                    .thenReturn("upload-123");
            when(fileInfoMapper.insert(any(FileInfo.class))).thenAnswer(invocation -> {
                FileInfo fi = invocation.getArgument(0);
                fi.setId(42L);
                return 1;
            });

            MultipartInitResponse response = fileService.initMultipartUpload(request);

            assertThat(response).isNotNull();
            assertThat(response.getFileId()).isEqualTo(42L);
            assertThat(response.getUploadId()).isEqualTo("upload-123");
            assertThat(response.getStorageKey()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("分片上传校验 - uploadId 为 null 时应抛 400")
    class MultipartValidationTests {

        private FileInfo normalFile;

        @BeforeEach
        void setUpNormalFile() {
            normalFile = new FileInfo();
            normalFile.setId(1L);
            normalFile.setStatus(0);
            normalFile.setBucketName("test-bucket");
            normalFile.setStorageKey("key");
            normalFile.setOriginalName("normal.txt");
            normalFile.setUploadId(null);
        }

        @Test
        @DisplayName("getPresignedPartUrl 对普通文件抛 400 BizException")
        void getPresignedPartUrl_normalFile_shouldThrow400() {
            when(fileInfoMapper.selectById(1L)).thenReturn(normalFile);

            assertThatThrownBy(() -> fileService.getPresignedPartUrl(1L, 1))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(400));

            verify(s3Service, never()).generatePresignedUploadPartUrl(anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("completeMultipartUpload 对普通文件抛 400 BizException")
        void completeMultipartUpload_normalFile_shouldThrow400() {
            when(fileInfoMapper.selectById(1L)).thenReturn(normalFile);

            MultipartCompleteRequest request = new MultipartCompleteRequest();
            request.setFileId(1L);
            MultipartCompleteRequest.PartInfo part = new MultipartCompleteRequest.PartInfo();
            part.setPartNumber(1);
            part.setEtag("etag1");
            request.setParts(List.of(part));

            assertThatThrownBy(() -> fileService.completeMultipartUpload(request))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(400));

            verify(s3Service, never()).completeMultipartUpload(anyString(), anyString(), anyList());
        }

        @Test
        @DisplayName("abortMultipartUpload 对普通文件抛 400 BizException")
        void abortMultipartUpload_normalFile_shouldThrow400() {
            when(fileInfoMapper.selectById(1L)).thenReturn(normalFile);

            assertThatThrownBy(() -> fileService.abortMultipartUpload(1L))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(400));

            verify(s3Service, never()).abortMultipartUpload(anyString(), anyString());
        }
    }
}
