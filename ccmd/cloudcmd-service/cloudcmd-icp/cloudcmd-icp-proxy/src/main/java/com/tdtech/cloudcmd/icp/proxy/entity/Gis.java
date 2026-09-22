package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.GisNotify;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString(callSuper = true)
@TableName("tb_gis_status")
@Schema(description = "GIS位置信息")
public class Gis extends GisNotify {

    @TableId
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "纬度")
    public BigDecimal lat;

    @Schema(description = "经度")
    public BigDecimal lon;

    @Schema(description = "海拔")
    public BigDecimal alt;

}
