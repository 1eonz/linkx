package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

@Data
public class ImUserNodeInfoVO {
    private Long userId;
    private String name;
    private Long departmentId;
    private String departmentPeerNode;
    private String departmentPeerNodeGateWayPrefix;
    private String departmentPeerNodeIP;
    private String version;
}
