package com.tdtech.cloudcmd.mysql.encrypt.config;

import com.tdtech.cloudcmd.mysql.encrypt.interceptor.DecryptionInnerInterceptor;
import com.tdtech.cloudcmd.mysql.encrypt.interceptor.EncryptionInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;

import java.util.List;

/**
 * 加密自动配置类
 * 
 * 自动配置加密相关的Bean
 * - 如果用户没有提供EncryptionService实现，则使用默认的Base64实现
 * - 自动注册加密和解密拦截器
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DtoDecryptConfig.class)
public class EncryptAutoConfiguration {

    /**
     * 加密拦截器（MyBatis-Plus InnerInterceptor）
     * 会被自动注入到MybatisPlusInterceptor中
     */
    @Bean
    public EncryptionInnerInterceptor encryptionInnerInterceptor(EncryptionService encryptionService) {
        log.info("注册加密拦截器");
        return new EncryptionInnerInterceptor(encryptionService);
    }

    /**
     * 解密拦截器（MyBatis Interceptor）
     */
    @Bean
    public DecryptionInnerInterceptor decryptionInnerInterceptor(
            EncryptionService encryptionService, 
            DtoDecryptConfig dtoDecryptConfig) {
        
        log.info("注册解密拦截器");
        DecryptionInnerInterceptor interceptor = new DecryptionInnerInterceptor(encryptionService);
        
        // 注册DTO解密配置
        if (dtoDecryptConfig != null && dtoDecryptConfig.getMappings() != null 
            && !dtoDecryptConfig.getMappings().isEmpty()) {
            interceptor.registerDtoDecryptFields(dtoDecryptConfig.toMap());
            log.info("已注册{}个DTO解密配置", dtoDecryptConfig.getMappings().size());
        }
        
        return interceptor;
    }
    
    /**
     * 解密拦截器注册器
     * 使用SmartInitializingSingleton确保在所有Bean初始化完成后注册
     */
    @Bean
    public DecryptionInterceptorRegister decryptionInterceptorRegister() {
        return new DecryptionInterceptorRegister();
    }
    
    /**
     * 解密拦截器注册器（内部类）
     * 实现SmartInitializingSingleton接口，在所有单例Bean初始化完成后注册拦截器
     */
    public static class DecryptionInterceptorRegister implements SmartInitializingSingleton {
        
        @Autowired
        private List<SqlSessionFactory> sqlSessionFactoryList;
        
        @Autowired
        private DecryptionInnerInterceptor decryptionInnerInterceptor;
        
        @Override
        public void afterSingletonsInstantiated() {
            if (sqlSessionFactoryList != null && decryptionInnerInterceptor != null) {
                for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
                    org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
                    
                    // 检查是否已经注册过该拦截器，避免重复注册
                    boolean alreadyRegistered = configuration.getInterceptors().stream()
                        .anyMatch(interceptor -> interceptor instanceof DecryptionInnerInterceptor);
                    
                    if (!alreadyRegistered) {
                        configuration.addInterceptor(decryptionInnerInterceptor);
                        log.info("解密拦截器已注册到SqlSessionFactory");
                    } else {
                        log.warn("解密拦截器已存在，跳过注册");
                    }
                }
            }
        }
    }
}
