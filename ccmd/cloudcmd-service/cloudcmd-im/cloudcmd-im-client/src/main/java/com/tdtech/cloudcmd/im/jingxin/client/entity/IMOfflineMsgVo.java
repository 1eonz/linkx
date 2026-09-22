package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

@Data
public class IMOfflineMsgVo {
    private String transId;
    private String module;
    private String notifyType;//域内消息类型，必选。文本-TEXT_MSG；彩信-MEDIA_MSG；回执-ACK_READ；名片-NAME_CARD；接龙-GROUP_NOTE；撤回-WITHDRAW_MSG；合并转发-MERGE_FORWARD；
    private IMOfflineMsgItemVo data;
}
