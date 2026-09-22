package com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@TableName("tb_police_ticket_client")
@Schema(description = "警单对接配置")
public class PoliceTicketClient  implements Serializable {

    @TableId
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "配置名称", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "所属系统", maxLength = 50)
    private String systemName;

    @Schema(description = "系统编码", maxLength = 50)
    private String systemCode;

    @TableField("`schema`")
    @Schema(description = "访问协议", allowableValues = {"http", "https"}, maxLength = 10)
    private String schema;

    @Schema(description = "IP", maxLength = 50)
    private String ip;

    @Schema(description = "端口", minimum = "1", maximum = "65535")
    private Integer port;

    @Schema(description = "API路径", maxLength = 200)
    private String path;

    @Schema(description = "HTTP方法", allowableValues = {"GET", "POST", "PUT", "DELETE"})
    private String method;

    @Schema(description = "HTTP请求头信息")
    private String headers;

    @Schema(description = "URL查询参数")
    private String params;

    @Schema(description = "请求体内容，JSON格式")
    private String body;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建时间")
    private Date gmtCreated;

    @Schema(description = "数据处理脚本，用于解析响应或转换数据", maxLength = 2000)
    private String script;

    @Schema(description = "执行周期，定时任务的执行间隔（毫秒）")
    private Long executePeriod;

    @Schema(description = "状态：（0 启用 1 禁用）")
    private Integer status;
}
