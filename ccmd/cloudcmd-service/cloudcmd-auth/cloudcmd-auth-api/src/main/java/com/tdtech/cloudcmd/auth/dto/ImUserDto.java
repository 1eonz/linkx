package com.tdtech.cloudcmd.auth.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
public class ImUserDto implements Serializable {

    private Long id;
    private String code;
    private String name;
    private String avatar;
    private String gender;
    private String mobile;
    private String email;
    private String isdn;
    private String idCard;
    private String district;
    private Long directLeaderId;
    private String directLeaderName;
    private String departmentCode;
    private String departmentName;
    private Long departmentId;

    /******** ICS属性 IM没得 ********/
    //用户类型：0 超级管理员 1 其他
    private Integer type;
    //是否锁定 1 是 0 否
    private Integer status;
    private String password;
    private Date pwdTime;

    private Date gmtCreated;
    private Date gmtUpdated;
}
