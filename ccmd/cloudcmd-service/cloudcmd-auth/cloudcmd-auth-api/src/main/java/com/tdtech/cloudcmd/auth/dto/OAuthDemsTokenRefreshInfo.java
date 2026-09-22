package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

/**
 * 根据dems的token对ICC的token进行token或删除
 */
@Data
public class OAuthDemsTokenRefreshInfo {

    /**
     * dems的token
     */
    private String token;
    /**
     * ICC的token
     */
    private String accessToken;

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
