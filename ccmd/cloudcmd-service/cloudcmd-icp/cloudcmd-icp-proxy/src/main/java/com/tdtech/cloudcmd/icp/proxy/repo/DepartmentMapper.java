package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentMapper extends ExBaseMapper<Department> {

    @Insert("<script>" +
            "INSERT INTO tb_department (id, departmentcode, departmentid, departmentname, upperdepartment_id, vpnid, subnet_id, department_id_path, department_name_path, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.departmentcode}, #{item.departmentid}, #{item.departmentname}, #{item.upperdepartmentId}, #{item.vpnid}, #{item.subnetId}, #{item.departmentIdPath}, #{item.departmentNamePath}, #{item.createTime})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<Department> departmentList);

    /**
     * 批量新增或更新部门信息（根据departmentid判断，存在则更新，不存在则新增）
     * @param departmentList 部门列表
     * @return 影响记录数
     */
    @Insert("<script>" +
            "INSERT INTO tb_department (id, departmentcode, departmentid, departmentname, upperdepartment_id, vpnid, subnet_id, department_id_path, department_name_path, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.departmentcode}, #{item.departmentid}, #{item.departmentname}, #{item.upperdepartmentId}, #{item.vpnid}, #{item.subnetId}, #{item.departmentIdPath}, #{item.departmentNamePath}, #{item.createTime})" +
            "</foreach>" +
            " ON DUPLICATE KEY UPDATE " +
            "departmentcode = VALUES(departmentcode), " +
            "departmentname = VALUES(departmentname), " +
            "upperdepartment_id = VALUES(upperdepartment_id), " +
            "vpnid = VALUES(vpnid), " +
            "subnet_id = VALUES(subnet_id), " +
            "department_id_path = VALUES(department_id_path), " +
            "department_name_path = VALUES(department_name_path), " +
            "create_time = VALUES(create_time)" +
            "</script>")
    int insertOrUpdateBatch(@Param("list") List<Department> departmentList);
}
