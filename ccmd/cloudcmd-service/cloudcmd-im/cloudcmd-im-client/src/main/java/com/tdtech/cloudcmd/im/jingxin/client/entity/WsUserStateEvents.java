package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WsUserStateEvents {
    // 操作类型
    private Integer operateType;
    // 状态 .0-离线 1-在线 2-忙碌 3-离开
    private Integer state;
    // 状态名称。.0-离线 1-在线 2-忙碌 3-离开
    private String stateName;
    // 人员ID
    private Long userId;
    // 身份证号码。与userId必须二者选一。不能全部为空
    private String idCard;
}
