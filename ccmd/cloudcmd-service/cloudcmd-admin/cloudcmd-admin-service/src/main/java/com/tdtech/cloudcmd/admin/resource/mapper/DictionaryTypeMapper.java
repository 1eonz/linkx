package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryType;

/**
 * <p>
 * 数据字典类型表 Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Mapper
public interface DictionaryTypeMapper extends BaseMapper<DictionaryType> {

    /**
     * 批量插入字典类型
     * 
     * @param dictionaryTypeList
     */
    void batchInsertDictionaryType(@Param("dictionaryTypeList") List<DictionaryType> dictionaryTypeList);
}
