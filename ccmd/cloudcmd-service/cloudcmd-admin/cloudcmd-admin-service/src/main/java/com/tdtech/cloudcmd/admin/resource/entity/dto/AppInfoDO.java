package com.tdtech.cloudcmd.admin.resource.entity.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
public class AppInfoDO {

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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

    /** 前置应用ID */
    private Long prerequisite;

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

    /** 小乔智能体的垂域 */
    private String domain;

    /** 小乔智能体的接口版本 */
    private String version;

    /** 接口描述 */
    private String description;

    /** 0：否；1：是 */
    private Integer official;

    private Integer isDeleted;

    private Integer scope;

    private List<Integer> scopeList;
}
