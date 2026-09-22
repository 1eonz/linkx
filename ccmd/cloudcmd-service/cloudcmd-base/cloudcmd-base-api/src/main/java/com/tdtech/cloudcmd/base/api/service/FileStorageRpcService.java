package com.tdtech.cloudcmd.base.api.service;

import com.tdtech.cloudcmd.base.api.param.FileStorageDto;
import com.tdtech.cloudcmd.base.api.param.FileStorageQueryParam;

/**
 * <p>
 * 文件存储信息 RPC 服务
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
public interface FileStorageRpcService {

    /**
     * 根据条件查询文件存储信息（id、fileUuid、fileMd5 任一非空即可匹配）
     *
     * @param param 查询参数
     * @return 文件存储信息；未命中返回 null
     */
    FileStorageDto getFileStorage(FileStorageQueryParam param);

    /**
     * 保存文件存储信息
     *
     * @param dto 文件存储信息
     * @return 保存后的主键ID
     */
    Long saveFileStorage(FileStorageDto dto);

}