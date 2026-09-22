package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * <p>
 * 文件存储信息 DTO，用于 RPC 查询返回与保存入参
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
@Data
public class FileStorageDto implements Serializable {

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
     * 原始文件名
     */
    private String fileName;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 文件MD5校验值，用于去重和完整性校验
     */
    private String fileMd5;
    /**
     * 文件SHA256校验值（更高安全性）
     */
    private String fileSha256;
    /**
     * 文件MIME类型，如 image/png, application/pdf
     */
    private String mimeType;
    /**
     * 文件扩展名，如 .pdf, .jpg
     */
    private String fileExtension;
    /**
     * 存储方式。1：DISK；2：NFS；3：FTP；4：SFTP；5：S3；6：OSS；7：COS；8：MINIO；9：OTHER
     */
    private Integer storageType;
    /**
     * 存储端点/地址（如NFS挂载点、FTP主机、S3 Endpoint）
     */
    private String storageEndpoint;
    /**
     * 存储桶/容器/目录名称（对象存储类）
     */
    private String storageBucket;
    /**
     * 存储路径（相对路径，不含Endpoint/Bucket）
     */
    private String storagePath;
    /**
     * 完整访问URL（临时或永久）
     */
    private String storageFullUrl;
    /**
     * 扩展元数据（JSON格式，存储自定义属性）
     */
    private String metadata;
    /**
     * 文件标签，逗号或JSON数组分隔
     */
    private String tags;
    /**
     * 文件状态。1：UPLOADING；2：AVAILABLE；3：LOCKED；4：ARCHIVED；5：DELETED
     */
    private Integer fileStatus;
    /**
     * 是否加密存储。0：否；1：是
     */
    private Integer isEncrypted;
    /**
     * 加密方式，如 AES-256-GCM
     */
    private String encryptionMethod;
    /**
     * 上传人ID/系统ID
     */
    private String uploadId;
    /**
     * 上传客户端类型。1：人员；2：openapi
     */
    private Integer uploadBy;
    /**
     * 上传时间
     */
    private Date uploadTime;
    /**
     * 文件过期时间
     */
    private Date expireTime;
    /**
     * 记录创建时间
     */
    private Date createTime;
    /**
     * 额外扩展字段（灵活使用）
     */
    private String extraFields;

}