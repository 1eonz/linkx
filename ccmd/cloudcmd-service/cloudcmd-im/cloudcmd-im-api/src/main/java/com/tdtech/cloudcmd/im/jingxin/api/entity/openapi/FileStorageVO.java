package com.tdtech.cloudcmd.im.jingxin.api.entity.openapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件存储响应
 */
@Data
@Schema(description = "文件存储响应")
public class FileStorageVO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "文件UUID")
    private String fileUuid;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "文件MIME类型")
    private String mimeType;

    @Schema(description = "文件扩展名")
    private String fileExtension;

    @Schema(description = "文件访问URL")
    private String storageFullUrl;

    @Schema(description = "上传时间")
    private Date uploadTime;

    @Schema(description = "是否命中已有记录（相同MD5文件已存在，未重新落盘）")
    private Boolean hitExist;

}