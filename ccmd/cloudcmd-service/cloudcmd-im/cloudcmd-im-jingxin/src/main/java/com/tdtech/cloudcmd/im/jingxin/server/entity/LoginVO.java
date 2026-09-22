package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
@Builder
public class LoginVO implements Serializable {

    private String accessToken;

    private String tokenType;

    private int expireIn;

//    private String refreshToken;
//
//    private int refreshTokenExpireIn;

    private String scope;

}
