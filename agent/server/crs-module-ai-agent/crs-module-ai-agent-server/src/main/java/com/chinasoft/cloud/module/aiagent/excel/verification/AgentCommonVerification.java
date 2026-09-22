package com.chinasoft.cloud.module.aiagent.excel.verification;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.chinasoft.cloud.module.aiagent.excel.bo.AgentCommonBO;
import com.chinasoft.cloud.module.aiagent.excel.bo.ResultBO;
import com.chinasoft.cloud.module.aiagent.excel.handler.AgentConfigHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class AgentCommonVerification {
    public static void assemblyErrorMsg(ResultBO<?> resultBO, Integer rowIndex,
                                        boolean result, String formatErrorMsg) {
        if (!result) {
            resultBO.getRowIndexToErrorMsgMap().putIfAbsent(rowIndex, String.format(Locale.ENGLISH,
                    formatErrorMsg, rowIndex));
        }
    }

    /**
     * 判断字符串是否是json字符串
     *
     * @param str 字符粗
     * @return 是否是json字符串
     */
    protected boolean isJsonStr(String str) {
        try {
            JSON.parse(str);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    protected boolean verifyMethod(String method, Integer rowIndex, ResultBO<?> resultBO, String formatErrorMsg) {
        boolean result = StringUtils.isNotBlank(method) && AgentConfigHandler.REQUEST_METHODS.contains(method);
        assemblyErrorMsg(resultBO, rowIndex, result, formatErrorMsg);
        return result;
    }

    protected boolean verifyHeader(AgentCommonBO config, ResultBO<?> resultBO, String formatErrorMsg) {
        return verifyHeaderOrQueryOrBody(config.getHeader(), config.getRowIndex(), resultBO, formatErrorMsg);
    }

    protected boolean verifyQuery(AgentCommonBO config, ResultBO<?> resultBO, String formatErrorMsg) {
        return verifyHeaderOrQueryOrBody(config.getQuery(), config.getRowIndex(), resultBO, formatErrorMsg);
    }

    protected boolean verifyBody(AgentCommonBO config, ResultBO<?> resultBO, String formatErrorMsg) {
        return verifyHeaderOrQueryOrBody(config.getBody(), config.getRowIndex(), resultBO, formatErrorMsg);
    }

    private boolean verifyHeaderOrQueryOrBody(String str, Integer rowIndex, ResultBO<?> resultBO, String formatErrorMsg) {
        boolean result = StringUtils.isBlank(str) || isJsonStr(str);
        assemblyErrorMsg(resultBO, rowIndex, result, formatErrorMsg);
        return result;
    }
}
