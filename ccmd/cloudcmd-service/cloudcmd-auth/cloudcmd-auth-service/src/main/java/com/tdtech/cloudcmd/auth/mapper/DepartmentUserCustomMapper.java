package com.tdtech.cloudcmd.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.DepartmentUserCustom;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义通讯录用户关系 Mapper。
 */
@Mapper
public interface DepartmentUserCustomMapper extends BaseMapper<DepartmentUserCustom> {

    /**
     * 批量插入自定义通讯录用户关系。
     *
     * @param list 用户关系列表
     * @return 插入数量
     */
    @Insert("<script>"
        + "INSERT INTO tb_department_node_user_custom (id, custom_dept_id, user_id, create_user_id, gmt_created) VALUES "
        + "<foreach collection='list' item='item' separator=','>"
        + "(#{item.id}, #{item.customDeptId}, #{item.userId}, #{item.createUserId}, #{item.gmtCreated})"
        + "</foreach>"
        + "</script>")
    int insertBatch(@Param("list") List<DepartmentUserCustom> list);
}
