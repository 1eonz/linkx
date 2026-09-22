package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.icp.proxy.entity.IcpImUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString(callSuper = true)
public class IcpImUserVO extends IcpImUser {

    @Schema(description = "纬度")
    public BigDecimal lat;

    @Schema(description = "经度")
    public BigDecimal lon;

    @Schema(description = "状态值")
    private String statusValue;

}
