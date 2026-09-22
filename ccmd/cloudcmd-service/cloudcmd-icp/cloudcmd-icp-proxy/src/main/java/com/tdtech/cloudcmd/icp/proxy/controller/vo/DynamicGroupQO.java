package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;

@Getter
@Setter
@Valid
@Schema(description = "动态群组查询对象")
public class DynamicGroupQO extends CcmdPageParam {

    @Schema(description = "用户ID", example = "user001")
    private String userId;

    @Schema(description = "用户名", example = "张三")
    private String userName;

    @Schema(description = "ISDN号码", example = "123456789")
    private String isdn;

}
