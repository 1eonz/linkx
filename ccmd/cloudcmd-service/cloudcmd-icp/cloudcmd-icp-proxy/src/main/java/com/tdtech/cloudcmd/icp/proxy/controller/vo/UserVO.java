package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.icp.proxy.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "用户信息")
public class UserVO extends User {

    @Schema(description = "状态值")
    private String statusValue;

    @Schema(description = "经度")
    private BigDecimal lon;

    @Schema(description = "纬度")
    private BigDecimal lat;

    private String departmntName;

    @Schema(description = "设备类型图标文件地址")
    private String icon;

    @Schema(description = "设备类型图标URI地址")
    private String iconUri;

    @Schema(description = "是否展示，1-展示 0-不展示")
    private Integer isShow;

    @Schema(description = "isdn_type表id", example = "1")
    private Long isdnTypeId;
}
