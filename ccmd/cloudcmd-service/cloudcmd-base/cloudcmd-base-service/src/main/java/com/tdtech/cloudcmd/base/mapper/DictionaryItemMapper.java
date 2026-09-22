package com.tdtech.cloudcmd.base.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.base.entity.DictionaryItem;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code） Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Mapper
public interface DictionaryItemMapper extends BaseMapper<DictionaryItem> {

    /**
     * 根据类型查询出所有字典列表
     * 
     * @param code
     * @return
     */
    List<DictionaryItem> getDictionaryItemListByTypeCode(@Param("code") String code);

    /**
     * 根据类型和值查询出字典
     * 
     * @param type
     * @param value
     * @return
     */
    DictionaryItem getDictionaryItemByTypeAndValue(@Param("type") String type, @Param("value") String value);
}
