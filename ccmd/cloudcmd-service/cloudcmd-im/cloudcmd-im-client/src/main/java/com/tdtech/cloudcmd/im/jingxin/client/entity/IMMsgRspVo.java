package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 发消息给IM后，IM的响应类
 */
@Getter
@Setter
@ToString
public class IMMsgRspVo {
    /**
     * 服务器分配的msgId。可选，当非多人转发，且服务器处理成功时必填
     */
    private String msgId;

    /**
     * 如果消息中携带转发列表，按转发列表返回。则基于转发人返回原因值。非多人转发不携带
     */
    private List<Result> multiResult;

    @Getter
    @Setter
    @ToString
    public static class Result {
        /**
         * 0-成功；1-未知错误；2-资源不存在；3-群组禁言；4-数据校验失败
         */
        private Integer code;

        /**
         * 错误描述。可选
         */
        private String msg;

        /**
         * 服务器分配的msgId。可选，code为0时必填
         */
        private String msgId;
    }
}
