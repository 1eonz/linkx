package com.tdtech.cloudcmd.im.jingxin.server.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

@AutoConfiguration
@ConditionalOnClass({MeterRegistryCustomizer.class, JvmInfoMetrics.class})
@ConditionalOnProperty(prefix = "linkx.metrics", value = "enable", matchIfMissing = true)
public class JingxinMetricsAutoConfiguration {

    @Resource
    @Lazy
    private MeterRegistry registry;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }

    @Bean
    @ConditionalOnClass(JvmInfoMetrics.class)
    public JvmInfoMetrics jvmInfoMetrics() {
        var info = new JvmInfoMetrics();
        info.bindTo(registry);
        return info;
    }
}
