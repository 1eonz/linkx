package com.tdtech.cloudcmd.base.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.base.api.param.DictionaryDto;
import com.tdtech.cloudcmd.base.entity.DictionaryItem;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code） 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
public interface IDictionaryItemService extends IService<DictionaryItem> {

    /**
     * 根据类型id查询所有的字典项
     *
     * @param typeId
     * @return
     */
    List<DictionaryItem> getDictionaryItemListByTypeId(Long typeId);

    /**
     * 获取所有字典配置列表
     *
     * @return
     */
    List<DictionaryItem> getDictionaryItemList();

    /**
     * 获取所有字典配置分类
     *
     * @return
     */
    Map<String, List<DictionaryItem>> getDictionaryItemMap();

    /**
     * 根据code查询字典项名称
     *
     * @param type
     * @param value
     * @return
     */
    String getItemNameByCode(String type, String value);

    /**
     * 根据字典类型code查询配置项
     *
     * @param code
     * @return
     */
    List<DictionaryItem> getDictionaryItemByTypeCode(String code);

    /**
     * 获取设备图标映射
     * 
     * @param typeId
     * @param dicType
     * @return
     */
    DictionaryDto queryIconFromDictionary(Long typeId, String dicType);
}
