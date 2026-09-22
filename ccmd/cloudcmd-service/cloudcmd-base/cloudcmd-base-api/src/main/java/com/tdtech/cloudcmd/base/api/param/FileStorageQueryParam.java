package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>
 * 文件存储信息查询参数，支持按 id、fileUuid、fileMd5 查询
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
@Data
public class FileStorageQueryParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;
    /**
     * 文件全局唯一标识（UUID）
     */
    private String fileUuid;
    /**
     * 文件MD5校验值
     */
    private String fileMd5;

}