package com.chinasoft.cloud.module.aiagent.excel.verification;

import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import com.chinasoft.cloud.module.aiagent.excel.bo.AgentAttachmentConfigBO;
import com.chinasoft.cloud.module.aiagent.excel.bo.ResultBO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.chinasoft.cloud.module.aiagent.excel.constant.AttachmentErrorMessage.ERROR_MESSAGE_BODY;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AttachmentErrorMessage.ERROR_MESSAGE_HEADER;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AttachmentErrorMessage.ERROR_MESSAGE_METHOD;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AttachmentErrorMessage.ERROR_MESSAGE_NAME_NOT_EMPTY;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AttachmentErrorMessage.ERROR_MESSAGE_QUERY;

@Component
public class AgentAttachmentConfigVerification extends AgentCommonVerification {

    public boolean verifyParams(AgentAttachmentConfigBO config,
                                ResultBO<AgentAttachmentConfig> resultBO) {
        return verifyName(config, resultBO)
                && verifyMethod(config.getMethod(), config.getRowIndex(), resultBO, ERROR_MESSAGE_METHOD)
                && verifyHeader(config, resultBO, ERROR_MESSAGE_HEADER)
                && verifyQuery(config, resultBO, ERROR_MESSAGE_QUERY)
                && verifyBody(config, resultBO, ERROR_MESSAGE_BODY);
    }

    private boolean verifyName(AgentAttachmentConfigBO config, ResultBO<AgentAttachmentConfig> resultBO) {
        boolean result = StringUtils.isNotBlank(config.getName());
        assemblyErrorMsg(resultBO, config.getRowIndex(), result, ERROR_MESSAGE_NAME_NOT_EMPTY);
        return result;
    }
}
