package com.tdtech.cloudcmd.icp.proxy.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 配置静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 添加静态资源处理器
     * 将设备图标目录映射为HTTP可访问的静态资源
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置设备图标静态资源映射
        // 访问路径: /proxy/icp/static/device-icon/**
        // 实际路径: /data/linkx/data/device-icon/
        registry.addResourceHandler("/proxy/icp/static/device-icon/**")
                .addResourceLocations("file:" + "/data/linkx/data/device-icon/")
                .setCachePeriod(0); // 0表示不缓存
    }
}