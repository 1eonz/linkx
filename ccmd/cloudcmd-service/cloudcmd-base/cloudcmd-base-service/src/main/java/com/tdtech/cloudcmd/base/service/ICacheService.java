package com.tdtech.cloudcmd.base.service;

import java.util.List;

import com.tdtech.cloudcmd.base.api.param.DictionaryDto;
import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesDto;
import com.tdtech.cloudcmd.base.api.param.ItemDto;

/**
 * 缓存服务
 * 
 * @author mWX556161
 * @date 2020/6/16 13:34
 */
public interface ICacheService {

    /**
     * 初始化所有的缓存配置
     */
    void init();

    void initDictionary();

    void initExtendInfoProperties();

    void initGlobals();

    /**
     * 从缓存中得到字典值
     * 
     * @return
     */
    String getItemName(String type, String value);

    /**
     * 根据code查询其下所有配置项列表
     * 
     * @param code
     * @return
     */
    List<ItemDto> getItemListByTypeCode(String code);

    /**
     * 根据名称获取配置值
     * 
     * @param name
     * @return
     */
    String getGlobalsValue(String name);

    /**
     * 根据code获取扩展属性配置列表
     * 
     * @param code
     * @return
     */
    List<ExtendInfoPropertiesDto> getPropertiesListByCode(String code);

    /**
     * 根据类型和名字查询展示值
     * 
     * @param code
     * @param name
     * @return
     */
    String getPropertiesLabel(String code, String name);

    /**
     * 获取设备icon
     * 
     * @param typeId
     * @return
     */
    DictionaryDto queryIconFromDictionary(Long typeId, String dicType);
}
