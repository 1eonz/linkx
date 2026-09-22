package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author: S063874
 * @date: 2026-03-18 13:54
 */
@Data
public class IMSendbyMsgReq {

    // 用户id
    private Long userId;
    // 开始时间
    private Date beginTime;
    // 结束时间
    private Date endTime;
    // //消息类型。1-文本消息；2-彩信消息；3-位置消息；5-名片；6-群接龙；8-合并转发；9-逐条转发
    private Integer msgType;
    // 群组消息
    private Integer category = 2;
    // 最大返回记录
    private Integer limit;

}
