package com.tdtech.cloudcmd.im.jingxin.server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TasksExpiredVO {

    /**
     * 发起人姓名
     */
    @Schema(description = "发起人姓名")
    private String senderName;
    /**
     * 问题内容
     */
    @Schema(description = "问题内容")
    private String questionContent;
    /**
     * 问题时间
     */
    @Schema(description = "问题时间")
    private String questionTime;
    /**
     * 群组名称
     */
    @Schema(description = "群组名称")
    private String groupName;
    /**
     * 协同岗名称
     */
    @Schema(description = "协同岗名称")
    private String postName;
    /**
     * 组织名称
     */
    @Schema(description = "组织名称")
    private String orgName;
    /**
     * 协同岗人员名称
     */
    @Schema(description = "协同岗人员名称")
    private String postUserNames;

    @Schema(description = "协同岗人员ID")
    private String relatedUserIds;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "逾期时间")
    private String overDueTime;

    @Schema(description = "群组ID")
    private Long groupId;

    @Schema(description = "IM消息对应的ICS消息ID")
    private Long icsMsgId;

    @Schema(description = "是否处理")
    private Boolean isHandle;

    @Schema(description = "处理时间")
    private String handleTime;
}
