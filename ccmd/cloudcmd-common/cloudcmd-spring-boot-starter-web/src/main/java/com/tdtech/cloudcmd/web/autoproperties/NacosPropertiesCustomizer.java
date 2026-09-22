package com.tdtech.cloudcmd.web.autoproperties;

import java.util.Properties;

import com.tdtech.cloudcmd.web.runner.PropertiesCustomizer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NacosPropertiesCustomizer implements PropertiesCustomizer {
    @Override
    public Properties properties() {
        var properties = new Properties();
        properties.put("spring.cloud.nacos.discovery.server-addr","${cloudcmd.nacos.host}");
        properties.put("spring.cloud.nacos.config.server-addr","${cloudcmd.nacos.host}");
        properties.put("spring.cloud.nacos.config.file-extension","properties");
        log.info("default properties:{}",properties);
        return properties;
    }
}
