package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
public class LoginReq implements Serializable {

    @NotNull(message = "grantType不能为空")
    private Integer grantType;

    @NotBlank(message = "clientId不能为空")
    private String clientId;

    @NotBlank(message = "clientSecret不能为空")
    private String clientSecret;

    private String scope;

}
