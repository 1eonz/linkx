package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TxtMsgVo {
    /**
     * 文本消息内容。最大长度10000。1、支持内置表情格式：[emotion]或[:emotion]；2、支持[@userId:@name],在聊天群组中@指定人员；3、[@groupId:@all]，在聊天群组中@所有人
     */
    private String text;
    /**
     * 如果存在引用，填写引用消息的msgId。
     */
    private String srcMsgId;
}
