package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
public class WsCollaborationChangeMember {

    /**
     * 用户ID
     */
    @NotNull
    private Long userId;

    /**
     * False-正常 True-已删除，该用户成员已被删除
     */
    @NotNull
    private Boolean isDel;

    /**
     * 用户姓名，变更群组服务不推送
     */
    @NotNull
    private String name;

    /**
     * 用户别名
     */
    private String userAlias;

    /**
     * 成员用户禁言状态，0-未设置 1-禁言 2-可言
     */
    private Integer muteType;

    /**
     * 0-普通成员1-群管理员2-群主
     */
    @NotNull
    private Integer role;

    /**
     * 加群方式，0-主动加群 1-二维码加群 2-他人邀请
     */
    private Integer joinType;

    /**
     * 邀请人用户ID
     */
    private Long inviteId;

    /**
     * 成员邀请时间戳
     */
    private Long inviteTime;
    /**
     * 记录创建时间戳
     */
    private Long gmtCreated;
    /**
     * 变更时间戳
     */
    private Long gmtModified;

}
