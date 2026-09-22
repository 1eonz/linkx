package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "摄像头层级查询对象")
public class CameraLevelQO extends CcmdPageParam {

    @Schema(description = "上级编号", example = "higher001")
    private String higherLevelNumber;
    
    @Schema(description = "根级编号", example = "root001")
    private String rootLevelNumber;


}
