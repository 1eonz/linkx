package com.tdtech.cloudcmd.base.api.service;

import java.util.List;

import com.tdtech.cloudcmd.base.api.param.TypeDto;

/**
 * @author mWX556161
 * @date 2020/6/23 10:20
 */
public interface DictionaryTypeRpcService {

    /**
     * 获取全量配置
     * 
     * @return
     */
    List<TypeDto> getDictionaryTypeDtoList();
}
