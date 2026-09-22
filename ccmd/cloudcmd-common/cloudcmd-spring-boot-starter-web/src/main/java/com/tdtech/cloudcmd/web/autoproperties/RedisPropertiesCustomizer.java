package com.tdtech.cloudcmd.web.autoproperties;

import java.util.Properties;

import com.tdtech.cloudcmd.web.runner.PropertiesCustomizer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisPropertiesCustomizer implements PropertiesCustomizer {
    @Override
    public Properties properties() {
        var properties = new Properties();
        properties.put("spring.redis.host","${cloudcmd.redis.host}");
        properties.put("spring.redis.port","${cloudcmd.redis.port}");
        properties.put("spring.redis.password","${cloudcmd.redis.password}");
        properties.put("spring.redis.database","${cloudcmd.redis.database}");
        properties.put("spring.redis.timeout","3000");
        log.info("default properties:{}",properties);
        return properties;
    }
}
