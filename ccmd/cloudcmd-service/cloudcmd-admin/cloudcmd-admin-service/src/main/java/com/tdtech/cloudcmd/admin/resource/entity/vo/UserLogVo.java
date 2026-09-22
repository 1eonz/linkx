package com.tdtech.cloudcmd.admin.resource.entity.vo;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户操作日志Vo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLogVo {
    @NotNull
    private Integer page;
    @NotNull
    private Integer limit;
}
