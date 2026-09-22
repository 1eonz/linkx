package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class WsDepartmentEvents {
    private String departmentCode;
    private Long departmentId;
    // 1新增 2修改 3删除
    private Integer operateType;
    private ImDepartment department;
}
