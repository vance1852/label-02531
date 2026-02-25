package com.s3manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.s3manager.entity.MultipartRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MultipartRecordMapper extends BaseMapper<MultipartRecord> {
}
