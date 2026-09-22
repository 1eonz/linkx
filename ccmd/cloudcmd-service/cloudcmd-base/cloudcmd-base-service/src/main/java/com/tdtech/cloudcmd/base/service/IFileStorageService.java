package com.tdtech.cloudcmd.base.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.base.api.param.FileStorageQueryParam;
import com.tdtech.cloudcmd.base.entity.FileStorage;

/**
 * <p>
 * 文件存储信息记录表 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
public interface IFileStorageService extends IService<FileStorage> {

    /**
     * 根据条件查询文件存储信息（id、fileUuid、fileMd5 任一非空即可匹配）
     *
     * @param param 查询参数
     * @return 文件存储信息；未命中返回 null
     */
    FileStorage getFileStorageByParam(FileStorageQueryParam param);

    /**
     * 保存文件存储信息
     *
     * @param fileStorage 文件存储信息
     * @return 保存后的主键ID
     */
    Long saveFileStorage(FileStorage fileStorage);

}