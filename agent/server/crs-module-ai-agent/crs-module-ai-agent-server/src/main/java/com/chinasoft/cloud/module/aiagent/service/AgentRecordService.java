package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.framework.common.pojo.PageResult;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentRecordCursorQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.AgentRecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordCountQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.qo.RecordQO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AgentRecordCursorVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.RecordCountVO;
import com.chinasoft.cloud.module.aiagent.controller.app.qo.UserAgentHistoryQO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.UserAgentHistoryRecordVO;
import com.chinasoft.cloud.module.aiagent.controller.app.vo.UserAgentRecordVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;

import java.util.List;
import java.util.Optional;

public interface AgentRecordService {
    void addRecord(AgentRecord agentrecord);

    List<RecordCountVO> countRecord(RecordCountQO recordCountQO);

    List<AgentRecord> recordListByAgentName(RecordQO recordQO);

    PageResult<AgentRecord> listRecordPaged(AgentRecordQO qo);

    List<UserAgentRecordVO> listUserAgentRecords(String userId);

    PageResult<UserAgentHistoryRecordVO> listUserAgentHistoryPaged(UserAgentHistoryQO qo);

    void deleteById(Long id);

    void deleteUserAgentRecords(String idCard, UserAgentRecordVO userAgentRecordVO);

    Optional<AgentRecord> getRecordById(Long id);

    AgentRecordCursorVO listRecordByCursor(AgentRecordCursorQO qo);
}