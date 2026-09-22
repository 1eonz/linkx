package com.tdtech.cloudcmd.icp.proxy.ws.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "摄像头层级通知信息")
public class CameraLevelNotify {

    @JsonProperty("highLevelNumber")
    @Schema(description = "上级编号", example = "001")
    private String highLevelNumber;

    @JsonProperty("level")
    @Schema(description = "层级", example = "1")
    private String level;

    @JsonProperty("levelNumber")
    @Schema(description = "层级编号", example = "001")
    private String levelNumber;

    @JsonProperty("nodeName")
    @Schema(description = "节点名称", example = "摄像头层级1")
    private String nodeName;

    @JsonProperty("servermode")
    @Schema(description = "服务器模式", example = "master")
    private String servermode;
}
