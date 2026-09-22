package com.tdtech.cloudcmd.base.api.service;

import java.util.List;

import com.tdtech.cloudcmd.base.api.param.GlobalsDto;

/**
 * @author mWX556161
 * @date 2020/6/16 14:34
 */
public interface GlobalsRpcService {

    /**
     * 根据名称获取配置值
     * 
     * @param name
     * @return
     */
    String getGlobalsValueByName(String name);

    /**
     * 获取所有的全局配置
     * 
     * @return
     */
    List<GlobalsDto> getGlobalsList();

    Integer getDisplayTarget();
}
