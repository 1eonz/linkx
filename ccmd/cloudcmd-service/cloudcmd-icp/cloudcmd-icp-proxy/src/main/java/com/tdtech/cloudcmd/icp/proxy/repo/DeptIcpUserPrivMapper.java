package com.tdtech.cloudcmd.icp.proxy.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tdtech.cloudcmd.icp.proxy.entity.DeptIcpUserPriv;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeptIcpUserPrivMapper extends ExBaseMapper<DeptIcpUserPriv> {

    @Insert({"<script>",//
        "INSERT INTO tr_dept_icp_user_priv (id, dept_code, department_id, gmt_created, gmt_modified) VALUES ",//
        "<foreach collection='list' item='item' separator=','>",//
        "(#{item.id}, #{item.deptCode}, #{item.departmentId}, #{item.gmtCreated}, #{item.gmtModified})",//
        "</foreach>",//
        "</script>"//
    })
    int insertBatch(@Param("list") List<DeptIcpUserPriv> deptIcpUserPrivList);

    /**
     * 根据部门编码删除权限
     */
    default void deleteByDeptCode(String deptCode) {
        delete(new LambdaQueryWrapper<DeptIcpUserPriv>()
                .eq(DeptIcpUserPriv::getDeptCode, deptCode));
    }

    /**
     * 根据部门编码列表删除权限
     */
    default void deleteByDeptCodes(List<String> deptCodes) {
        delete(new LambdaQueryWrapper<DeptIcpUserPriv>()
                .in(DeptIcpUserPriv::getDeptCode, deptCodes));
    }

    /**
     * 根据部门编码查询权限列表
     */
    default List<DeptIcpUserPriv> selectByDeptCode(String deptCode) {
        return selectList(new LambdaQueryWrapper<DeptIcpUserPriv>()
                .eq(DeptIcpUserPriv::getDeptCode, deptCode));
    }

    /**
     * 根据部门编码列表查询权限列表
     */
    default List<DeptIcpUserPriv> selectByDeptCodes(List<String> deptCodes) {
        return selectList(new LambdaQueryWrapper<DeptIcpUserPriv>()
                .in(DeptIcpUserPriv::getDeptCode, deptCodes));
    }
}
