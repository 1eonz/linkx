package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiVirtualUser {

    private String clientId;

    private String clientSec;

    private Long agentId;
}
