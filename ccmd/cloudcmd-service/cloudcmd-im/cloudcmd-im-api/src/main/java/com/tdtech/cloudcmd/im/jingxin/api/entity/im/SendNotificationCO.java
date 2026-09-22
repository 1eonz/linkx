package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Schema(description = "发送通知请求体")
public class SendNotificationCO implements Serializable {

    @Schema(description = "三方应用ID（对应tb_collaboration_client.clientId）")
    private String appId;

    @Schema(description = "用户ID，多个用户ID用\";\"分隔")
    private String targetUserIds;

    @Schema(description = "身份证号，多个用\";\"分隔")
    private String targetIdCards;

    @NotBlank
    @Schema(description = "通知内容")
    private String content;

    @NotBlank
    @Schema(description = "模块名称，用于消息聚合")
    private String moduleName;

    @NotNull
    @Schema(description = "协作消息。0：非协作消息；1：协作消息")
    private Integer collaborativeMsg;

    @Schema(description = "三方系统的消息详情URL")
    private String url;

    @Schema(description = "是否上通知栏。0：不上通知栏；1：上通知栏（默认）。仅在协作消息时有效")
    private Integer showInNotification = 1;

    @Schema(description = "通知ID，由服务端生成，随消息下发并作为持久化记录主键", hidden = true)
    private Long notificationId;
}