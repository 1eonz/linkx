package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class GroupSortDto {
    /**
     * 分组id
     */
    @NotNull(message = "分组id不能为空")
    private Long id;

    /**
     * 分组排序
     */
    @NotNull(message = "分组排序不能为空")
    private Integer sort;

    /**
     * 分组名称
     */
    @NotBlank(message = "分组名称不能为空")
    private String name;

    /**
     * 分组名称
     */
    @NotBlank(message = "分组类型不能为空")
    private String type;

    /*
      应用id列表
     */
    private List<Long> appIds;
}
