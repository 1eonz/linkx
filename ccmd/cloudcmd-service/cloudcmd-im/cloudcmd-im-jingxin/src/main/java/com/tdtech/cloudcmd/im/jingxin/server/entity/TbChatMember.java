package com.tdtech.cloudcmd.im.jingxin.server.entity;

/**
 * @author ChinasoftPortal
 * @date 2025/9/18
 * @Describe：
 */
import lombok.Data;

import java.io.Serializable;

@Data
public class TbChatMember implements Serializable {
    private Long id;                 // 主键
    private String code;             // 人员编码（唯一）
    private String name;             // 用户姓名
    private String alias;            // 用户别名（昵称）
    private String tumbAvatar;       // 头像缩略图的base64编码
    private Integer gender;          // 性别(0-保密 1-男 2-女)
    private String genderName;       // 性别名称
    private String mobile;           // 手机号
    private String email;            // 邮箱
    private String isdn;             // 通讯号码
    private Long directLeaderId;     // 直属领导ID
    private Integer status;          // 人员状态(-1未激活 0-正常 1-离职)
    private String statusName;       // 人员状态名称
    private String userAlias;        // 用户在本群的昵称
    private String idcard;           // 身份证号
    private Integer role;            // 群组角色(0-成员 1-管理员 2-群主)
    private Integer muteType;        // 禁言类型(0-未设置 1-禁言 2-不禁言)
    private Integer joinType;        // 加群方式(0-初始 1-申请 2-扫码 3-邀请)
    private Long inviteId;           // 邀请人用户ID
    private Long inviteTime;         // 邀请时间戳
}