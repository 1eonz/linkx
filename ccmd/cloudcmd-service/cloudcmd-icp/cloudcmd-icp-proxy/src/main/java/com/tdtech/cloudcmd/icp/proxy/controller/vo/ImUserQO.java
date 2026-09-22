package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "用户查询对象")
public class ImUserQO extends CcmdPageParam {

    @Schema(description = "是否在线", example = "true")
    private Boolean isOnline;

    @Schema(description = "左上角坐标[lat,lon]", example = "[30.123,120.456]")
    private String lp;

    @Schema(description = "右下角坐标[lat,lon]", example = "[30.789,120.012]")
    private String rp;

    @Schema(description = "是否有位置")
    private Integer hasLocation;
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
