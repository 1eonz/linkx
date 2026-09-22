package com.tdtech.linkx.node.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AuthorizedCallbackDTO {

    @NotNull(message = "授权状态不能为空")
    private Integer authorized;

    private String desc;

    /**
     * 授权有效期（毫秒时间戳），对端授权时传给本端
     */
    private Long expiredIn;
}
