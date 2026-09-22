package com.tdtech.cloudcmd.mysql;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class MybatisAutoConfiguration {

    /**
     * 使用ObjectProvider延迟获取InnerInterceptor，避免Bean创建顺序问题
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            ObjectProvider<List<com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor>> innerInterceptorsProvider) {
        
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 延迟获取InnerInterceptor列表
        List<com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor> innerInterceptors = 
            innerInterceptorsProvider.getIfAvailable();
        
        // 添加自定义的InnerInterceptor（如加密拦截器）
        if (innerInterceptors != null && !innerInterceptors.isEmpty()) {
            for (com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor innerInterceptor : innerInterceptors) {
                interceptor.addInnerInterceptor(innerInterceptor);
                log.info("添加InnerInterceptor: {}", innerInterceptor.getClass().getSimpleName());
            }
        }
        
        // 分页拦截器（如果配置多个插件, 切记分页最后添加）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        return interceptor;
    }

}
