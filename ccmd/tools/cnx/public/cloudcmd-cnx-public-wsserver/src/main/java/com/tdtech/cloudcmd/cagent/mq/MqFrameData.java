package com.tdtech.cloudcmd.cagent.mq;

import lombok.Data;

/**
 * @author LWX623661
 * @date 2019-12-09 10:57
 * @description
 */
@Data
public class MqFrameData {
    private String module;
    private String notifyType;
    private Object data;
    private boolean fanout;
}
