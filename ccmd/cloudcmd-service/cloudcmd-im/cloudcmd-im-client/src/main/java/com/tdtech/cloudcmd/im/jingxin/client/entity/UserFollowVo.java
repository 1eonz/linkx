package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

@Data
public class UserFollowVo {
    private String avatar;
    private String code;
    private List<DepartmentVo> department;
    private String friendId;
    private Long gmtCreated;
    private String gmtModified;
    private String isdn;
    private String mobile;
    private String name;
    private Integer opType;
    private Integer relation;
    private String remark;
    private Integer state;
    private String stateName;
    private Integer status;
}
