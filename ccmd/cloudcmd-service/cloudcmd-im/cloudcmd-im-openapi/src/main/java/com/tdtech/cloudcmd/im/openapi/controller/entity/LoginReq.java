package com.tdtech.cloudcmd.im.openapi.controller.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
@Schema(description = "登录请求参数")
public class LoginReq implements Serializable {

    @Schema(description = "授权类型：1.authorization_code, 2.password, 3.client_credentials, 4.refresh_token(目前只支持3)", example = "1", required = true)
    private Integer grantType;

    @Schema(description = "客户端ID", example = "client_app", required = true)
    private String clientId;

    @Schema(description = "客户端密钥", example = "secret123", required = true)
    private String clientSecret;

}
