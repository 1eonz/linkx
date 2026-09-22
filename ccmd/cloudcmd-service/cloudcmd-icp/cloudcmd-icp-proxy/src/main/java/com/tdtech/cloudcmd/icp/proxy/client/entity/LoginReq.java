package com.tdtech.cloudcmd.icp.proxy.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class LoginReq {
    // 密码
    @NotBlank
    private String password;
    // 强制登录,默认true。
    // true: 强制登录。
    // false: 正常登录。
    @Null
    private String force = "true";
    // 用于扩展。
    @Null
    private Map<String, Object> param;
    // 本地IP。
    @NotBlank
    @JsonProperty("localip")
    private String localIp;
}
