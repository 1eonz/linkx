package com.tdtech.cloudcmd.gateway.filter;

import com.tdtech.cloudcmd.i18n.CustomizedResourceBundleMessageSource;
import com.tdtech.cloudcmd.i18n.I18nMessageCustomizer;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.i18n.VersionedResourceBundleMessageSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
public class CommonLocaleConfig {

    @Bean
    public CustomizedResourceBundleMessageSource versionedResourceBundleMessageSource(
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