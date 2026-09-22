package com.tdtech.cloudcmd.base.service.rpc;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.tdtech.cloudcmd.base.api.param.FileStorageDto;
import com.tdtech.cloudcmd.base.api.param.FileStorageQueryParam;
import com.tdtech.cloudcmd.base.api.service.FileStorageRpcService;
import com.tdtech.cloudcmd.base.entity.FileStorage;
import com.tdtech.cloudcmd.base.service.IFileStorageService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 文件存储信息 RPC 服务实现
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
@DubboService
@Slf4j
public class FileStorageRpcServiceImpl implements FileStorageRpcService {

    @Autowired
    private IFileStorageService fileStorageService;

    @Override
    public FileStorageDto getFileStorage(FileStorageQueryParam param) {
        log.debug("getFileStorage param:{}", param);
        FileStorage fileStorage = fileStorageService.getFileStorageByParam(param);
        if (fileStorage == null) {
            return null;
        }
        return BeanCopyUtils.copyBean(fileStorage, FileStorageDto::new);
    }

    @Override
    public Long saveFileStorage(FileStorageDto dto) {
        log.debug("saveFileStorage dto:{}", dto);
        if (dto == null) {
            return null;
        }
        FileStorage fileStorage = BeanCopyUtils.copyBean(dto, FileStorage::new);
        return fileStorageService.saveFileStorage(fileStorage);
    }

}