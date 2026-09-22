package com.tdtech.cloudcmd.base.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.base.entity.DictionaryType;

/**
 * <p>
 * 数据字典类型表 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
public interface IDictionaryTypeService extends IService<DictionaryType> {

    /**
     * 获取所有的字典种类
     * 
     * @return
     */
    List<DictionaryType> getDictionaryTypeList();
}
