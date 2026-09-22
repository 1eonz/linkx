package com.tdtech.cloudcmd.icp.proxy.controller.vo;

import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class DepartmentTree extends Department {

    private List<DepartmentTree> children=new LinkedList<>();

}
