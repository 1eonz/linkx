package com.tdtech.cloudcmd.auth.constant;

/**
 * @author zhuangzl
 * @date 2020-06-02 09:18
 */
public interface AuthBaseConstant {
    String USER_INFO = "cloudcmd:auth:user_info:user_id:";
    String AUTH_INFO = "cloudcmd:auth:auth_info:user_id:";
    String USER_TO_TOKEN = "cloudcmd:auth:token:user_id:";
    String TOKEN_TO_USER = "cloudcmd:auth:user_id:token:";
    String TOKEN_POOL = "cloudcmd:auth:token_pool";
    String USER_POOL = "cloudcmd:auth:user_pool";

    String CDC_2000 = "CDC-2000";// 云指挥后台管理平台
    String DEMS_WEB = "DEMS-WEB";// dems-web平台
    String PWD_AUTH_TYPE = "password";
    String H5_TYPE = "H5";
    String IM_TOKEN_TYPE = "H5";
    /**
     * icc
     */
    String CDC_1000 = "CDC-1000";

    /**
     * CAPP
     */
    String CAPP_1000 = "CAPP-1000";

    Integer SEAT_CATEGORY = 500003;

    Integer CAPP_CATEGORY = 500006;
    Long CAPP_TYPE_ID = 10L;
}
