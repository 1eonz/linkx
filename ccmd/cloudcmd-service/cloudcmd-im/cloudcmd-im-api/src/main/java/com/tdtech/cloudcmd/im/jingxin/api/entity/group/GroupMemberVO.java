package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "群组成员视图对象")
public class GroupMemberVO implements Serializable {

    @Schema(description = "主键")
    private Long id;                 // 主键
    
    @Schema(description = "人员编码（唯一）")
    private String code;             // 人员编码（唯一）
    
    @Schema(description = "用户姓名")
    private String name;             // 用户姓名
    
    @Schema(description = "用户别名（昵称）")
    private String alias;            // 用户别名（昵称）
    
    @Schema(description = "头像缩略图的base64编码")
    private String tumbAvatar;       // 头像缩略图的base64编码
    
    @Schema(description = "性别(0-保密 1-男 2-女)")
    private Integer gender;          // 性别(0-保密 1-男 2-女)
    
    @Schema(description = "性别名称")
    private String genderName;       // 性别名称
    
    @Schema(description = "手机号")
    private String mobile;           // 手机号
    
    @Schema(description = "邮箱")
    private String email;            // 邮箱
    
    @Schema(description = "通讯号码")
    private String isdn;             // 通讯号码
    
    @Schema(description = "直属领导ID")
    private Long directLeaderId;     // 直属领导ID
    
    @Schema(description = "人员状态(-1未激活 0-正常 1-离职)")
    private Integer status;          // 人员状态(-1未激活 0-正常 1-离职)
    
    @Schema(description = "人员状态名称")
    private String statusName;       // 人员状态名称
    
    @Schema(description = "用户在本群的昵称")
    private String userAlias;        // 用户在本群的昵称
    
    @Schema(description = "身份证号")
    private String idcard;           // 身份证号
    
    @Schema(description = "群组角色(0-成员 1-管理员 2-群主)")
    private Integer role;            // 群组角色(0-成员 1-管理员 2-群主)
    
    @Schema(description = "禁言类型(0-未设置 1-禁言 2-不禁言)")
    private Integer muteType;        // 禁言类型(0-未设置 1-禁言 2-不禁言)
    
    @Schema(description = "加群方式(0-初始 1-申请 2-扫码 3-邀请)")
    private Integer joinType;        // 加群方式(0-初始 1-申请 2-扫码 3-邀请)
    
    @Schema(description = "邀请人用户ID")
    private Long inviteId;           // 邀请人用户ID
    
    @Schema(description = "邀请时间戳")
    private Long inviteTime;         // 邀请时间戳

}
