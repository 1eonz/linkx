package com.tdtech.cloudcmd.im.jingxin.client;

import java.util.concurrent.TimeUnit;

import cloudcmd.service.rpc.SystemConfigRpcService;
import com.tdtech.cloudcmd.util.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;

import lombok.SneakyThrows;

@Component
public class CachedImConfig {

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @DubboReference
    private SystemConfigRpcService systemConfigRpcService;

    private final Cache<String, String> cache =
        CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    private final Cache<String, String> sysConfigCache =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    @SneakyThrows
    public String getConfig(String key) {
        return cache.get(key, () -> globalsRpcService.getGlobalsValueByName(key));
    }

    public void clear(){
        cache.cleanUp();
    }

    @SneakyThrows
    public String getSysConfig(String key) {
        return sysConfigCache.get(key, () -> {
            String value = systemConfigRpcService.getValueByKey(key);
            if (value == null) {
                return StringUtils.EMPTY;
            }
            return value;
        });
    }

    public void clearSysConfig(){
        sysConfigCache.cleanUp();
    }
}
