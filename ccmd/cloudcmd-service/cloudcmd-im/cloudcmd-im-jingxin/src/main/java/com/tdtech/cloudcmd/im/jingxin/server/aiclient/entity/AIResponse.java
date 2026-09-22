package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AIResponse<T> {
    private Integer code;
    private T data;
    private String msg;
}
