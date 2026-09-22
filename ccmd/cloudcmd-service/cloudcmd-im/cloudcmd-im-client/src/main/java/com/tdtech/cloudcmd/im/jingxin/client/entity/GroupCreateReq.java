package com.tdtech.cloudcmd.im.jingxin.client.entity;

import java.util.List;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/16
 **/
@Data
public class GroupCreateReq {

    private String name; // 客户端指定群名称（可选，默认值为 "default"）
    private Integer type = 3; // 群类型，默认值为 1（群聊组）
    private String introduction; // 群简介
    private Integer muteType; // 群禁言状态（1-禁言，2-不禁言）
    private List<AddMember> addMembers; // 待添加的群成员列表（必选，至少携带 2 个成员）
    private Owner owner; // 创建协同群组时指定的群主（可选）
    private List<Long> labelIds; // 群组标签 ID 列表（当前仅支持协同群组）
}
