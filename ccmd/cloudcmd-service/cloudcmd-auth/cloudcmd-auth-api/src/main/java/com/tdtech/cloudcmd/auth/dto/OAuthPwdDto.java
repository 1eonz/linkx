package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author mWX556161
 * @date 2020/12/24 15:54
 */
@Data
public class OAuthPwdDto implements Serializable {

    private String username;

    private String oldPassword;

    private String newPassword;

    private String repeatNewPassword;
}
