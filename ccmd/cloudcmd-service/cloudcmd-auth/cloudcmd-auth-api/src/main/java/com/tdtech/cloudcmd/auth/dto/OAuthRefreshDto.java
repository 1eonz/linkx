package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/11/23 16:47
 */

@Data
public class OAuthRefreshDto {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * Token过期秒数
     */
    private int expireIn;

    /**
     * 更新令牌，用来获取下一次的访问令牌
     */
    private String refreshToken;

    /**
     * refresh_token过期秒数
     */
    private int refreshTokenExpireIn;

    /**
     * 令牌类型，该值大小写不敏感。默认为Bearer
     */
    private String tokenType;
}
