package com.tdtech.cloudcmd.admin.resource.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "融合通信平台配置信息")
public class IcpConfigVO {

    @NotBlank
    @Schema(description = "服务器IP地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    private String ip;

    @NotNull
    @Schema(description = "服务器IP地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "8002")
    private Integer port;

    @NotBlank
    @Schema(description = "网关代理用户", requiredMode = Schema.RequiredMode.REQUIRED, example = "100001")
    private String username;

    @NotNull
    @Schema(description = "网关代理用户密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull
    @Schema(description = "ICP服务器消息websocket地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "/sdkserver?agentVersion=2.0")
    private String wssUrl;

    @Schema(description = "部门根节点", requiredMode = Schema.RequiredMode.REQUIRED)
    private String departmentId;

    @Schema(description = "部门根节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String departmentName;

    @Schema(description = "摄像头层级根节点", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cameraLevelId;

    @Schema(description = "摄像头层级根节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cameraLevelName;

    @Schema(description = "协议类型, 默认取值5, 即HTTP")
    private Integer protocol;

}
