package com.tdtech.cloudcmd.bean;

import lombok.Data;

/**
 * @author zhuangzl
 * @date 2018-03-20 17:09
 */
@Data
public class MsgBody<T> {
    private String module;
    private String notifyType;
    private T data;
}
