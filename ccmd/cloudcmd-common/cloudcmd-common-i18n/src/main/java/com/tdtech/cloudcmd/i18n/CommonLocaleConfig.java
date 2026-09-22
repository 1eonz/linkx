package com.tdtech.cloudcmd.i18n;

import java.io.IOException;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
public class CommonLocaleConfig {

    @Bean
    public AcceptHeaderLocaleResolver acceptHeaderLocaleResolver() {
        log.info("acceptHeaderLocaleResolver is start....  ");
        // 会话区域解析器也就是说，你设置完只针对当前的会话有效，session失效，还原为默认状态
        AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
        // 设置默认区域 默认区域机制遵循eapp中英文配置
        Locale locale = Locale.getDefault();
        acceptHeaderLocaleResolver.setDefaultLocale(locale);
        log.info("acceptHeaderLocaleResolver setDefaultLocale：" + locale.getLanguage());
        return acceptHeaderLocaleResolver;
    }

    @Bean
    public VersionedResourceBundleMessageSource versionedResourceBundleMessageSource(
        @Value("${spring.messages.basename:common_bean}") String messageNames,
        @Value("${cloudcmd.spring.messages.version:main}") String version,
        ObjectProvider<I18nMessageCustomizer> i18nMessageCustomizers) throws IOException {
        return new CustomizedResourceBundleMessageSource(messageNames, version, i18nMessageCustomizers);
    }

    @Bean
    public I18nUtil i18nUtil(VersionedResourceBundleMessageSource versionedResourceBundleMessageSource) {
        return new I18nUtil(versionedResourceBundleMessageSource);
    }
}