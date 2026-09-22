package com.tdtech.cloudcmd.auth.dto;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author mWX556161
 * @date 2020/12/24 15:54
 */
@Getter
@Setter
@ToString
public class OAuthPwdDto implements Serializable {

    @Deprecated
    private String username;
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;

    private String repeatNewPassword;
}
