package com.s3manager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("multipart_record")
public class MultipartRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private Integer partNumber;

    private String etag;

    private Long partSize;

    /** 0-上传中 1-已完成 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
