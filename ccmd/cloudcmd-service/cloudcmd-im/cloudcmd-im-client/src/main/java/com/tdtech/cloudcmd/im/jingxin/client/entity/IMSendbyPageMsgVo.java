package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author: S063874
 * @date: 2026-03-18 13:54
 */
@Data
public class IMSendbyPageMsgVo {

    private Integer isEnd;
    private List<IMSendbyMsgVo> imMsgs;

    @Data
    public static class IMSendbyMsgVo{
        // 消息id
        private Long msgId;
        // 消息分类
        private Integer category;
        // 消息类型
        private Integer msgType;
        // sessionId
        private Long sessionId;
        // 消息子类型
        private Integer subType;
        // 消息发送时间
        private Date time;
    }
}
