package com.tdtech.cloudcmd.icp.proxy.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DepartmentResp {

    private String departmentcode;//部门编号，1-18位自然数。
    private String departmentid;//部门ID。
    private String departmentname;//部门名称。
    private String upperdepartmentId;//上级部门。
    private String vpnid;//VPN索引
    private String subnetId;//部门所属的子网编号（-1:本网）。

}
