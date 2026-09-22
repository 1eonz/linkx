package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AppToGroupDto {

    /*
     分组类型。1：系统级；2：用户级
     */
    @NotNull(message = "分组类型不能为空")
    private Integer type;

    /*
      应用id列表
     */
    private List<Long> appIds;

    /*
    用户id
     */
    private Long userId;
}
