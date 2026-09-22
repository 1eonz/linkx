package com.tdtech.cloudcmd.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:cloudcmd-config-display-target.properties")
public class DisplayTarget {

    /**
     * 是否屏蔽目标识别功能 0表示隐藏，1表示显示
     */
    @Value("${cloudcmd.config.DisplayTarget}")
    public Integer displayTarget;
}
