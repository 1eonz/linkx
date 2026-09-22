package com.chinasoft.cloud.module.aiagent.excel.constant;

public interface AttachmentErrorMessage {
    String ERROR_MESSAGE_NAME_NOT_EMPTY = "“AI前置文件接口”导入表，第%s行“名称”不能为空";

    String ERROR_MESSAGE_NAME_NOT_REPEATED = "“AI前置文件接口”导入表，第%s行“名称”不能重复";

    String ERROR_MESSAGE_METHOD = "“AI前置文件接口”导入表，第%s行“请求方式”不能为空，只能为“GET”、“POST”、“PUT”、“DELETE”其中的一个值";

    String ERROR_MESSAGE_HEADER = "“AI前置文件接口”导入表，第%s行“Header参数”只能为JSON格式";

    String ERROR_MESSAGE_QUERY = "“AI前置文件接口”导入表，第%s行“Query参数”只能为JSON格式";

    String ERROR_MESSAGE_BODY = "“AI前置文件接口”导入表，第%s行“Body参数”只能为JSON格式";
}
