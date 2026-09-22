package com.tdtech.cloudcmd.gateway.constant;

/**
 * @author zhuangzl
 * @date 2020-08-08 12:45
 */
public class GatewayConstant {

    public static final String HEADER_NETWORK = "X-CloudCmd-Network";

    public static final String HEADER_REMOTE = "X-CloudCmd-RemoteIp";

    public static final String CONTENT_TYPE = "Content_Type";

    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    /**
     * value 为超级管理员
     */
    public static final Integer SUPPERADMIN = 505001;

    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Value为用户登录对象。
     */
    public static final String ACCESS_TOKEN_USER_KEY = "icp-x:auth:token:{access_token}";

    public static final String ACCESS_TMP_TOKEN_USER_KEY = "icp-x:auth:tmp-token:{access_token}";
    /**
     * token最新请求时间 键
     */
    public static final String ACCESS_TOKEN_REQUEST_LATEST_TIME_KEY = "icp-x:auth:latest:time:token:{access_token}";

    /**
     * token超时时间
     */
    /**
     * Token有效期，单位小时
     */
    public static final String ACCESS_TOKEN_REQUEST_LATEST_TIME_PERIOD = "cloudcmd:base:globals:AUTH_TOKEN_EXPIRE_IN";
}
