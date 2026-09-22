package com.tdtech.cloudcmd.icp.proxy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.icp.proxy.client.entity.DepartmentResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString(callSuper = true)
@TableName("tb_department")
@Schema(description = "部门信息")
public class Department extends DepartmentResp {

    @TableId
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "部门ID路径", example = "dept001.dept002.dept003")
    private String departmentIdPath;


    @Schema(description = "部门名称路径", example = "总公司.部门A.子部门B")
    private String departmentNamePath;

    @Schema(description = "创建时间")
    private Date createTime;
}
