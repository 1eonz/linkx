package com.tdtech.cloudcmd.web.autoproperties;

import com.tdtech.cloudcmd.web.runner.PropertiesCustomizer;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
public class CommonsPropertiesCustomizer implements PropertiesCustomizer {
    @Override
    public Properties properties() {
        var properties = new Properties();
        properties.put("logging.config", "classpath:log/logback.xml");
        properties.put("spring.jackson.date-format", "yyyy-MM-dd HH:mm:ss");
        properties.put("spring.jackson.time-zone", "Asia/Shanghai");
        properties.put("spring.jackson.deserialization.fail-on-unknown-properties", "false");
        properties.put("spring.jackson.serialization.fail-on-empty-beans", "false");
        properties.put("spring.jackson.serialization.write-self-references-as-null", "true");
        properties.put("server.port", "8080");
        properties.put("spring.main.allow-circular-references", "true");
        properties.put("springdoc.use-management-port", "true");
        properties.put("management.endpoints.web.exposure.include", "openapi,swagger-ui,health,info,prometheus,metrics");
        properties.put("management.server.port", "9090");
        properties.put("logging.level.com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor", "ERROR");//关闭optimise告警日志
        log.info("default properties:{}", properties);
        return properties;
    }
}