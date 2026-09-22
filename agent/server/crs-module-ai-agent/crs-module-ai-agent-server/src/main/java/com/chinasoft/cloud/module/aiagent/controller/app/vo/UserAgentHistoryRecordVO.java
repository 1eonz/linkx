package com.chinasoft.cloud.module.aiagent.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAgentHistoryRecordVO {

    @Schema(description = "问答记录ID")
    private Long id;

    @Schema(description = "智能体ID")
    private String agentConfigId;

    @Schema(description = "请求文本")
    private String queryContent;

    @Schema(description = "响应文本")
    private String responseContent;

    @Schema(description = "当前回答字符位置，-1 表示全部回答完成")
    private Integer replyPosition;

    @Schema(description = "回答是否暂停")
    private Boolean replyPaused;

    @Schema(description = "是否需要审核")
    private Boolean approvalRequired;

    @Schema(description = "审核子模式 0-先问后审 1-先审后答")
    private String approvalSubMode;

    @Schema(description = "审核状态：0-无需审核 1-审核中 2-审核完成 3-待建单")
    private String approvalStatus;

    @Schema(description = "审核结果：0-通过 1-不通过")
    private Integer approveResult;

    @Schema(description = "审核H5地址")
    private String approveUrl;

    @Schema(description = "审核详情H5地址")
    private String approveDetailUrl;

    @Schema(description = "给审批领导发送卡片的url")
    private String toLeaderUrl;

    @Schema(description = "审批用户")
    private String approveUser;

    @Schema(description = "上传文件id")
    private String attachement;

    @Schema(description = "上传文件的路径")
    private String attachement_path;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "提问人名称")
    private String userName;

    @Schema(description = "响应时间")
    private LocalDateTime answerTime;

    @Schema(description = "提问时间")
    private LocalDateTime queryTime;
}