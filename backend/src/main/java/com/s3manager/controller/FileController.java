package com.s3manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.s3manager.common.R;
import com.s3manager.dto.*;
import com.s3manager.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** 普通文件上传 */
    @PostMapping("/upload")
    public R<FileInfoVO> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.upload(file));
    }

    /** 文件列表(分页) */
    @GetMapping
    public R<Page<FileInfoVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(fileService.listFiles(page, size, keyword));
    }

    /** 获取下载预签名URL */
    @GetMapping("/download/{id}")
    public R<String> download(@PathVariable Long id) {
        return R.ok(fileService.getDownloadUrl(id));
    }

    /** 删除文件 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.deleteFile(id);
        return R.ok();
    }

    /** 初始化分片上传 */
    @PostMapping("/multipart/init")
    public R<MultipartInitResponse> initMultipart(@Valid @RequestBody MultipartInitRequest request) {
        return R.ok(fileService.initMultipartUpload(request));
    }

    /** 获取分片预签名URL */
    @GetMapping("/multipart/presign")
    public R<String> presignPart(
            @RequestParam Long fileId,
            @RequestParam int partNumber) {
        return R.ok(fileService.getPresignedPartUrl(fileId, partNumber));
    }

    /** 完成分片上传 */
    @PostMapping("/multipart/complete")
    public R<FileInfoVO> completeMultipart(@Valid @RequestBody MultipartCompleteRequest request) {
        return R.ok(fileService.completeMultipartUpload(request));
    }

    /** 取消分片上传 */
    @PostMapping("/multipart/abort")
    public R<Void> abortMultipart(@RequestParam Long fileId) {
        fileService.abortMultipartUpload(fileId);
        return R.ok();
    }
}
