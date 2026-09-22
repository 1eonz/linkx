package com.tdtech.cloudcmd.im.jingxin.client.entity;

/**
 * @author lsc
 * @date 2025/7/14
 **/
import lombok.Data;

@Data
public class UserDepartmentVo {

    private Long departmentId; // 部门ID
    private String departmentName; // 部门名称
    private String fullPath; // 全路径
    private String fullPathName; // 全路径部门名称
}
