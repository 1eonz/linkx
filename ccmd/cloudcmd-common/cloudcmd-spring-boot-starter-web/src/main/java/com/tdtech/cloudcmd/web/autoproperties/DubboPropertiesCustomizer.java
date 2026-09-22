package com.tdtech.cloudcmd.web.autoproperties;

import com.tdtech.cloudcmd.web.runner.PropertiesCustomizer;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
public class DubboPropertiesCustomizer implements PropertiesCustomizer {
    @Override
    public Properties properties() {
        var properties = new Properties();
        properties.put("dubbo.registry.address", "${cloudcmd.zookeeper.conn-str}");
        properties.put("dubbo.registry.protocol", "zookeeper");
        properties.put("dubbo.scan.base-packages", "com.tdtech.cloudcmd");
        properties.put("dubbo.application.name", "${spring.application.name}");
        properties.put("dubbo.application.serialize-check-status", "DISABLE");
        properties.put("dubbo.application.check-serializable", "false");
        properties.put("dubbo.application.register-mode", "instance");
        properties.put("dubbo.provider.timeout", "10000");
        properties.put("dubbo.provider.retries", "0");
        properties.put("dubbo.provider.threads", "400");
        properties.put("dubbo.consumer.check", "false");
        properties.put("dubbo.consumer.retries", "0");
        properties.put("dubbo.protocol.name", "dubbo");
        properties.put("dubbo.protocol.port", "18080");
        properties.put("dubbo.protocol.serialization", "hessian2");
        log.info("default properties:{}", properties);
        return properties;
    }
}
