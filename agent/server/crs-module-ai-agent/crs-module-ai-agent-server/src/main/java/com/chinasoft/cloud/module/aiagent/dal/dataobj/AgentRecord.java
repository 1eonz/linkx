package com.chinasoft.cloud.module.aiagent.dal.dataobj;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chinasoft.cloud.framework.tenant.core.aop.TenantIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("ai_agent_record")
@KeySequence("ai_agent_record") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@TenantIgnore
public class AgentRecord {
    @TableId
    private Long id;

    private String userName;

    private String identityCardNumber;

    private String queryContent;

    private String responseContent;

    private Integer replyPosition;

    private Boolean replyPaused;

    private LocalDateTime time;

    private String agentName;

    private Long agentConfigId;

    private String departmentCode;

    private String departmentId;

    private String departmentName;

    @TableLogic
    private Boolean deleted;

    private String attachement;

    private String attachementPath;

    private Integer approvalEnabled;

    private String approvalSubMode;

    private LocalDateTime answerTime;

    //提问类型：1-智能体ai助手提问，2-@群ai助手提问，3-单聊智能体，4-群聊普通消息（非@）
    private Integer askType;

    // IM会话ID，仅 IM 透传场景(askType=2/3)记录，用于按会话拉取上下文
    private Long imSessionId;
}