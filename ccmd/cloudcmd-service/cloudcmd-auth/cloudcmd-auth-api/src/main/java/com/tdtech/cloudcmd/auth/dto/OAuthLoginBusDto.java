package com.tdtech.cloudcmd.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author michstabe
 * @date 2022/8/19 15:41
 * @description
 */
@Data
public class OAuthLoginBusDto implements Serializable {
    /**
     * 用户id
     */
    private String userId;

    /**
     * 组织id
     */
    private String orgId;

    /**
     * 身份证号
     */
    private String sfzh;

    /**
     * 警号
     */
    private String jh;

    /**
     * 姓名
     */
    private String xm;

    /**
     * 扩展信息
     */
    private String exten;
}
