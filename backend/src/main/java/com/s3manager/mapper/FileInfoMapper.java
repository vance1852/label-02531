package com.s3manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.s3manager.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}
