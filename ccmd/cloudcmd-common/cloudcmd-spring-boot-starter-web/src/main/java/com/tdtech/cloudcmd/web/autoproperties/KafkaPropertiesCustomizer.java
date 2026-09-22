package com.tdtech.cloudcmd.web.autoproperties;

import java.util.Properties;

import com.tdtech.cloudcmd.web.runner.PropertiesCustomizer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaPropertiesCustomizer implements PropertiesCustomizer {
    @Override
    public Properties properties() {
        var properties = new Properties();
        properties.put("spring.cloud.stream.kafka.binder.brokers","${cloudcmd.kafka.brokers}");
        properties.put("spring.cloud.stream.kafka.binder.auto-create-topics","true");
        properties.put("spring.cloud.stream.kafka.binder.auto-add-partitions","true");
        properties.put("spring.kafka.bootstrap-servers","${cloudcmd.kafka.brokers}");
        properties.put("spring.kafka.consumer.enable-auto-commit","true");
        log.info("default properties:{}",properties);
        return properties;
    }
}
