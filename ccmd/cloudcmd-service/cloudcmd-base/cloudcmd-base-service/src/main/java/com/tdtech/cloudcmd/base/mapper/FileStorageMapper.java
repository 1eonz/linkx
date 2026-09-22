package com.tdtech.cloudcmd.base.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.base.entity.FileStorage;

/**
 * <p>
 * 文件存储信息记录表 Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
@Mapper
@DS("linkx_base")
public interface FileStorageMapper extends BaseMapper<FileStorage> {

}