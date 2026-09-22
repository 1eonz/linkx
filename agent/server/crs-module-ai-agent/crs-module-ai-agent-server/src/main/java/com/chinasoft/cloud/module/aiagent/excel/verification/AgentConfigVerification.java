package com.chinasoft.cloud.module.aiagent.excel.verification;

import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.enums.ScopeEnum;
import com.chinasoft.cloud.module.aiagent.enums.YesNoEnum;
import com.chinasoft.cloud.module.aiagent.excel.bo.AgentConfigBO;
import com.chinasoft.cloud.module.aiagent.excel.bo.ResultBO;
import com.chinasoft.cloud.module.aiagent.excel.handler.AgentConfigHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_AUDIO_TYPE;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_BODY;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_CATEGORIES;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_DOCUMENT_TYPE;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_HEADER;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_IMAGE_TYPE;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_METHOD;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_NAME_NOT_EMPTY;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_PRIORITY;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_QUERY;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_RECEIVE_IM;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_SCOPE;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_URL;
import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_VIDEO_TYPE;


@Component
public class AgentConfigVerification extends AgentCommonVerification {

    public boolean verifyParams(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        return verifyName(config, resultBO)
                && verifyUrl(config, resultBO)
                && verifyPriority(config, resultBO)
                && verifyCategories(config, resultBO)
                && verifyAudioType(config, resultBO)
                && verifyVideoType(config, resultBO)
                && verifyImageType(config, resultBO)
                && verifyDocumentType(config, resultBO)
                && verifyMethod(config.getHttpMethod(), config.getRowIndex(), resultBO, ERROR_MESSAGE_METHOD)
                && verifyReceiveIm(config, resultBO)
                && verifyScope(config, resultBO)
                && verifyHeader(config, resultBO, ERROR_MESSAGE_HEADER)
                && verifyQuery(config, resultBO, ERROR_MESSAGE_QUERY)
                && verifyBody(config, resultBO, ERROR_MESSAGE_BODY);
    }

    private boolean verifyName(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        boolean result = StringUtils.isNotBlank(config.getName());
        assemblyErrorMsg(resultBO, config.getRowIndex(), result, ERROR_MESSAGE_NAME_NOT_EMPTY);
        return result;
    }

    private boolean verifyUrl(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        boolean result = StringUtils.isNotBlank(config.getUrl());
        assemblyErrorMsg(resultBO, config.getRowIndex(), result, ERROR_MESSAGE_URL);
        return result;
    }

    private boolean verifyPriority(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        boolean result = StringUtils.isNotBlank(config.getPriorityStr())
                && AgentConfigHandler.PRIORITY_VALUES.containsKey(config.getPriorityStr());
        assemblyErrorMsg(resultBO, config.getRowIndex(), result, ERROR_MESSAGE_PRIORITY);
        return result;
    }

    private boolean verifyCategories(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        boolean result = StringUtils.isNotBlank(config.getCategories());
        assemblyErrorMsg(resultBO, config.getRowIndex(), result, ERROR_MESSAGE_CATEGORIES);
        return result;
    }

    private boolean verifyReceiveIm(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        boolean result = YesNoEnum.isValidName(config.getReceiveImStr());
        assemblyErrorMsg(resultBO, config.getRowIndex(), result, ERROR_MESSAGE_RECEIVE_IM);
        return result;
    }

    private boolean verifyScope(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        boolean result = ScopeEnum.isValidName(config.getScopeStr());
        assemblyErrorMsg(resultBO, config.getRowIndex(), result, ERROR_MESSAGE_SCOPE);
        return result;
    }

    private boolean verifyAudioType(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        return verifyMultimodal(config.getAudioTypeStr(), config.getRowIndex(), AgentConfigHandler.AUDIO_TYPES,
                ERROR_MESSAGE_AUDIO_TYPE, resultBO);
    }

    private boolean verifyVideoType(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        return verifyMultimodal(config.getVideoTypeStr(), config.getRowIndex(), AgentConfigHandler.VIDEO_TYPES,
                ERROR_MESSAGE_VIDEO_TYPE, resultBO);
    }

    private boolean verifyImageType(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        return verifyMultimodal(config.getImageTypeStr(), config.getRowIndex(), AgentConfigHandler.IMAGE_TYPES,
                ERROR_MESSAGE_IMAGE_TYPE, resultBO);
    }

    private boolean verifyDocumentType(AgentConfigBO config, ResultBO<AgentConfig> resultBO) {
        return verifyMultimodal(config.getDocumentTypeStr(), config.getRowIndex(), AgentConfigHandler.DOCUMENT_TYPES,
                ERROR_MESSAGE_DOCUMENT_TYPE, resultBO);
    }

    private boolean verifyMultimodal(String typeStr, Integer rowIndex, Set<String> types, String formatErrorMsg,
                                     ResultBO<AgentConfig> resultBO) {
        if (StringUtils.isBlank(typeStr)) {
            return true;
        }

        String[] documentTypes = typeStr.split(AgentConfigHandler.MULTIMODAL_DELIMITER);
        boolean result = Arrays.stream(documentTypes).allMatch(types::contains);
        assemblyErrorMsg(resultBO, rowIndex, result, formatErrorMsg);
        return result;
    }
}