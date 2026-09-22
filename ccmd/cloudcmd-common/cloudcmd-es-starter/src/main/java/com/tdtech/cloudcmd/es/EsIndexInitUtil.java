package com.tdtech.cloudcmd.es;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;

import javax.annotation.Resource;

/**
 * ES 索引初始化工具类
 * 功能：判断索引是否存在，不存在则创建（存在则跳过）
 * 支持所有标注 @Document 的实体类
 * 
 * 仅在 ElasticsearchRestTemplate 存在时才创建
 */
@Slf4j
public class EsIndexInitUtil {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 初始化索引：不存在则创建，存在则跳过
     *
     * @param clazz 你的 @Document 实体类
     * @param <T>   泛型
     */
    public <T> void createIndexIfNotExists(Class<T> clazz) {
        try {
            // 1. 获取索引操作对象
            IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(clazz);

            // 2. 判断索引是否存在
            boolean exists = indexOperations.exists();
            if (exists) {
                log.info("✅ 索引已存在，跳过创建：" + indexOperations.getIndexCoordinates().getIndexName());
                return;
            }

            // 3. 索引不存在 → 创建索引 + 映射（mapping）
            // createIndex()：创建索引结构
            // putMapping()：加载 @Document 里的字段映射
            indexOperations.create();
            indexOperations.putMapping(indexOperations.createMapping(clazz));

            log.info("✅ 索引创建成功：" + indexOperations.getIndexCoordinates().getIndexName());

        } catch (Exception e) {
            log.error("❌ 索引创建失败：" + clazz.getName());
        }
    }
}