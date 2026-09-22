package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 第三方用户授权登录信息表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class UserAuths implements Serializable {

    public static final String USER_ID = "user_id";
    public static final String PLATFORM = "platform";
    public static final String OPEN_ID = "open_id";
    public static final String OPEN_NAME = "open_name";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String EXPIRE_IN = "expire_in";
    public static final String LOGIN_TIME = "login_time";
    public static final String EXPIRE_TIME = "expire_time";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 资源关联的用户ID.( 资源可以开户，也可以不开户)
     */
    private Long userId;
    /**
     * 第三方应用。WEIXIN,域账户、第三方账户名。
     */
    private String platform;
    /**
     * 第三方应用唯一ID
     */
    private String openId;
    /**
     * 第三方会员名称
     */
    private String openName;
    /**
     * access_token
     */
    private String accessToken;
    /**
     * access_token
     */
    private String refreshToken;
    /**
     * 有效期（秒数）
     */
    private Integer expireIn;
    /**
     * 登录时间
     */
    private Date loginTime;
    /**
     * 过期时间
     */
    private Date expireTime;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 更新时间
     */
    private Date gmtModified;
}
