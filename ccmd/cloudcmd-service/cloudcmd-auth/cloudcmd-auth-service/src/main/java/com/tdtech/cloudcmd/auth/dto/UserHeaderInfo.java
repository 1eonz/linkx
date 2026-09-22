package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * @author zhuangzl
 * @date 2020-06-01 15:29
 */
@Data
public class UserHeaderInfo implements Serializable {

    private String appKey;

    private String requestId;

    private String remoteIp;

}
