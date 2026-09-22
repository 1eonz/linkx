package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author mWX556161
 * @date 2020/11/20 16:39
 */
@Data
public class OAuthLoginDto implements Serializable {

    private String accessToken;

    private int expireIn;

    private String refreshToken;

    private int refreshTokenExpireIn;

    private String scope;

    private String tokenType;

    private String userId;

    /**
     * 新增isdn编号
     */
    private String isdncode;

    /**
     * 新增isdn密码
     */
    private String isdnpass;
}
