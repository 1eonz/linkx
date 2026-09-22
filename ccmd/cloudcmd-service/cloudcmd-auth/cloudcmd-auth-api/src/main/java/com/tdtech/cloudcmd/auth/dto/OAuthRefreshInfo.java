package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/11/20 17:33
 */
@Data
public class OAuthRefreshInfo {

    /**
     * token
     */
    private String authorization;

    /**
     * 客户端的ID
     */
    private String clientId;

    /**
     * 客户端的密钥
     */
    private String clientSecret;

    /**
     * 授权类型，此处的值固定为"refreshtoken"
     */
    private String grantType;

    /**
     * 早前收到的更新令牌
     */
    private String refreshToken;

}
