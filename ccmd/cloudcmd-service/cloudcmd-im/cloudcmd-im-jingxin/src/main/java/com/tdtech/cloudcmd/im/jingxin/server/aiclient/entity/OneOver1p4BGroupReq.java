package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OneOver1p4BGroupReq {

    private String  sceneCode;
    private String areaCode;
    private Integer groupType;
    private Long groupId;
    private String groupName;


}
