package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/8/12
 **/
@Data
public class CollaborationClientLoginDTO {
    private String accessToken;
    private Integer expireIn;
    private String refreshToken;
    private String clientId;
    private String clientName;
    private Integer refreshTokenExpireIn;
}
