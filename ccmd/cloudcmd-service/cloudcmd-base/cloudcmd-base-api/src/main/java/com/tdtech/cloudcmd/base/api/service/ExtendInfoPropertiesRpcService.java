package com.tdtech.cloudcmd.base.api.service;

import java.util.List;
import java.util.Map;

import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesDto;
import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesQueryParam;

/**
 * @author mWX556161
 * @date 2020/6/16 16:17
 */
public interface ExtendInfoPropertiesRpcService {

    /**
     * 根据code获取扩展属性列表
     * 
     * @param code
     * @return
     */
    List<ExtendInfoPropertiesDto> getExtendInfoPropertiesListByCode(String code);

    /**
     * 根据参数获取扩展属性
     * 
     * @param param code和name查询参数
     * @return
     */
    String getExtendInfoPropertiesLabelByParam(ExtendInfoPropertiesQueryParam param);

    /**
     * 获取所有扩展信息配置
     * 
     * @return
     */
    Map<String, List<ExtendInfoPropertiesDto>> getExtendInfoPropertiesList();

    String getExtendInfoPropertiesLabelByName(String name);
}
