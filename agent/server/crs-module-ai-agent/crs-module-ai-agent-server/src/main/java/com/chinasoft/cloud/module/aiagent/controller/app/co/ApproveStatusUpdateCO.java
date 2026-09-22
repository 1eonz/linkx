package com.chinasoft.cloud.module.aiagent.controller.app.co;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "AI审批状态更新")
@Data
public class ApproveStatusUpdateCO {
    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "身份证号")
    private String userID;

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "WebSocket 会话ID")
    private String wsSessionId;

    @Schema(description = "智能体ID")
    private Long agentId;

    @Schema(description = "智能体配置ID")
    private Long agentConfigId;

    public Long getResolvedAgentId() {
        return agentConfigId != null ? agentConfigId : agentId;
    }
}
