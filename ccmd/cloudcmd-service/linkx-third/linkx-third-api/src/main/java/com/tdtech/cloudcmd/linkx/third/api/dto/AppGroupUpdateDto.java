package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppGroupUpdateDto extends AppGroupCreateDto{

    @NotNull(message = "分组ID不能为空")
    private Long id;

}
