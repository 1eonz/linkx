package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

/**
 * 系统应用授权信息 VO（新表 linkx_open.tb_application_grant 对应入参/出参）
 */
@Data
public class ApplicationGrantVO {

    /**
     * ID
     */
    private Long id;
    /**
     * linkx_auth.tb_application.id
     */
    private Long applicationId;
    /**
     * 被授权组织名称
     */
    private String systemName;
    /**
     * 颁发给三方的组织编码
     */
    private String systemCode;
    /**
     * 颁发给三方的client_id
     */
    private String clientId;
    /**
     * 颁发给三方的client_secret
     */
    private String clientSecret;
    /**
     * 过期时间；null 表示永久
     */
    private Date expired;
    /**
     * Token有效期。-1为永不过期，单位秒
     */
    private Long tokenTime;
    /**
     * 应用类型
     */
    private String clientType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 刷新token有效期
     */
    private Integer refreshTokenTime;
    /**
     * 授权人
     */
    private Long grantUserId;
    /**
     * 授权人姓名
     */
    private String grantUserName;
    /**
     * 授权时间
     */
    private Date grantTime;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}