package com.tdtech.cloudcmd.admin.resource.entity.vo;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.ToString;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
@ToString(callSuper = true)
public class AppInfoUpdateStatusReqVO {

    private Long id;

    private String name;

    @NotNull(message = "是否启用不能为空")
    private Integer status;

}
