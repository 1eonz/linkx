package com.tdtech.cloudcmd.web.converter;

import java.util.TimeZone;

import javax.annotation.Resource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DateTimeConfig implements WebMvcConfigurer {
    @Resource
    private Jackson2HttpProperties properties;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateFormatter simpleDateFormat = new DateFormatter(properties.getDateFormat());
        TimeZone timeZone = properties.getTimeZone();
        if (timeZone == null) {
            timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
        }
        simpleDateFormat.setTimeZone(timeZone);
        registry.addFormatter(simpleDateFormat);
    }

    // @Override
    // protected void addFormatters(FormatterRegistry registry) {
    // DateFormatter simpleDateFormat = new DateFormatter(properties.getDateFormat());
    // TimeZone timeZone = properties.getTimeZone();
    // if (timeZone == null) {
    // timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
    // }
    // simpleDateFormat.setTimeZone(timeZone);
    // registry.addFormatter(simpleDateFormat);
    // }
    //
    // @Bean
    // public FormattingConversionService mvcConversionService() {
    // FormattingConversionService conversionService = new DefaultFormattingConversionService(false);
    // addFormatters(conversionService);
    // return conversionService;
    // }
}