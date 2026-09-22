package com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserProfileCreateReq {

    @NotNull
    private Integer type;

    @NotNull
    private Long userId;
}
