package com.tdtech.cloudcmd.auth.constant;

/**
 * @author mWX556161
 * @date 2020/11/23 15:49
 */
public interface OAuthConstant {

    /**
     * Value为用户登录对象。过期时间为access_token有效时间。
     */
    String ACCESS_TOKEN_USER_KEY = "icp-x:auth:token:{access_token}";

    String ACCESS_TMP_TOKEN_USER_KEY = "icp-x:auth:tmp-token:{access_token}";

    /**
     * 曾经存在的token
     */
    String ACCESS_TOKEN_TIMEOUT_KEY = "icp-x:auth:timeout:token:{access_token}";

    /**
     * 已经发送过token即将超时的Key
     */
    String ACCESS_TOKEN_WILL_TIMEOUT_KEY = "icp-x:auth:expire:token:{access_token}";

    /**
     * Value为用户登录对象。过期时间为refresh_token有效时间。
     */
    String REFRESH_TOKEN_USER_KEY = "icp-x:auth:refreshtoken:{refresh_token}";

    String REFRESH_TMP_TOKEN_USER_KEY = "icp-x:auth:tmp-refreshtoken:{refresh_token}";

    /**
     * Value为用户对象。永不过期。
     */
    String CLIENT_USER_KEY = "icp-x:auth:user:{userId}:client:{clientId}";

    /**
     * token最新请求时间 键
     */
    String ACCESS_TOKEN_REQUEST_LATEST_TIME_KEY = "icp-x:auth:latest:time:token:{access_token}";

    /**
     * token最新请求时间 键
     */
    String ACCESS_TOKEN_REQUEST_LATEST_TIME_EXIST_KEY = "icp-x:auth:latest:time:exist:token:{access_token}";

    /**
     * 用户锁定后自动解锁时长,单位为分钟
     */
    String AUTH_AUTO_UNLOCK_TIME = "cloudcmd:base:globals:AUTH_AUTO_UNLOCK_TIME";

    /**
     * 登录失败次数限制
     */
    String AUTH_LOGIN_ERROR_NUM_LIMIT = "cloudcmd:base:globals:AUTH_LOGIN_ERROR_NUM_LIMIT";

    /**
     * 首次登录是否强制修改密码
     */
    String AUTH_FIRSTLOGIN_FORCE_CHANGEPWD = "cloudcmd:base:globals:AUTH_FIRSTLOGIN_FORCE_CHANGEPWD";

    /**
     * 密码有效期，单位为天
     */
    String AUTH_PWD_VALIDITY_PERIOD = "cloudcmd:base:globals:AUTH_PWD_VALIDITY_PERIOD";

    /**
     * Token有效期，单位小时
     */
    String AUTH_TOKEN_EXPIRE_IN = "cloudcmd:base:globals:AUTH_TOKEN_EXPIRE_IN";

    /**
     * RefreshToken有效期，单位天
     */
    String AUTH_REFRESHTOKEN_EXPIRE_IN = "cloudcmd:base:globals:AUTH_REFRESHTOKEN_EXPIRE_IN";

    /**
     * 口令有效期,单位分钟
     */
    String AUTH_USER_OFFLINE_TIME = "cloudcmd:base:globals:AUTH_USER_OFFLINE_TIME";

    /**
     * 是否允许多端登录
     */
    String AUTH_MULTI_END_LOGIN_ALLOWED = "cloudcmd:base:globals:AUTH_MULTI_END_LOGIN_ALLOWED";

    /**
     * dems的tokenKey
     */
    String AUTH_DEMS_USER_TOKEN = "dems:user:token:{access_token}";

    /**
     * 以下用于组装消息
     */
    Short MSG_NOTIFY_TYPE = 0x0002;

    /**
     * 用户单播
     */
    Short MUNICAST_NOTIFY_TYPE = 0x0000;

    Short MSG_KICKOUT_TYPE = 0x0028;

    Short TYPE_BIZ_TOKEN_REFRESH = 0x0005;

    String AUTH_SUB_SYSTEM = "PASSPORT";

    String MODEL_NAME_AUTH = "PASSPORT";

    String NOTIFY_EIGHTH_TIMEOUT = "token_eighth_timeout";

    String NOTIFY_TWELFTH_TIMEOUT = "token_twelfth_timeout";

    String NOTIFY_KICKOUT = "kickout";

    String NOTIFY_OFFLINE = "offline";

    String NOTIFY_LOCKSCREEN = "lockScreen";

    String NOTIFY_TOKEN_REFRESH = "tokenRefresh";
}
