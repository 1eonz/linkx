package com.tdtech.cloudcmd.linkx.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 群组消息统计表
 *
 * @author: S063874
 * @date: 2026-03-10
 */
@TableName("tb_group_msg_send_static")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMsgSendStatic {

    /**
     * 消息ID
     */
    @TableId
    private Long msgId;

    /**
     * SESSIONID
     */
    private Long sessionId;

    /**
     * 用户id
     */
    private Long userId;


    /**
     * 部门id
     */
    private Long departmentId;

    /**
     * 消息时间
     */
    private Date msgTime;

    /**
     * 消息类型
     */
    private Integer category;

    /**
     * 子消息类型
     */
    private Integer msgType;

    /**
     * 创建时间
     */
    private Date gmtCreated;
}