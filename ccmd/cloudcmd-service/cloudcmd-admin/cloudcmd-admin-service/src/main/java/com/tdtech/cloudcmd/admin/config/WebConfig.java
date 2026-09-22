package com.tdtech.cloudcmd.admin.config;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author zWX446107
 * @Description <一句话功能简述> <功能详细描述>
 * @create 2020-08-11 14:12
 */
@RefreshScope // 可更新
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * 图片存储路径
     */
    public static final String FILE_SAVE_PATH = "/home/linkx/headshot/";
    /**
     * map
     */
    private static final String FILE_MAP_PATH = "/home/ics/offlinemap/data/mapicon/";
    /**
     * 上传文件路径
     */
    public static final String UPLOAD_FILE_PATH = "/home/linkx/file/";




    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/admin/static/**").addResourceLocations("file:" + FILE_SAVE_PATH)
            .setCachePeriod(0);// 0表示不缓存
        registry.addResourceHandler("/admin/staticFile/**").addResourceLocations("file:" + UPLOAD_FILE_PATH)
            .setCachePeriod(0);
        registry.addResourceHandler("/admin/static/map/mapicon/**").addResourceLocations("file:" + FILE_MAP_PATH)
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
