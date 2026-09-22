package com.tdtech.cloudcmd.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.DepartmentCustom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 自定义通讯录 Mapper。
 */
@Mapper
public interface DepartmentCustomMapper extends BaseMapper<DepartmentCustom> {

    /**
     * 根据ID更新name和dutyType字段
     * @param id 通讯录ID
     * @param name 通讯录名称
     * @param dutyType 值班类型（可为null）
     * @return 更新行数
     */
    int updateNameAndDutyTypeById(@Param("id") Long id, @Param("name") String name, @Param("dutyType") Integer dutyType);
}
