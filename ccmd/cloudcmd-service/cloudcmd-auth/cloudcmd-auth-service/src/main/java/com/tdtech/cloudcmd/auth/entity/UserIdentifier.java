package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户登录标识（内部）信息表。扩展登录信息。如使用email.ISDN等。
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class UserIdentifier implements Serializable {

    public static final String USER_ID = "user_id";
    public static final String IDENTITY_TYPE = "identity_type";
    public static final String IDENTIFIER = "identifier";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 资源关联的用户ID.( 资源可以开户，也可以不开户)
     */
    private Long userId;
    /**
     * 认证标识类型 0- 所有 1-警号 2-域用户名
     */
    private String identityType;
    /**
     * 认证标识
     */
    private String identifier;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 更新时间
     */
    private Date gmtModified;

}
