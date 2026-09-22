package com.tdtech.cloudcmd.im.jingxin.client.entity;

import java.util.List;

import lombok.Data;

@Data
public class IMOfflineMsgPageVo {

    private Boolean isEnd;
    private List<IMOfflineMsgVo> imMsgs;

}
