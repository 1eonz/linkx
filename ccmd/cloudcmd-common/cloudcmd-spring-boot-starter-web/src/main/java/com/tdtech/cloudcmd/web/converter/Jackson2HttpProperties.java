package com.tdtech.cloudcmd.web.converter;

import java.util.TimeZone;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.tdtech.cloudcmd.util.DateFormatUtil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudcmd.jackson")
public class Jackson2HttpProperties {

    private String dateFormat = DateFormatUtil.YYYY_MM_DD_HH_MM_SS;
    private TimeZone timeZone = TimeZone.getTimeZone("GMT+8:00");
}
