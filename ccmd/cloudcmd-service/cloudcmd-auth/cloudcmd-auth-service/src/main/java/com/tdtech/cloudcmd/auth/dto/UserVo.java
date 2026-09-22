package com.tdtech.cloudcmd.auth.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * @author zhuangzl
 * @date 2020-06-01 14:58
 */
@Data
public class UserVo extends UserHeaderInfo {

    /**
     * 用户名（默认使用警员编号）
     */
    @NotBlank
    private String username;

    /**
     * 用户密码
     */
    @NotBlank
    private String password;

    /**
     * 授权类型
     */
    private String grantType;

    private String refreshToken;

    /**
     * 登录类型
     */
    private String loginType;

    /**
     * 登录设备
     */
    private String equipment;

}
