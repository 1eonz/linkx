package com.tdtech.cloudcmd.base.service.rpc;

import java.util.List;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.tdtech.cloudcmd.base.api.param.DictionaryDto;
import com.tdtech.cloudcmd.base.api.param.ItemDto;
import com.tdtech.cloudcmd.base.api.param.ItemQueryParam;
import com.tdtech.cloudcmd.base.api.service.DictionaryItemRpcService;
import com.tdtech.cloudcmd.base.service.ICacheService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author mWX556161
 * @date 2020/6/16 14:10
 */
@DubboService
@Slf4j
public class DictionaryItemRpcServiceImpl implements DictionaryItemRpcService {

    @Autowired
    private ICacheService cacheService;

    @Override
    public String getName(ItemQueryParam param) {
        log.debug("ItemQueryParam is:{}", param);
        return cacheService.getItemName(param.getType(), param.getValue());
    }

    @Override
    public List<ItemDto> getItemListByType(String type) {
        return cacheService.getItemListByTypeCode(type);
    }

    @Override
    public DictionaryDto queryIconFromDictionary(Long typeId, String dicType) {
        return cacheService.queryIconFromDictionary(typeId, dicType);
    }
}
