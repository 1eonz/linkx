package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * @author zhuangzl
 * @date 2020-06-01 14:44
 */
@Data
public class AuthInfo implements Serializable {

    private Long token;

    private String refreshToken;

    private Long expireIn;

    private String scope;

    private String tokenType;

    private String userId;

}
