package com.tdtech.cloudcmd.encryptor.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 加密器自动配置类
 * 
 * 自动扫描并注册 EncryptionService 的实现
 * 默认实现：EncryptionServiceImpl
 * 
 * 启用定时任务支持，用于定时刷新加密配置
 * 
 * 自动初始化功能：
 * - EncryptionAutoInitializer：启动时自动检查 archive 目录，决定是否开启加密
 */
@Configuration
@EnableScheduling
@ComponentScan(basePackages = {
    "com.tdtech.cloudcmd.encryptor.service",
    "com.tdtech.cloudcmd.encryptor.initializer"
})
public class EncryptorAutoConfiguration {
    // 通过 @ComponentScan 自动扫描以下包：
    // 1. service.impl：加密服务实现
    // 2. initializer：启动时的自动初始化检查器
    // EncryptionServiceImpl 已标注 @Service，会被自动注册
    // EncryptionAutoInitializer 已标注 @Component，会被自动注册
    // @EnableScheduling 启用定时任务，支持配置定时刷新
}
