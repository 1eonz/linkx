package com.tdtech.cloudcmd.base.service.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesDto;
import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesQueryParam;
import com.tdtech.cloudcmd.base.api.service.ExtendInfoPropertiesRpcService;
import com.tdtech.cloudcmd.base.entity.ExtendInfoProperties;
import com.tdtech.cloudcmd.base.service.ICacheService;
import com.tdtech.cloudcmd.base.service.IExtendInfoPropertiesService;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author mWX556161
 * @date 2020/6/16 16:18
 */
@DubboService
@Slf4j
public class ExtendInfoPropertiesRpcServiceImpl implements ExtendInfoPropertiesRpcService {
    @Autowired
    private ICacheService cacheService;
    @Autowired
    private IExtendInfoPropertiesService extendInfoPropertiesService;

    @Override
    public List<ExtendInfoPropertiesDto> getExtendInfoPropertiesListByCode(String code) {
        return cacheService.getPropertiesListByCode(code);
    }

    @Override
    public String getExtendInfoPropertiesLabelByParam(ExtendInfoPropertiesQueryParam param) {
        log.debug("getExtendInfoPropertiesByParam code param:{}", param);
        return cacheService.getPropertiesLabel(param.getCode(), param.getName());
    }

    @Override
    public Map<String, List<ExtendInfoPropertiesDto>> getExtendInfoPropertiesList() {
        Map<String, List<ExtendInfoProperties>> extendInfoPropertiesMap =
            extendInfoPropertiesService.getExtendInfoPropertiesMap();
        Map<String, List<ExtendInfoPropertiesDto>> resultMap = new HashMap<>();
        for (String key : extendInfoPropertiesMap.keySet()) {
            List<ExtendInfoPropertiesDto> list = new ArrayList<>();
            List<ExtendInfoProperties> extendInfoProperties = extendInfoPropertiesMap.get(key);
            for (ExtendInfoProperties extendInfoProperty : extendInfoProperties) {
                ExtendInfoPropertiesDto dto = new ExtendInfoPropertiesDto();
                dto.setLabel(I18nUtil.get(extendInfoProperty.getLabel()));
                dto.setName(extendInfoProperty.getName());
                list.add(dto);
            }
            resultMap.put(key, list);
        }
        return resultMap;
    }

    @Override
    public String getExtendInfoPropertiesLabelByName(String name) {

        return extendInfoPropertiesService.getExtendInfoPropertiesLabelByName(name);
    }
}
