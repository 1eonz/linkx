package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

/**
 * 根据dems的token对ICC的token进行token或删除返回值
 */
@Data
public class OAuthDemsTokenRefreshDto {

    /**
     * 0表示dems的token未失效，1表示已失效
     */
    private Integer isInvalid;
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
