package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryItem;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code） Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Mapper
public interface DictionaryItemMapper extends BaseMapper<DictionaryItem> {

    /**
     * 根据类型编号查询所有字典配置
     * 
     * @param code
     * @return
     */
    List<DictionaryItem> getDictionaryItemListByTypeCode(@Param("code") String code,@Param("displayTarget")Integer displayTarget);

    /**
     * 批量插入字典配置
     * 
     * @param dictionaryItemList
     */
    void batchInsertDictionaryItem(@Param("dictionaryItemList") List<DictionaryItem> dictionaryItemList);
}
