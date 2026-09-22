package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

@Data
public class GroupLocationVO {

    private Long userId;

    // 位置：经纬度，格式：经度,纬度
    private String location;
}
