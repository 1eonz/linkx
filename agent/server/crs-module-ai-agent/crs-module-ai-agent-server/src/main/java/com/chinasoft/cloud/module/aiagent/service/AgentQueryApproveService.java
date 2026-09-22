package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentQueryApprove;

public interface AgentQueryApproveService {

    Long create(AgentQueryApprove agentQueryApprove);

    void updateById(AgentQueryApprove agentQueryApprove);

    AgentQueryApprove getByRecordId(String recordId);
}
