package com.chinasoft.cloud.module.aiagent.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinasoft.cloud.framework.utils.IdWorker;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentQueryApprove;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentQueryApproveMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AgentQueryApproveServiceImpl implements AgentQueryApproveService {

    @Resource
    private AgentQueryApproveMapper agentQueryApproveMapper;

    @Resource
    private IdWorker idWorker;

    @Override
    public Long create(AgentQueryApprove agentQueryApprove) {
        Long id = idWorker.nextId();
        agentQueryApprove.setId(id);
        agentQueryApproveMapper.insert(agentQueryApprove);
        return id;
    }

    @Override
    public void updateById(AgentQueryApprove agentQueryApprove) {
        agentQueryApproveMapper.updateById(agentQueryApprove);
    }

    @Override
    public AgentQueryApprove getByRecordId(String recordId) {
        return agentQueryApproveMapper.selectOne(
            Wrappers.lambdaQuery(AgentQueryApprove.class).eq(AgentQueryApprove::getRecordId, recordId));
    }
}
