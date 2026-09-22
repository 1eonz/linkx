package com.chinasoft.cloud.module.aiagent.controller.app.co;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "AI审批建单回调")
@Data
public class ApproveCreatedCallbackCO {

    @Schema(description = "Linkx问答记录ID")
    @NotNull(message = "recordId cannot be null")
    private Long recordId;

    @Schema(description = "第三方审批单号")
    @NotBlank(message = "approveNo cannot be blank")
    private String approveNo;

    @Schema(description = "第三方审批详情H5地址")
    @NotBlank(message = "approveDetailUrl cannot be blank")
    private String approveDetailUrl;

    @Schema(description = "给审批领导发送卡片的url")
    @NotBlank(message = "toLeaderUrl cannot be blank")
    private String toLeaderUrl;

    @Schema(description = "审批用户")
    @NotBlank(message = "approveUser cannot be blank")
    private String approveUser;
}
