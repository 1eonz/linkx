package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class WsUserStateNoticeMessage {
    // 批量变更消息的最后修改标识
    private Long etag;
    // 批量人员状态通知对象。每次最大50条
    private List<WsUserStateEvents> userStateEvents;
}
