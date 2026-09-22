package com.tdtech.cloudcmd.im.openapi.controller.entity;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token implements Serializable {

    @Schema(description = "ACCESS_TOKEN")
    private String accessToken;
    @Schema(description = "Bearer")
    private String tokenType;
    @Schema(description = "超时时间")
    private int expireIn;
    @Schema(description = "作用域")
    private List<String> scope;
    @Schema(description = "应用ID")
    private String clientId;

}
