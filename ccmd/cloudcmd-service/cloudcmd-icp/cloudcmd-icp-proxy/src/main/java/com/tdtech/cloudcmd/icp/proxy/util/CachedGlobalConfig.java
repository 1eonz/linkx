package com.tdtech.cloudcmd.icp.proxy.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import lombok.SneakyThrows;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CachedGlobalConfig {

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    private final Cache<String, String> cache =
        CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    @SneakyThrows
    public String getConfig(String key) {
        return cache.get(key, () -> globalsRpcService.getGlobalsValueByName(key));
    }

    public void clear(){
        cache.cleanUp();
    }

}
