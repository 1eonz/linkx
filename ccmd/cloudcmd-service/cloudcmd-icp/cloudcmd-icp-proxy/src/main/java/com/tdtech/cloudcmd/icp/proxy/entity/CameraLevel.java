package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.icp.proxy.client.entity.CameraLevelResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString(callSuper = true)
@TableName("tb_camera_level")
@Schema(description = "摄像头层级信息")
public class CameraLevel extends CameraLevelResp {

    @TableId
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "层级编号路径", example = "001.002.003")
    private String levelNumberPath;

    @Schema(description = "创建时间")
    private Date createTime;
}
