package com.tdtech.cloudcmd.base.service.rpc;

import javax.annotation.Resource;

import org.apache.dubbo.config.annotation.DubboService;

import com.tdtech.cloudcmd.base.api.service.CacheRpcService;
import com.tdtech.cloudcmd.base.service.ICacheService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author mWX556161
 * @date 2020/8/13 14:09
 */
@DubboService
@Slf4j
public class CacheRpcServiceImpl implements CacheRpcService {

    @Resource
    private ICacheService cacheService;

    @Override
    public void initDictionary() {
        cacheService.initDictionary();
    }

    @Override
    public void initExtendInfoProperties() {
        cacheService.initExtendInfoProperties();
    }

    @Override
    public void initGlobals() {
        cacheService.initGlobals();
    }

}
