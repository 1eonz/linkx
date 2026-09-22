package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

@Data
public class CoopUser {

    /**
     * 协同岗id
     */
    private Long id;

    /**
     * 协同岗名称
     */
    private String name;

    private String orgName;

    private Long orgId;

    private String relatedUserNames;

    private Date operateTime;

    private Long uid;

    private String iconUrl;

    private String levelName;

    private Integer checked;
}
