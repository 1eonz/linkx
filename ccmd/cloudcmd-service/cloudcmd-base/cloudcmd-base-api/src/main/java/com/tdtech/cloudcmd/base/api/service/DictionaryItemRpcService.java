package com.tdtech.cloudcmd.base.api.service;

import java.util.List;

import com.tdtech.cloudcmd.base.api.param.DictionaryDto;
import com.tdtech.cloudcmd.base.api.param.ItemDto;
import com.tdtech.cloudcmd.base.api.param.ItemQueryParam;

/**
 * @author mWX556161
 * @date 2020/6/16 14:08
 */
public interface DictionaryItemRpcService {

    /**
     * 根据item value查询名称
     *
     * @param itemQueryParam
     * @return
     */
    String getName(ItemQueryParam itemQueryParam);

    /**
     * 根据字典类型获取字典配置项
     *
     * @param typeCode
     * @return
     */
    List<ItemDto> getItemListByType(String typeCode);

    /**
     * 获取设备icon
     * 
     * @param typeId
     * @return
     */
    DictionaryDto queryIconFromDictionary(Long typeId, String dicType);
}
