package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WsTextMessage {

    private Integer category;
    private String clientMsgId;
    private Boolean forwardMsg;
    private Long from;
    private Integer fromIdType;
    private String fromIsdn;
    private Boolean fromType;
    // 主叫真实用户id。仅在category取值为2(群聊消息)，且主叫为协同岗时填写。此时from填写为协同岗虚拟id。
    private Long fromRealUserId;
    private WsMsg msg;
    private String msgId;
    private Integer msgType;
    private Boolean oneByOneMsg;
    private Integer plaintext;
    private Boolean read;
    private Long seq;
    private Long sessionSeqId;
    private Long time;
    private String to;
    private Integer toIdType;
    private String toIsdn;
    private Boolean toType;
    private Boolean withdraw;

}
