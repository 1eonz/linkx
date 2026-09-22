package com.tdtech.cloudcmd.admin.resource.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@TableName("app_info")
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppInfo {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String APPID = "app_id";
    public static final String URL = "uri";
    public static final String PACKAGE_ANDROID = "package_android";
    public static final String PACKAGE_HM = "package_hm";
    public static final String ACTIVITY = "activity";
    public static final String PARAMS = "params";
    public static final String ICON = "icon";
    public static final String CREATE_TIME = "create_time";
    public static final String SORT = "sort";
    public static final String STATUS = "status";
    public static final String UPDATE_TIME = "update_time";
    public static final String ZONE = "zone";
    public static final String PRE_REQUISITE = "prerequisite";
    public static final String IS_DELETED = "is_deleted";
    public static final String TYPE = "type";

    /**
     * 应用编号
     */
    @TableId
    private Long id;
    /**
     * 应用名称
     */
    private String name;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用类型 0:H5应用（default）；1:原生App；2：RESTful应用；3:前置应用
     */
    private Integer type;

    /**
     * 应用链接
     */
    @TableField(value = "uri")
    private String url;

    @TableField(value = "package_android")
    private String packageAndroid;

    @TableField(value = "package_hm")
    private String packageHm;

    private String activity;

    /**
     * 跳转参数
     */
    private String params;
    /**
     * 应用图标
     */
    private String icon;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 状态：0-上架，1-下架
     */
    private Integer status;

    /**
     * 1:一类区；2（default）:二类区(移动信息网)；3：三类区（公安网）
     */
    private Integer zone;

    /**
     * 前置应用ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long prerequisite;

    /** 是否删除。0：否（default）；1：是 */
    private Integer isDeleted;

    /** 应用别名（面向接口调用。在小乔智能体里面标识‘意图名称’） */
    private String alias;

    /** RESTful应用，对接的服务类型。（1：小乔智能体） */
    private Integer service;

    /** RESTful应用JSON参数，接口设计参考OpenAI的function calling参数定义 */
    private String headers;

    /** RESTful应用JSON参数，接口设计参考OpenAI的function calling参数定义 */
    private String body;

    /** RESTful应用JSON响应，接口设计参考OpenAI的function calling参数定义 */
    private String response;

    /**
     * 小乔智能体的垂域
     */
    private String domain;

    /**
     * 小乔智能体的接口版本
     */
    private String version;

    /**
     * 接口描述
     */
    private String description;

    /** 0：否；1：是 */
    private Integer official;

    /**应用展示范围。1：鸿蒙移动端；2：安卓移动端；4：PC浏览器；8：PC桌面端*/
    private Integer scope;
}
