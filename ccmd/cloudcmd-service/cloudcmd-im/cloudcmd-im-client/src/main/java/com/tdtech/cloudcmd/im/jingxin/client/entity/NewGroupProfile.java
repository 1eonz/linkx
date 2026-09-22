package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

/**
 * @author lsc
 * @date 2025/9/26
 **/
@Data
public class NewGroupProfile {
    private String name;
    private String undefinedName;
    private String undefinedNameIds;
    private Integer muteType;
    private String avatar;
    private Integer applyJoinPolicy;
    private List<GroupLabel> groupLabels;
    private Long gmtCreated;
    private Long gmtModified;
}
