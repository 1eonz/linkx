package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class UserStatVo {

    private Integer bindUserNum; // 绑定人员数
    private Integer supportGroupNum; // 正在支撑群组数
    private Integer supportUserNum; // 正在支撑人员数（1个人员可能同时支撑多个群组）
}