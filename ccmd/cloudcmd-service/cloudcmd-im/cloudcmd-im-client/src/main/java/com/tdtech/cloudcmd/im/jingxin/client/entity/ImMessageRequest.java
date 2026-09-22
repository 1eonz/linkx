package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ImMessageRequest<T> {
    /**
     * 消息分类：1-点对点消息；2-群组消息；4-多人转发
     */
    private Integer category;
    /**
     * 消息类型。1-文本消息；2-彩信消息；5-名片
     */
    private Integer msgType;
    /**
     * 主叫ID。点对点、群组消息中填写用户ID或代理用户ID
     */
    private Long from;
    /**
     * 被叫id，可选。category取值为1、2时必选。取值为1时为用户身份证号；取值为2时为群组id。category取值为4时不填。
     */
    private String to;
    /**
     * 多人转发列表。category取值为4时必选
     */
    private List<ForwardObj> forwardList;

    /**
     * 是否明文消息。不带(或带0)且为文本（或公众号、合并转发）消息，表示消息为密文；不带(或带0)且非文本（或公众号、合并转发）消息，表示消息为明文；0-密文；1-明文，表示消息为明文；默认不带。若为明文消息，必带且填写为1。
     */
    private Integer plaintext;

    /**
     * 0或不填，表示虚拟号码；1-身份证号码。
     * 不填。
     */
    private Integer fromIdType;

    /**
     * 0或不填，表示虚拟号码；1-身份证号码。
     * category取值为1，必须填为1
     */
    private Integer toIdType;
    /**
     * 主叫发送上行消息的重试次数。
     */
    private Integer retryCount;
    /**
     * 该消息是否为重发消息。0-非重发消息；1-重发消息。与retryCount配合识别wespace重发场景：a.isResend=0时，retryCount表示首次发送时重试次数；b.isResend=1时，retryCount表示重发时的重试次数。
     */
    private Boolean isResend;
    /**
     * 主叫请求时携带的客户端分配的msgId。最大长度:100
     */
    private String cMsgId;
    /**
     * 消息内容对象。当msgType为1时为TxtMsgVo；当msgType为5时为CardMsgVo；当msgType为11时为NotifyMsgVo
     */
    private T msg;

}
