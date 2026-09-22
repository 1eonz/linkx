package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OneOver1p4BResp<T> {

    private Integer code;
    private String msg;
    private T data;


}
