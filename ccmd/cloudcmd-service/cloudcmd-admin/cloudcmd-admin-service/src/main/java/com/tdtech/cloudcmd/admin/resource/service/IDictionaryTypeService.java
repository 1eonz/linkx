package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryType;

/**
 * <p>
 * 数据字典类型表 服务类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
public interface IDictionaryTypeService extends IService<DictionaryType> {

    /**
     * 获取所有的字典类型列表
     * 
     * @return
     */
    List<DictionaryType> getDictionaryTypeList();

    /**
     * 根究类型编号获取字典类型对象
     * 
     * @param typeCode
     * @return
     */
    DictionaryType getDictionaryTypeByTypeCode(String typeCode);

    /**
     * 创建字典类型
     * 
     * @param dictionaryType
     * @return
     */
    String createDictionaryType(DictionaryType dictionaryType);

    /**
     * 修改字典类型信息
     * 
     * @param dictionaryType
     * @return
     */
    String updateDictionaryType(DictionaryType dictionaryType);

    /**
     * 批量删除字典类型
     * 
     * @param id
     * @return
     */
    String deleteDictionaryTypeById(Long id);

    /**
     * 批量删除字典类型
     * 
     * @param dictionaryTypeIds
     */
    void batchDeleteDictionaryTypeByIds(List<Long> dictionaryTypeIds);

    /**
     * 批量插入字典类型
     * 
     * @param dictionaryTypeList
     */
    void batchInsertDictionaryType(List<DictionaryType> dictionaryTypeList);
}
