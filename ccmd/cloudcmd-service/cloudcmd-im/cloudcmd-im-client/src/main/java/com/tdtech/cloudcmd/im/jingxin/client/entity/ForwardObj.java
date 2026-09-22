package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ForwardObj {
    /**
     * 消息分类：1-单聊消息；2-群聊消息; 4-多人转发
     */
    private Integer category;
    /**
     * 转发id。如果category取值1，为用户id；取值2，为群组id；
     */
    private Long toObjId;
    /**
     * 0或不填，表示虚拟号码；1-身份证号码。
     * 当toObjId是身份证号码时，必填为1
     */
    private Integer toIdType;
}
