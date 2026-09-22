package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.icp.proxy.client.entity.CameraResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString(callSuper = true)
@TableName("tb_camera")
@Schema(description = "摄像头信息")
public class Camera extends CameraResp {

    @TableId
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "纬度")
    private BigDecimal lat;

    @Schema(description = "经度")
    private BigDecimal lon;

    @Schema(description = "海拔")
    private BigDecimal alt;
}
