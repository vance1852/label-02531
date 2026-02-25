package com.s3manager.service;

import com.s3manager.config.S3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service 单元测试")
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Config s3Config;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        lenient().when(s3Config.getBucketName()).thenReturn("test-bucket");
        lenient().when(s3Config.getPresignExpirationMinutes()).thenReturn(60);
    }

    @Test
    @DisplayName("上传文件 - 应正确调用S3 putObject")
    void uploadFile_shouldCallPutObject() {
        byte[] content = "hello world".getBytes();
        var inputStream = new ByteArrayInputStream(content);

        s3Service.uploadFile("test/key.txt", inputStream, content.length, "text/plain");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertEquals("test-bucket", request.bucket());
        assertEquals("test/key.txt", request.key());
        assertEquals("text/plain", request.contentType());
        assertEquals(content.length, request.contentLength());
    }

    @Test
    @DisplayName("下载文件 - 应正确调用S3 getObject")
    @SuppressWarnings("unchecked")
    void downloadFile_shouldCallGetObject() {
        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);

        var result = s3Service.downloadFile("test-bucket", "test/key.txt");

        assertNotNull(result);
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("test/key.txt", captor.getValue().key());
    }

    @Test
    @DisplayName("删除文件 - 应正确调用S3 deleteObject")
    void deleteFile_shouldCallDeleteObject() {
        s3Service.deleteFile("test-bucket", "test/key.txt");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("test/key.txt", captor.getValue().key());
    }

    @Test
    @DisplayName("生成预签名下载URL - 应返回有效URL")
    void generatePresignedDownloadUrl_shouldReturnUrl() throws Exception {
        PresignedGetObjectRequest mockPresigned = mock(PresignedGetObjectRequest.class);
        when(mockPresigned.url()).thenReturn(new URL("https://s3.amazonaws.com/test-bucket/key.txt?signed=true"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresigned);

        String url = s3Service.generatePresignedDownloadUrl("test-bucket", "key.txt", "key.txt");

        assertNotNull(url);
        assertTrue(url.contains("test-bucket"));
    }

    @Test
    @DisplayName("初始化分片上传 - 应返回uploadId")
    void initiateMultipartUpload_shouldReturnUploadId() {
        CreateMultipartUploadResponse mockResponse = CreateMultipartUploadResponse.builder()
                .uploadId("upload-123")
                .build();
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(mockResponse);

        String uploadId = s3Service.initiateMultipartUpload("test/large.zip", "application/zip");

        assertEquals("upload-123", uploadId);
        ArgumentCaptor<CreateMultipartUploadRequest> captor = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(s3Client).createMultipartUpload(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("test/large.zip", captor.getValue().key());
    }

    @Test
    @DisplayName("生成分片预签名URL - 应返回有效URL")
    void generatePresignedUploadPartUrl_shouldReturnUrl() throws Exception {
        PresignedUploadPartRequest mockPresigned = mock(PresignedUploadPartRequest.class);
        when(mockPresigned.url()).thenReturn(new URL("https://s3.amazonaws.com/test-bucket/key?partNumber=1&uploadId=abc"));
        when(s3Presigner.presignUploadPart(any(UploadPartPresignRequest.class))).thenReturn(mockPresigned);

        String url = s3Service.generatePresignedUploadPartUrl("test/key.zip", "upload-123", 1);

        assertNotNull(url);
        assertTrue(url.contains("test-bucket"));
    }

    @Test
    @DisplayName("完成分片上传 - 应正确调用completeMultipartUpload")
    void completeMultipartUpload_shouldCallComplete() {
        List<CompletedPart> parts = List.of(
                CompletedPart.builder().partNumber(1).eTag("etag1").build(),
                CompletedPart.builder().partNumber(2).eTag("etag2").build()
        );
        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(CompleteMultipartUploadResponse.builder().build());

        s3Service.completeMultipartUpload("test/key.zip", "upload-123", parts);

        ArgumentCaptor<CompleteMultipartUploadRequest> captor = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(s3Client).completeMultipartUpload(captor.capture());
        assertEquals("upload-123", captor.getValue().uploadId());
        assertEquals(2, captor.getValue().multipartUpload().parts().size());
    }

    @Test
    @DisplayName("取消分片上传 - 应正确调用abortMultipartUpload")
    void abortMultipartUpload_shouldCallAbort() {
        s3Service.abortMultipartUpload("test/key.zip", "upload-123");

        ArgumentCaptor<AbortMultipartUploadRequest> captor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(s3Client).abortMultipartUpload(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("upload-123", captor.getValue().uploadId());
    }
}
