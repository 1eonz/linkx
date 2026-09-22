package com.tdtech.cloudcmd.admin.resource.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 */
@Schema(description = "应用创建/更新 Request VO")
@Data
@ToString(callSuper = true)
public class AppInfoSaveReqVO {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "示例应用")
    @NotNull(message = "应用名称不能为空")
    private String name;

    @Schema(
            description = "应用链接",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://example.com")
    private String url;

    @Schema(description = "安卓原生应用的包名", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "com.example.app")
    private String packageAndroid;

    @Schema(description = "鸿蒙应用的包名", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "com.example.hm")
    private String packageHm;

    @Schema(description = "鸿蒙应用时需要填写的activity name", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "EntryAbility")
    private String activity;

    @Schema(description = "应用ID", example = "xxxx")
    private String appId;

    @Schema(description = "跳转参数", example = "{name:'*',query:{key:value}}")
    private String params;

    @Schema(description = "应用图标", requiredMode = Schema.RequiredMode.REQUIRED, example = "icon.png")
    @NotNull(message = "应用图标不能为空")
    private String icon;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "排序值不能为空")
    private Integer sort;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用不能为空")
    private Integer status;

    @Schema(description = "应用类型", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "0")
    private Integer type;

    @Schema(description = "应用区域", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "2")
    private Integer zone;

    @Schema(description = "前置应用ID", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private Long prerequisite;

    // 小乔智能体相关字段
    @Schema(
            description = "应用别名（面向接口调用。在小乔智能体里面标识“意图名称”）",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "示例应用")
    private String alias;

    @Schema(
            description = "RESTful应用，对接的服务类型。（1：小乔智能体）",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "1")
    private Integer service;

    @Schema(
            description = "RESTful应用JSON参数，接口设计参考OpenAI的function calling参数定义",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "{name:'*',query:{key:value}}")
    private String headers;

    @Schema(
            description = "RESTful应用JSON参数，接口设计参考OpenAI的function calling参数定义",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "{name:'*',query:{key:value}}")
    private String body;

    @Schema(
            description = "RESTful应用JSON响应，接口设计参考OpenAI的function calling参数定义",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "{name:'*',query:{key:value}}")
    private String response;

    @Schema(description = "小乔智能体的垂域", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private String domain;

    @Schema(
            description = "小乔智能体的接口版本",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "1")
    private String version;

    @Schema(description = "接口描述", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private String description;

    @Schema(description = "0：否；1：是", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private Integer official;

    @Schema(description = "应用展示范围。1：鸿蒙移动端；2：安卓移动端；4：PC浏览器；8：PC桌面端", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1,2,4,8 ")
    private List<Integer> scope;
}
