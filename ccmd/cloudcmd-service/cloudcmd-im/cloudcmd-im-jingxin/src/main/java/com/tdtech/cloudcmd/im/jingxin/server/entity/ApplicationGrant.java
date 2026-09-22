package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统应用授权信息表
 *
 * 对应 linkx_open.tb_application_grant，由原 icp_collabs.tb_collaboration_client 迁移而来
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("linkx_open.tb_application_grant")
public class ApplicationGrant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
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
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
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
     * 修改时间
     */
    private Date gmtModified;
    /**
     * 授权人
     */
    private Long grantUserId;
    /**
     * 授权时间
     */
    private Date grantTime;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 授权人姓名（非表字段）
     */
    @TableField(exist = false)
    private String grantUserName;
}