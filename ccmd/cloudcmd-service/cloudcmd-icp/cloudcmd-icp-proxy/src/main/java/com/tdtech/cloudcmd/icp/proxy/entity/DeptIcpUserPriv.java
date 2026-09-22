package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 部门设备权限配置
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tr_dept_icp_user_priv")
public class DeptIcpUserPriv {

    @TableId
    private Long id;
    /**
     * 部门编码
     */
    private String deptCode;
    /**
     * 部门ID（设备权限）
     */
    private String departmentId;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 修改时间
     */
    private Date gmtModified;
}
