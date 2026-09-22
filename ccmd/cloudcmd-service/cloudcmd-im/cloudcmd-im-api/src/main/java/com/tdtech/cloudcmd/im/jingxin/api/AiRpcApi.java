package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.AiRecordQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountResp;

import java.util.LinkedList;
import java.util.List;

public interface AiRpcApi {
    List<RecordCountResp> agentAskCount(AiRecordQO aiRecordQO, LinkedList<RecordCountReq.GroupEnum> groups);

    /**
     * 获取智能体数量
     *
     * @return 智能体数量
     */
    Long getAgentCount();
}
