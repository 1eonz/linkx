package com.chinasoft.cloud.module.aiagent.dal.dataobj;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chinasoft.cloud.framework.tenant.core.aop.TenantIgnore;
import lombok.Data;

@TableName("ai_agent_query_approve")
@KeySequence("ai_agent_query_approve")
@Data
@TenantIgnore
public class AgentQueryApprove {

    @TableId
    private Long id;

    private String recordId;

    private String approveNo;

    private String approveUrl;

    private String approveDetailUrl;

    private String toLeaderUrl;

    private String approveState;

    private Integer approve;

    private String approveDescription;

    private String approveTime;

    private String approveUser;
}
