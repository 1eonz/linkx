package com.chinasoft.cloud.module.aiagent.msip.entity;

import lombok.Data;

import java.util.List;

@Data
public class VirtualUserToken {

    private String accessToken;

    private String tokenType;

    private Long expireIn;

    private List<String> scope;

    private String clientId;
}
