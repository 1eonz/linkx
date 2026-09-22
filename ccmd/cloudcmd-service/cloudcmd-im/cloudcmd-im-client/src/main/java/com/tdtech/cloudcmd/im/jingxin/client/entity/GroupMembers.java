package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/25
 **/
@Data
public class GroupMembers {
    private Long userId;
    private String idCard;
    private String userAlias;
    private Boolean isDel;
    // 人员类型 0-通讯录用户 1-三方用户 2-协同岗用户
    private Integer type;
    private Integer role;
    private Integer muteType;
    private Integer joinType;
    private Long inviteId;
    private Long inviteTime;
    private Long tag;
    private Long gmtCreated;
    private Long gmtModified;
    private String name;
    private String avatar;
    private Integer gender;
    private String genderName;
    private String mobile;
    private String email;
    private String isdn;
    private Long directLeaderId;
    private Integer status;
    private String statusName;
    private String alias;

}
