package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

/**
 * @author lsc
 * @date 2025/7/25
 **/
@Data
public class GroupVo {
    private Long id;
    private Boolean isDel;
    private String undefinedName;
    private String undefinedNameIds;
    private String name;
    private String avatar;
    // 群类型 1-群聊组 ; 3-协同群组。
    private Integer type;
    private String introduction;
    private Integer muteType;
    private Integer memberNum;
    private Long tag;
    private Long gmtCreated;
    private Long gmtModified;
    private Long ownerId;
    private String ownerIdCard;
    // 文档里没有写返回这个字段及其值的说明，2应该是已冻结
    private Integer status;
    private List<GroupMembers> groupMembers;
}
