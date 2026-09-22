package com.tdtech.cloudcmd.icp.proxy.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginResp {

    private String rsp;
    // 版本号。
    private String version;
    // 登录成功获取的session。
    // 业务功能需要此session。
    private String session;
    // 用户号。
    private String isdn;
    // .
    // .
    // .
    // .
    // .
    // .
    // 其他字段忽略 详情见文档
}
