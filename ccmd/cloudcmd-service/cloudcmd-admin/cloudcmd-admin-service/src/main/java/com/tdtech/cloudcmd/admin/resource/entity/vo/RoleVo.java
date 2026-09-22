package com.tdtech.cloudcmd.admin.resource.entity.vo;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2021/1/11 16:48
 */
@Data
@Deprecated
public class RoleVo implements Serializable {

    private String name;

    private Integer type;

    private Long applicationId;

    @NotNull
    private Integer page;

    @NotNull
    private Integer limit;
}
