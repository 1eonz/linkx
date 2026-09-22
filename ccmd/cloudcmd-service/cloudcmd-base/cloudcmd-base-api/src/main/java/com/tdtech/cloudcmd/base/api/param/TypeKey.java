package com.tdtech.cloudcmd.base.api.param;

/**
 * @author mWX556161
 * @Date 2020/6/18 15:42
 */
public class TypeKey {

    public static final String BASE_CACHE_IS_INIT = "cloudcmd:base:cache:init";
    /**
     * 基础配置
     */
    public static final String ITEM_REDIS_KEY = "cloudcmd:base:item:{type}:{value}";
    public static final String TYPE_REDIS_KEY = "cloudcmd:base:type:{type}:list";
    public static final String GLOBALS_REDIS_KEY = "cloudcmd:base:globals:{name}";
    public static final String EXTEND_PROPERTIES_VALUE_KEY = "cloudcmd:base:extend:{code}:{name}";
    public static final String EXTEND_PROPERTIES_LIST_KEY = "cloudcmd:base:extend:{code}:list";
    /**
     * 事件类型
     */
    public static final String EVENT_TYPE = "30";
    /**
     * 事件级别
     */
    public static final String EVENT_LEVEL = "31";
    /**
     * 事件报告类型
     */
    public static final String EVENT_REPORT_TYPE = "32";
    /**
     * 事件报告方式
     */
    public static final String EVENT_REPORT_METHOD = "33";
    /**
     * 证件类型
     */
    public static final String CARD_TYPE = "34";
    /**
     * 事件状态
     */
    public static final String EVENT_STATE = "35";
    /**
     * 处置目标种类
     */
    public static final String TARGET_TYPE = "36";
    /**
     * 任务类型
     */
    public static final String MISSION_TYPE = "40";
    /**
     * 任务重要程度
     */
    public static final String MISSION_IMPORTANCE = "41";
    /**
     * 任务紧急程度
     */
    public static final String MISSION_URGENCY = "42";
    /**
     * 任务状态
     */
    public static final String MISSION_STATE = "43";
    /**
     * 预案级别
     */
    public static final String SCHEME_LEVEL = "44";
    /**
     * 预案试用区域
     */
    public static final String SCHEME_USE_AREA = "45";
    /**
     * 行动组成员类型
     */
    public static final String ACTION_GROUP_MEMBER_TYPE = "46";
    /**
     * 证据类型
     */
    public static final String EVIDENCE_TYPE = "47";
    /**
     * 流程种类
     */
    public static final String PROCESS_TYPE = "48";
    /**
     * 装备种类
     */
    public static final String EQUIPMENT_TYPE = "50";
    /**
     * 执行者种类
     */
    public static final String EXECUTOR_TYPE = "51";
    /**
     * 设施种类
     */
    public static final String FACILITY_TYPE = "52";
    /**
     * 权限种类
     */
    public static final String PERMISSION_TYPE = "53";
    /**
     * 区域类型
     */
    public static final String REGION_TYPE = "54";
    /**
     * 角色类型
     */
    public static final String ROLE_TYPE = "55";
    /**
     * 上报状态
     */
    public static final String LOCATION_REPORT_STATE = "56";
    /**
     * 上报类型
     */
    public static final String LOCATION_REPORT_TYPE = "57";
    /**
     * 定位方式
     */
    public static final String LOCATION_MODE = "58";
    /**
     * 第三方应用平台种类
     */
    public static final String THIRD_PLATFORM = "59";
    /**
     * 认证标志类型
     */
    public static final String AUTHENTICATION_TYPE = "60";

}
