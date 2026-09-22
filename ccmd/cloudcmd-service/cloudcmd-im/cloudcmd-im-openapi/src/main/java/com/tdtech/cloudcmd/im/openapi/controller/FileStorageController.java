package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.im.jingxin.api.FileStorageRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.openapi.FileStorageVO;
import com.tdtech.cloudcmd.im.openapi.aop.OAuthContext;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.Token;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储接口
 * <p>
 * 文件通过 RPC 调用 jingxin 服务落盘到 /data/linkx/data/openapi/，
 * 附件记录保存到 linkx_base.tb_file_storage 表。
 * upload_id 取自 token 的 clientId，upload_by 固定为 2（openapi）。
 */
@Tag(name = "文件存储", description = "文件上传相关接口")
@Slf4j
@RestController
@RequestMapping("/openapi/v1/file")
@RequiredArgsConstructor
@OpenApiOauth(BusinessScopeEnum.THIRD_PARTY_APPLICATION)
@RequestLimit(business = "文件上传")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
public class FileStorageController {

    /**
     * 上传客户端类型：2-openapi
     */
    private static final Integer UPLOAD_BY_OPENAPI = 2;

    @DubboReference
    private FileStorageRpcApi fileStorageRpcApi;

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 文件MD5（作为文件标识，可用于后续去重与文件访问）
     */
    @Operation(summary = "上传文件", description = "上传文件到 openapi 目录并记录到附件表，返回文件MD5作为文件标识")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @PostMapping("/upload")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.failure("文件不能为空");
        }
        try {
            // 从 token 获取三方应用 clientId 作为上传人ID
            Token token = OAuthContext.getToken();
            String uploadId = token != null ? token.getClientId() : null;
            // MultipartFile 不可序列化，转字节数组通过 RPC 传输
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            // MIME 检测下沉到 RPC 实现层，此处仅透传客户端声明的 Content-Type 作为兜底声明值
            FileStorageVO vo = fileStorageRpcApi.uploadFile(fileBytes, originalFilename, file.getContentType(),
                    uploadId, UPLOAD_BY_OPENAPI);
            // 命中已有记录时返回成功，但在 message 中带提示信息
            if (Boolean.TRUE.equals(vo.getHitExist())) {
                return R.success(ResponseCodeEnum.SUCCESS.getCode(),
                        "文件已存在，命中已有记录: fileMd5=" + vo.getFileMd5(),
                        vo.getFileMd5());
            }
            return R.success(vo.getFileMd5());
        } catch (IOException e) {
            log.error("读取上传文件内容失败", e);
            return R.failure("读取上传文件内容失败");
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.failure(e.getMessage());
        }
    }

}