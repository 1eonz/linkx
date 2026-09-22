package com.chinasoft.cloud.module.aiagent.msip.entity;

import lombok.Data;

@Data
public class VirtualUserLogin {

    private Integer grantType = 3;

    private String clientId;

    private String clientSecret;

}
