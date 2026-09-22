package com.tdtech.cloudcmd.mysql.encrypt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * DTO解密配置
 * 用于配置哪些DTO的哪些字段需要解密
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "encrypt.dto")
public class DtoDecryptConfig {
    
    /**
     * DTO解密配置列表
     * 
     * 配置格式：
     * encrypt:
     *   dto:
     *     mappings:
     *       - className: "com.example.dto.UserVO"
     *         fields: ["name", "mobile", "idCard"]
     *       - className: "com.example.dto.GroupVO"
     *         fields: ["userName"]
     */
    private List<DtoMapping> mappings = new ArrayList<>();
    
    /**
     * 转换为Map格式
     * Key: DTO类
     * Value: 需要解密的字段名列表
     */
    public Map<Class<?>, List<String>> toMap() {
        Map<Class<?>, List<String>> result = new HashMap<>();
        
        if (mappings == null || mappings.isEmpty()) {
            return result;
        }
        
        for (DtoMapping mapping : mappings) {
            try {
                Class<?> clazz = Class.forName(mapping.getClassName());
                result.put(clazz, mapping.getFields());
            } catch (ClassNotFoundException e) {
                // 类不存在，跳过
            }
        }
        
        return result;
    }
    
    @Data
    public static class DtoMapping {
        /**
         * DTO类全限定名
         */
        private String className;
        
        /**
         * 需要解密的字段名列表
         */
        private List<String> fields = new ArrayList<>();
    }
}
