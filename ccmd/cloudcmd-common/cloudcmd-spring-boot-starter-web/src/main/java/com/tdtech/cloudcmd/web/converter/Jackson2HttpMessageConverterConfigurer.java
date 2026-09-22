package com.tdtech.cloudcmd.web.converter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@RefreshScope
@Configuration
@EnableConfigurationProperties(Jackson2HttpProperties.class)
public class Jackson2HttpMessageConverterConfigurer {

    /**
     * 解决前端js处理大数字丢失精度问题，将Long和BigInteger转换成string
     *
     * @return
     */
    @Bean
    public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter(Jackson2HttpProperties properties,
                                                                                      ObjectMapper objectMapper) {
        var copy = objectMapper.copy();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        copy.registerModule(simpleModule);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(properties.getDateFormat());
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        copy.registerModule(javaTimeModule);
        copy.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Date format
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(properties.getDateFormat());
        // Since Jackson 2.6.3 we always need to set a TimeZone (see
        // gh-4170). If none in our properties fallback to the Jackson's
        // default
        TimeZone timeZone = properties.getTimeZone();
        if (timeZone == null) {
            timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
        }
        simpleDateFormat.setTimeZone(timeZone);
        copy.setTimeZone(timeZone);
        copy.setDateFormat(simpleDateFormat);

        // 忽略json字符串中不识别的属性
        copy.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 忽略无法转换的对象
        copy.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 不输出空值
        copy.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        return new MappingJackson2HttpMessageConverter(copy);
    }
}
