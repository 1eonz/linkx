package com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString(exclude = "image")
@Accessors(chain = true)
public class OneOver1p4BCallReq {

    // 填代理用户departmentCode
    private String areaCode;

    private Integer groupType;

    private String groupId;

    private String groupName;

    // 协同岗支撑人员信息
    private Operator operatorInfo;

    private String image;

    @Data
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Operator {
        private String operatorName;
        private String operatorIdCard;
        private String departmentCode;
        private String departmentId;
        private String departmentName;
    }


}
