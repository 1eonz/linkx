package com.tdtech.cloudcmd.admin.resource.entity.qo;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RoleQO {
    private String name;
    @NotNull
    private Integer pageSize;
    @NotNull
    private Integer pageNum;
}
