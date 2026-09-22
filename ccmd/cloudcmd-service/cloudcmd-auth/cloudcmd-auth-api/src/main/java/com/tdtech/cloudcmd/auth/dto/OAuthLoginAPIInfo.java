package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/11/18 15:15
 */
@Data
public class OAuthLoginAPIInfo implements Serializable {

    /**
     * 客户端的ID
     */
    private String clientId;

    /**
     * 客户端的密钥
     */
    private String clientSecret;

    /**
     * 授权类型，此处的值固定为"password"
     */
    private String grantType;

    private String redirectUri;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户的密码
     */
    private String password;

    /**
     * 范围
     */
    private String scope;

    /**
     * 随机字符串，防止CSFR攻击。使用安全随机数
     */
    private String state;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 应用ID
     */
    private Long applicationId;

}
