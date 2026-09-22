package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AppGroupCreateDto {
    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 分组名称
     */
    @NotEmpty(message = "分组名称不能为空")
    private String name;

    /**
     * 分组类型。1：系统级；2：用户级
     */
    @NotNull(message = "分组类型不能为空")
    private Integer type;

    /**
     * 分组排序。用户级需要支持排序,越小越靠前
     */
    private Integer sort;

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    private Long createUser;

    /**
     * 绑定的应用id列表
     */
    private List<Long> appIds;

}
