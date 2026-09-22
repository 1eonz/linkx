package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryItem;
import com.tdtech.cloudcmd.admin.resource.entity.dto.DictionaryItemDto;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典配置项code） 服务类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
public interface IDictionaryItemService extends IService<DictionaryItem> {

    /**
     * 根据类型编号查询字典配置值
     * 
     * @param typeCode
     * @return
     */
    List<DictionaryItem> getDictionaryItemListByType(String typeCode);

    /**
     * 获取所有的字典配置
     * 
     * @return
     */
    List<DictionaryItem> getDictionaryItemList();

    /**
     * 创建字典配置项
     * 
     * @param dictionaryItemDto
     * @return
     */
    String createDictionaryItem(DictionaryItemDto dictionaryItemDto);

    /**
     * 修改字典配置项信息
     * 
     * @param dictionaryItemDto
     * @return
     */
    String updateDictionaryItem(DictionaryItemDto dictionaryItemDto);

    /**
     * 批量删除字典配置项
     * 
     * @param id
     * @return
     */
    String deleteDictionaryItemById(Long id);

    /**
     * 批量删除装备类型
     * 
     * @param dictionaryItemIds
     */
    void batchDeleteDictionaryItemByIds(List<Long> dictionaryItemIds);

    /**
     * 批量插入装备类型
     * 
     * @param dictionaryItemList
     */
    void batchInsertDictionaryItem(List<DictionaryItem> dictionaryItemList);
}
