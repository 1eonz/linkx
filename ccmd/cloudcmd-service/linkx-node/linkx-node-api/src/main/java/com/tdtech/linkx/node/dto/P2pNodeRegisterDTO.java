package com.tdtech.linkx.node.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class P2pNodeRegisterDTO {

    @NotBlank(message = "peerId不能为空")
    private String peerId;

    @NotBlank(message = "IP不能为空")
    private String ip;

    @NotNull(message = "端口不能为空")
    private Integer port;

    private String name;

    private String tag;

    private String version;

    private String callback;
}
