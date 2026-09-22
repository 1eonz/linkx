package com.tdtech.cloudcmd.base.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.base.api.param.FileStorageQueryParam;
import com.tdtech.cloudcmd.base.entity.FileStorage;
import com.tdtech.cloudcmd.base.mapper.FileStorageMapper;
import com.tdtech.cloudcmd.base.service.IFileStorageService;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 文件存储信息记录表 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
@Service
@Slf4j
public class FileStorageServiceImpl extends ServiceImpl<FileStorageMapper, FileStorage>
    implements IFileStorageService {

    @Autowired
    private IdWorker idWorker;

    @Override
    public FileStorage getFileStorageByParam(FileStorageQueryParam param) {
        if (param == null) {
            return null;
        }
        LambdaQueryWrapper<FileStorage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(param.getId() != null, FileStorage::getId, param.getId())
            .eq(StringUtils.isNotBlank(param.getFileUuid()), FileStorage::getFileUuid, param.getFileUuid())
            .eq(StringUtils.isNotBlank(param.getFileMd5()), FileStorage::getFileMd5, param.getFileMd5())
            .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Long saveFileStorage(FileStorage fileStorage) {
        long id = idWorker.nextId();
        fileStorage.setId(id);
        baseMapper.insert(fileStorage);
        return id;
    }

}