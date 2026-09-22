package com.tdtech.cloudcmd.es;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Elasticsearch自动配置类
 * 
 * 基于Spring Data Elasticsearch 4.4.18 + RestHighLevelClient 7.17.25
 * 
 * 自动配置以下Bean：
 * - RestHighLevelClient: ES客户端
 * - ElasticsearchRestTemplate: ES操作模板
 * 
 * 启用条件：
 * - spring.elasticsearch.uris已配置
 * - 存在RestHighLevelClient类
 */
@Slf4j
@AutoConfiguration
public class ESAutoConfiguration {
    @Bean
    public EsIndexInitUtil esIndexInitUtil() {
        return new EsIndexInitUtil();
    }
}
