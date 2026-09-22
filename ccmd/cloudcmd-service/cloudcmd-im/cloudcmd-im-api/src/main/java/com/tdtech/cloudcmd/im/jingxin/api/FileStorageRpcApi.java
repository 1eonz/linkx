package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.im.jingxin.api.entity.openapi.FileStorageVO;

/**
 * 文件存储 RPC 接口
 * <p>
 * 提供 Dubbo 远程调用，供 openapi 服务上传文件。
 * 文件落盘到 jingxin 服务所在 Pod 的 /data/linkx/data/openapi/（共享 PVC），
 * 附件记录保存到 linkx_base.tb_file_storage 表。
 */
public interface FileStorageRpcApi {

    /**
     * 上传文件并保存附件记录
     *
     * @param fileBytes   文件字节数组（MultipartFile 不可序列化，故传字节）
     * @param originalFilename 原始文件名
     * @param mimeType    文件MIME类型声明值，作为兜底。实现层会以文件内容+文件名综合检测为准，
     *                    仅当无法识别（识别为 octet-stream）时采用本声明值兜底。可为空
     * @param uploadId    上传人ID/系统ID（openapi 场景为三方应用 clientId）
     * @param uploadBy    上传客户端类型。1：人员；2：openapi
     * @return 文件存储信息（含访问URL）
     */
    FileStorageVO uploadFile(byte[] fileBytes, String originalFilename, String mimeType,
                             String uploadId, Integer uploadBy);

}