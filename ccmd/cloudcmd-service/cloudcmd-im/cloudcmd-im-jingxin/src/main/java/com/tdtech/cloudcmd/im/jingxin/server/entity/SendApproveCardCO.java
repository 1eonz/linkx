package com.tdtech.cloudcmd.im.jingxin.server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Schema(description = "发送审批卡片消息参数")
public class SendApproveCardCO implements Serializable {
    @NotNull
    @Schema(description = "发送人id")
    private Long userId;
    @NotBlank
    @Schema(description = "审批用户")
    private String approveUser;
    @NotBlank
    @Schema(description = "审批地址")
    private String toLeaderUrl;
}
