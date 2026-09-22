package com.tdtech.cloudcmd.encryptor.service;

import cloudcmd.service.rpc.SystemConfigRpcService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 */
@Component
public class CachedConfig {

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @DubboReference
    private SystemConfigRpcService configRpcService;

    private final Cache<String, String> globalCache =
        CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.MINUTES).build();

    private final Cache<String, String> systemCache =
            CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.MINUTES).build();

    @SneakyThrows
    public String getGlobalConfig(String key) {
        return globalCache.get(key, () -> {
            String value = globalsRpcService.getGlobalsValueByName(key);
            return value != null ? value : StringUtils.EMPTY;
        });
    }


    @SneakyThrows
    public String getSystemConfig(String key) {
        return systemCache.get(key, () -> {
            String value = configRpcService.getValueByKey(key);
            return value != null ? value : StringUtils.EMPTY;
        });
    }
    public void clearGlobalConfig(){
        systemCache.cleanUp();
    }

    public void clearSystemConfig(){
        systemCache.cleanUp();
    }
}
