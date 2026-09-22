package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/11
 * @Describe：
 */
@Data
public class UserCareGroupDTO {
    private Long id;         // 对应tgc.id（关注记录ID）
    private Long userId;         // 用户ID
    private Long archivedUserId;
    private String userName;
    private Long groupId;        // 群组ID
    private String groupName;
    private Integer archived;    // 归档状态（来自tb_group_extends）
    private LocalDateTime gmtCreated; // 创建时间
    private LocalDateTime archivedTime; // 归档时间
    private String tagName;     // 聚合的标签名称（如"标签1,标签2"）
    private Integer isCare;
    /**
     * 群头像路径
     */
    private String avatarImg;

    private Integer polTicketCnt;

    private Integer tasksCnt;

    private Integer groupType;
    /**
     * 是否是群主
     */
    private Integer isOwner;
    /**
     * 群主id
     */
    private Long ownerId;
    /**
     * 是否是群成员
     */
    private Integer isMember;
    /**
     * 群成员
     */
    @JsonIgnore
    private String members;

    @JsonIgnore
    private List<Long> memberIds;

    /**
     * 是否有协同岗
     */
    private Integer hasCoopUser;

    /**
     * 1:我创建的；3：我可查看的但不是成员；4：我是成员；5：我的所有
     */
    private Integer scope;
}
