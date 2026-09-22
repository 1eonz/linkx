package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

/**
 * @author lsc
 * @date 2025/7/16
 **/
@Data
public class AddMember {

    private String userIdentity; // 用户标识
    private Integer idType; // 用户标识类型（0-userid，1-身份证号）
}