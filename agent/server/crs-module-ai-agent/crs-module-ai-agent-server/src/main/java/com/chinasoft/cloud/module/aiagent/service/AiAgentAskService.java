package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.module.aiagent.controller.app.co.*;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AskApprovalVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.AskDetailVO;

import javax.script.ScriptException;

public interface AiAgentAskService {
    Long askLegacy(AskAgentCO askAgentCO) throws ScriptException;

    AskApprovalVO ask(AskAgentCO askAgentCO) throws ScriptException;

    AskDetailVO getReply(Long id);

    AskApprovalVO getApproval(Long id);

    Boolean updateReplyReadState(UpdateReplyReadStateCO readStateCO);

    void approveCreatedCallback(ApproveCreatedCallbackCO callbackCO);

    void approveCallback(ApproveStatusCallbackCO callbackCO) throws ScriptException;

    String getCacheResult(Long id);

    Boolean updateApprovalStatus(ApproveStatusUpdateCO approveStatusUpdateCO);
}
