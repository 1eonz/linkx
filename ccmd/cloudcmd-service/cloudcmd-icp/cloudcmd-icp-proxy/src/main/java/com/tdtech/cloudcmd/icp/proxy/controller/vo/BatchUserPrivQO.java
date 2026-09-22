package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@Schema(description = "用户设备权限推送对象")
public class BatchUserPrivQO {

    @Schema(description = "用户ID列表")
    private List<Long> userIds;

    @Schema(description = "设备权限列表")
    private List<String> privs;
}
