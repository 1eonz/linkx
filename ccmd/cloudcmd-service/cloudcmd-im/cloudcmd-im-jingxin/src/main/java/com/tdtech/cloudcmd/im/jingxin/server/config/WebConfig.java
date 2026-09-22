package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.tdtech.cloudcmd.im.jingxin.server.mvc.EncryptStaticResourceResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author lsc
 * @date 2025/7/17
 **/
@Configuration
@RefreshScope
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private EncryptStaticResourceResolver encryptStaticResourceResolver;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/collaboration/static/**")
                .addResourceLocations("file:" + "/home/linkx/im/")
                .setCachePeriod(0);// 0表示不缓存
        registry.addResourceHandler("/collaboration/static/archive/data/linkx/**")
                .addResourceLocations("file:" + "/data/linkx/")
                .setCachePeriod(0)
                .resourceChain(false)
                .addResolver(encryptStaticResourceResolver);

        registry.addResourceHandler("/collaboration/static/archive/**")
                .addResourceLocations("file:" + "/data/linkx/data/archive/")
                .setCachePeriod(0);

        registry.addResourceHandler("/collaboration/static/openapi/**")
                .addResourceLocations("file:" + "/data/linkx/data/openapi/")
                .setCachePeriod(0);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 所有接口
                .allowedOriginPatterns("*")  // 支持所有来源，也可以指定具体域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);  // 预检请求的有效期
    }
}