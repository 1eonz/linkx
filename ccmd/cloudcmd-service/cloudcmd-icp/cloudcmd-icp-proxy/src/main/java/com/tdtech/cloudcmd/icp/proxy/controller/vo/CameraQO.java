package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "摄像头查询对象")
@Validated
public class CameraQO extends CcmdPageParam {

    @Schema(description = "是否在线", example = "true")
    private Boolean isOnline;

    @Schema(description = "排序 true 在线的放前边 false 放后边", example = "true")
    private Boolean onlineFirst;

    @Schema(description = "左上角坐标[lat,lon]", example = "[30.123,120.456]")
    private String lp;

    @Schema(description = "右下角坐标[lat,lon]", example = "[30.789,120.012]")
    private String rp;

    @Schema(description = "摄像头层级", example = "level1")
    private String cameraLevel;

    @Schema(description = "是否有位置")
    private Integer hasLocation;

    @Schema(description = "搜索字段")
    private String search;

    @Schema(description = "设备类型id")
    private Long isdnTypeId;

    public boolean validLpRp() {
        return lp != null && !lp.isBlank() && lp.split(",").length >= 2 && rp != null && !rp.isBlank() && rp.split(
            ",").length >= 2;
    }

    public String[] getLpArray(){
        return lp.split(",");
    }

    public String[] getRpArray(){
        return rp.split(",");
    }
}