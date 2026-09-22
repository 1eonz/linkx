package com.tdtech.cloudcmd.icp.proxy.ws.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "用户通知信息")
public class UserNotify {

    @JsonProperty("apptype")
    @Schema(description = "应用类型", example = "app001")
    private String apptype;

    @JsonProperty("category")
    @Schema(description = "类别", example = "normal")
    private String category;

    @JsonProperty("departmentid")
    @Schema(description = "部门ID", example = "dept001")
    private String departmentid;

    @JsonProperty("isdn")
    @Schema(description = "ISDN号码", example = "123456789")
    private String isdn;

    @JsonProperty("name")
    @Schema(description = "名称", example = "用户A")
    private String name;

    @JsonProperty("priority")
    @Schema(description = "优先级", example = "1")
    private String priority;

    @JsonProperty("servermode")
    @Schema(description = "服务器模式", example = "master")
    private String servermode;

    @JsonProperty("subusercategory")
    @Schema(description = "子用户类别", example = "sub_normal")
    private String subusercategory;

    @JsonProperty("uetype")
    @Schema(description = "设备类型", example = "mobile")
    private String uetype;

    @JsonProperty("vpnid")
    @Schema(description = "VPN ID", example = "vpn001")
    private String vpnid;

    @JsonProperty("vpnin")
    @Schema(description = "VPN入", example = "in001")
    private String vpnin;

    @JsonProperty("vpnout")
    @Schema(description = "VPN出", example = "out001")
    private String vpnout;
}
