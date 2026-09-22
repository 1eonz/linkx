package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

/**
 * @author ChinasoftPortal
 * @date 2025/9/19
 * @Describe：
 */
@Data
public class MessagesOfflineReq {
    /**
     * 当前登录用户ID
     */
    private Long userId;

    /**
     * 排序方式
     * 0：倒序（从新到旧）
     * 1：正序（从旧到新）
     */
    private Integer type;

    /**
     * 会话ID
     * 群聊场景填写群组ID
     */
    private String sessionId;

    /**
     * 查询起始序列号
     */
    private Integer fromSessionSeqId;

    /**
     * 查询结束序列号
     */
    private Integer toSessionSeqId;

    /**
     * 是否明文返回
     * 1：明文返回查询结果
     */
    private Integer plaintext;

    /**
     * 最大返回条数
     */
    private Integer limit;

    /**
     * 数据范围
     * 1：平台所有数据范围
     */
    private Integer scope;

    /**
     * 消息分类
     * 2：群聊
     */
    private Integer category;
}
