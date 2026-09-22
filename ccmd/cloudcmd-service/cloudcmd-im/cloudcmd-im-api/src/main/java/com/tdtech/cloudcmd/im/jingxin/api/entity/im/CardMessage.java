package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "卡片消息内容")
public class CardMessage implements Serializable {

    @Schema(description = "级别。blue/orange/yellow/red(一般/关键/重要/紧急)")
    private String level;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型 0-三方卡片(默认值) 1-任务卡片")
    private Integer type = 0;

    @Schema(description = "任务类型名称(type=1必填)")
    private String taskTypeName;

    @Schema(description = "时间戳 精确到秒(type=1必填)")
    private String time;

    @Schema(description = "描述")
    private String describe;

    @Schema(description = "缩略图(base64格式,type=1暂时无效不予展示)")
    private String thumb;

    @Schema(description = "跳转url")
    private String url;

    @Schema(description = "跳转类型 (1-普通url 2-全屏url 3-小程序)")
    private Integer jumpType;

    @Schema(description = "级别名，可不填，不填时不显示(最大长度6，超过截取前6个字符)")
    private String levelName;
}