package com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tdtech.cloudcmd.util.json.JsonObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
@ToString
@TableName("tb_police_ticket")
@Schema(description = "警单实体类")
public class PoliceTicket  implements Serializable {

    @TableId
    @NotNull
    @Schema(description = "主键ID")
    private Long id;

    @NotNull
    @Schema(description = "标签")
    private String tag;

    @NotNull
    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "原始数据")
    private JsonObject origin;

    // 名称
    @NotNull
    @Schema(description = "名称")
    private String name;

    // 单号
    @NotNull
    @Schema(description = "单号")
    private String code;

    // 三方系统
    @NotNull
    @Schema(description = "三方系统")
    private String source;

    @NotNull
    @Schema(description = "内容")
    private String content;

    @Schema(description = "创建时间")
    private Date createTime;

    @NotNull
    @Schema(description = "三方系统编码")
    private String systemCode;

    @NotNull
    @Schema(description = "三方系统名称")
    private String systemName;

    @NotNull
    @Schema(description = "派发人")
    private String dispatcher;
}
