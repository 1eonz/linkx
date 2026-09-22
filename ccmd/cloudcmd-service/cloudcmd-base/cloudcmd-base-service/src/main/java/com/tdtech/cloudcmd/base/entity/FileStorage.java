package com.tdtech.cloudcmd.base.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文件存储信息记录表
 * </p>
 *
 * @author zhuangzl
 * @since 2026-07-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_file_storage")
public class FileStorage implements Serializable {

    public static final String ID = "id";
    public static final String FILE_UUID = "file_uuid";
    public static final String FILE_NAME = "file_name";
    public static final String FILE_SIZE = "file_size";
    public static final String FILE_MD5 = "file_md5";
    public static final String FILE_SHA256 = "file_sha256";
    public static final String MIME_TYPE = "mime_type";
    public static final String FILE_EXTENSION = "file_extension";
    public static final String STORAGE_TYPE = "storage_type";
    public static final String STORAGE_ENDPOINT = "storage_endpoint";
    public static final String STORAGE_BUCKET = "storage_bucket";
    public static final String STORAGE_PATH = "storage_path";
    public static final String STORAGE_FULL_URL = "storage_full_url";
    public static final String METADATA = "metadata";
    public static final String TAGS = "tags";
    public static final String FILE_STATUS = "file_status";
    public static final String IS_ENCRYPTED = "is_encrypted";
    public static final String ENCRYPTION_METHOD = "encryption_method";
    public static final String UPLOAD_ID = "upload_id";
    public static final String UPLOAD_BY = "upload_by";
    public static final String UPLOAD_TIME = "upload_time";
    public static final String EXPIRE_TIME = "expire_time";
    public static final String CREATE_TIME = "create_time";
    public static final String EXTRA_FIELDS = "extra_fields";
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