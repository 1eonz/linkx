package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ImToken {
    private String accessToken;
    private Long expireIn;
    private String refreshToken;
    private String refreshTokenExpireIn;
    private String scope;
    private String tokenType;
    private ProxyUser proxyUser;
    private Department department;

    @Getter
    @Setter
    @ToString
    public static class ProxyUser {
        private Long id;
        private String isdn;
    }

    @Getter
    @Setter
    @ToString
    public static class Department{
        private Long departmentId;
        private String departmentCode;
        private String departmentName;
    }
}
