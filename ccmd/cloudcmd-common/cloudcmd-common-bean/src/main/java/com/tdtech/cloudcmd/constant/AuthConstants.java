package com.tdtech.cloudcmd.constant;

/**
 * @author zhuangzl
 * @date 2020-08-09 14:23
 */
public class AuthConstants {
    public static final String ACCESS_TOKEN_USER_KEY = "icp-x:auth:token:";

    /**
     * Value为用户登录对象。过期时间为access_token有效时间。
     */
    public static final String ACCESS_TOKEN_USER_KEYS = "icp-x:auth:token:{access_token}";
    /**
     * value为用户权限
     */
    public static final String ACCESSS_PERMISSION = "cloudcmd:auth:access_to_permission:";

    /**
     * 组织权限列表
     */
    public static final String ACCESSS_PERMISSION_ORG = "cloudcmd:org:access_to_permission:";

    /**
     * value为用户权限
     */
    public static final String ACCESSS_ROLE_CHANGE_STATE = "cloudcmd:status:change_to_role:";

    /**
     * 菜单权限列表
     */
    public static final String ACCESSS_PERMISSION_MENU = "cloudcmd:menu:access_to_permission:";

    /**
     * 功能权限列表
     */
    public static final String ACCESSS_PERMISSION_ACT = "cloudcmd:act:access_to_permission:";

    /**
     * 所有功能权限列表
     */
    public static final String ACCESSS_PERMISSION_ACT_ALL = "cloudcmd:act_all:access_to_permission:";


    public static final String ACCESSS_PERMISSION_UPDATE_MENU = "cloudcmd:menu:update_to_permission:";

    public static final String DATA_PRIV_TREE_CACHE = "cloudcmd:auth:data_priv_tree:";
    public static final String DATA_PRIV_TREE_LOCK = "cloudcmd:auth:data_priv_tree_lock:";

    public static final String ON_LINE_USER = "cloudcmd:auth:on_line_user:";
    public static final long ON_LINE_USER_EXPIRATION_TIME = 5 * 5L;
    /**
     * 角色类型
     */
    public static final int SUPPERADMIN = 505001;
    public static final Integer ADMIN = 505002;
    public static final int GENERAL = 505003;
}