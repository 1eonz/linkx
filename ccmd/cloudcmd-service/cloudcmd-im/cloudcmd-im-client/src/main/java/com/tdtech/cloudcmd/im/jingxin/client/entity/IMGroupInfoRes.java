package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/19
 * @Describe：
 */
@Data
public class IMGroupInfoRes {
    private Long id;

    private Boolean isDel;

    private String undefinedName;

    private String undefinedNameIds;

    private String name;

    private String avatar;

    private String gdn;

    private Integer type;

    private String introduction;

    private Integer applyJoinPolicy;

    private Integer inviteJoinPolicy;

    private Integer muteType;

    private Integer historyMsgPolicy;

    private Integer memberNum;

    private Long tag;

    private Long gmtCreated;

    private Long gmtModified;

    private List<GroupMembers> groupMembers;

    private List<GroupLabel> groupLabels;
}