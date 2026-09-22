package com.chinasoft.cloud.module.aiagent.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "AI问答审核信息")
@Data
public class AskApprovalVO {

    @Schema(description = "问答记录ID")
    private Long id;

    @Schema(description = "是否需要审核")
    private Boolean approvalRequired;

    @Schema(description = "审核状态：0-无需审核 1-审核中 2-审核完成 3-待建单")
    private String approvalStatus;

    @Schema(description = "审核子模式 0-先问后审 1-先审后答")
    private String approvalSubMode;

    @Schema(description = "审核结果：0-通过 1-不通过")
    private Integer approveResult;

    @Schema(description = "审核单号")
    private String approveNo;

    @Schema(description = "审核H5地址")
    private String approveUrl;

    @Schema(description = "审核详情H5地址")
    private String approveDetailUrl;

    @Schema(description = "给审批领导发送卡片的url")
    private String toLeaderUrl;

    @Schema(description = "审核说明")
    private String approveDescription;

    @Schema(description = "审核时间")
    private String approveTime;

    @Schema(description = "审核人")
    private String approveUser;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "提问人名称")
    private String userName;

    @Schema(description = "响应时间")
    private LocalDateTime answerTime;

    @Schema(description = "提问时间")
    private LocalDateTime queryTime;
}