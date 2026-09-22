package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "用户查询对象")
public class UserQO extends CcmdPageParam {

    @Schema(description = "是否在线", example = "true")
    private Boolean isOnline;

    @Schema(description = "排序 true 在线的放前边 false 放后边", example = "true")
    private Boolean onlineFirst;

    @Schema(description = "左下角坐标[lat,lon]", example = "[30.123,120.456]")
    private String lp;

    @Schema(description = "右上角坐标[lat,lon]", example = "[30.789,120.012]")
    private String rp;

    @Schema(description = "部门ID", example = "dept001")
    private String departmentId;

    @Schema(description = "类型 逗号分隔", example = "类型")
    private String categorys;

    @Schema(description = "是否有位置")
    private Integer hasLocation;

    @Schema(description = "搜索字段")
    private String search;

    @Schema(description = "类型 逗号分隔，0 记录仪 1 布控球")
    private String type;

    @Schema(description = "设备类型id")
    private Long isdnTypeId;

    public boolean validLpRp() {
        return lp != null && !lp.isBlank() && lp.split(",").length >= 2 && rp != null && !rp.isBlank() && rp.split(
            ",").length >= 2;
    }

    public String[] getLpArray() {
        return lp.split(",");
    }

    public String[] getRpArray() {
        return rp.split(",");
    }

    public List<Integer> getTypeList() {
        if (type == null || type.isBlank()) {
            return Collections.emptyList();
        }
        var split = type.split(",");
        return Arrays.stream(split).filter(a -> !a.isBlank()).map(Integer::parseInt).collect(Collectors.toList());
    }

}
