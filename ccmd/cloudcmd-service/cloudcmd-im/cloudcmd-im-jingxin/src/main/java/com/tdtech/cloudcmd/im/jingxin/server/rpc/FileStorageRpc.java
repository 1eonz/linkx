package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.base.api.param.FileStorageDto;
import com.tdtech.cloudcmd.base.api.param.FileStorageQueryParam;
import com.tdtech.cloudcmd.base.api.service.FileStorageRpcService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.FileStorageRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.openapi.FileStorageVO;
import com.tdtech.cloudcmd.im.jingxin.server.util.AttachmentUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.MimeTypeUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.UUID;

/**
 * 文件存储 Dubbo RPC 接口实现
 * <p>
 * 文件落盘到 /data/linkx/data/openapi/（共享 PVC），附件记录通过 RPC 保存到 linkx_base.tb_file_storage。
 * 供 openapi 服务调用。
 */
@DubboService
@Slf4j
public class FileStorageRpc implements FileStorageRpcApi {

    /**
     * 文件存储根目录（与 K8s 挂载点 /data/linkx/data/openapi 对应）
     */
    private static final String STORAGE_DIR = "/data/linkx/data/openapi/";

    /**
     * 静态资源访问 URL 前缀（与 WebConfig 中 /collaboration/static/openapi/** 映射对应）
     */
    private static final String URL_PREFIX = "/collaboration/static/openapi";

    /**
     * 存储方式：1-DISK
     */
    private static final Integer STORAGE_TYPE_DISK = 1;

    /**
     * 文件状态：2-AVAILABLE
     */
    private static final Integer FILE_STATUS_AVAILABLE = 2;

    /**
     * 是否加密：0-否
     */
    private static final Integer NOT_ENCRYPTED = 0;

    @DubboReference
    private FileStorageRpcService fileStorageRpcService;

    @Override
    public FileStorageVO uploadFile(byte[] fileBytes, String originalFilename, String mimeType,
                                    String uploadId, Integer uploadBy) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException("文件内容不能为空");
        }

        // MIME 检测下沉到实现层：以文件内容+文件名综合检测为准（防止扩展名/Content-Type 伪造），
        // 入参 mimeType 仅作为无法识别时的兜底声明值
        mimeType = MimeTypeUtils.getMimeType(fileBytes, originalFilename, mimeType);

        // 1. 计算文件 MD5
        String fileMd5 = DigestUtils.md5Hex(fileBytes);
        String fileExtension = AttachmentUtil.getExtension(originalFilename);

        // 2. 基于 MD5 去重：命中已有记录则直接返回
        FileStorageQueryParam queryParam = new FileStorageQueryParam();
        queryParam.setFileMd5(fileMd5);
        FileStorageDto existDto = fileStorageRpcService.getFileStorage(queryParam);
        if (existDto != null) {
            log.info("文件MD5命中已存在记录，直接返回：fileMd5={}, id={}", fileMd5, existDto.getId());
            // storageFullUrl 为空时兜底重新计算（兼容老数据）并回填，后续 buildVo 统一从该字段取
            if (StringUtils.isBlank(existDto.getStorageFullUrl())) {
                existDto.setStorageFullUrl(buildUrl(existDto.getStoragePath()));
            }
            FileStorageVO vo = buildVo(existDto);
            // 标记命中已有记录，供上层在响应 message 中带提示
            vo.setHitExist(true);
            return vo;
        }

        // 3. 落盘到 /data/linkx/data/openapi/
        AttachmentUtil.UploadResult upload = AttachmentUtil.upload(fileBytes, originalFilename, mimeType, STORAGE_DIR);

        // 4. 保存附件记录到 linkx_base.tb_file_storage
        FileStorageDto dto = new FileStorageDto();
        dto.setFileUuid(UUID.randomUUID().toString().replace("-", ""));
        dto.setFileName(originalFilename);
        dto.setFileSize(upload.getFileSize());
        dto.setFileMd5(fileMd5);
        dto.setMimeType(mimeType);
        dto.setFileExtension(fileExtension);
        dto.setStorageType(STORAGE_TYPE_DISK);
        // 存储路径：完整物理路径（含 STORAGE_DIR 前缀），便于后续直接定位文件
        dto.setStoragePath(upload.getFilePath());
        // 完整访问URL：与 storagePath 同步入库，避免后续重复计算
        String fileUrl = buildUrl(dto.getStoragePath());
        dto.setStorageFullUrl(fileUrl);
        dto.setFileStatus(FILE_STATUS_AVAILABLE);
        dto.setIsEncrypted(NOT_ENCRYPTED);
        if (StringUtils.isNotBlank(uploadId)) {
            dto.setUploadId(uploadId);
        }
        if (uploadBy != null) {
            dto.setUploadBy(uploadBy);
        }

        Long id = fileStorageRpcService.saveFileStorage(dto);
        dto.setId(id);

        log.info("文件上传成功：originalName={}, id={}, url={}", originalFilename, id, fileUrl);
        return buildVo(dto);
    }

    /**
     * 提取相对路径：从绝对路径中截掉 STORAGE_DIR 前缀，得到相对路径（含文件名）
     * 例：/data/linkx/data/openapi/20260730120000_xxx.png → 20260730120000_xxx.png
     */
    private String extractRelativePath(String absolutePath) {
        if (StringUtils.isBlank(absolutePath)) {
            return absolutePath;
        }
        String normalized = absolutePath.replace("\\", "/");
        if (normalized.startsWith(STORAGE_DIR)) {
            return normalized.substring(STORAGE_DIR.length());
        }
        return normalized;
    }

    /**
     * 拼接访问 URL：URL前缀 + 相对路径
     * 入参为完整物理路径（storagePath），内部先截掉 STORAGE_DIR 前缀得到相对路径再拼接
     */
    private String buildUrl(String storagePath) {
        String relativePath = extractRelativePath(storagePath);
        if (StringUtils.isBlank(relativePath)) {
            return URL_PREFIX;
        }
        return URL_PREFIX + "/" + relativePath;
    }

    /**
     * 构建返回 VO
     * storageFullUrl 与 DTO 字段名一致，由 BeanCopyUtils 自动拷贝
     */
    private FileStorageVO buildVo(FileStorageDto dto) {
        return BeanCopyUtils.copyBean(dto, FileStorageVO::new);
    }

}