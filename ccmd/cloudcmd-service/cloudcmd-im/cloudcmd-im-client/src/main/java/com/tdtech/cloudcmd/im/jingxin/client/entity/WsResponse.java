package com.tdtech.cloudcmd.im.jingxin.client.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsResponse {
    // register response
    private Integer code;
    // msg
    private String transId;
    private Boolean needAck;
    private String module;
    private String notifyType;
    private String commId;
    private Object data;
}
