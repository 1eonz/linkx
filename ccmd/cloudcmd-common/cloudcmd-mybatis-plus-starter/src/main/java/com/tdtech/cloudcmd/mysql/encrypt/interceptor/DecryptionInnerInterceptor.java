package com.tdtech.cloudcmd.mysql.encrypt.interceptor;

import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.mysql.encrypt.annotation.EncryptField;
import com.tdtech.cloudcmd.mysql.encrypt.util.EncryptFieldHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis解密拦截器
 * 拦截ResultSetHandler.handleResultSets方法，自动解密带有@EncryptField注解的字段
 * 
 * 支持两种解密方式：
 * 1. 实体类解密：基于@EncryptField注解
 * 2. DTO解密：基于配置的字段列表
 */
@Slf4j
@Intercepts({
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class DecryptionInnerInterceptor implements Interceptor {

    private final EncryptionService encryptionService;
    private final EncryptFieldHelper encryptFieldHelper;
    
    /**
     * DTO解密配置
     * Key: DTO类
     * Value: 需要解密的字段名列表
     */
    private final Map<Class<?>, List<String>> dtoDecryptFields = new ConcurrentHashMap<>();

    public DecryptionInnerInterceptor(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        this.encryptFieldHelper = new EncryptFieldHelper();
    }
    
    /**
     * 设置表白名单
     */
    public void setTableWhitelist(Set<String> tableWhitelist) {
        this.encryptFieldHelper.setTableWhitelist(tableWhitelist);
    }
    
    /**
     * 注册DTO解密配置
     * 
     * @param dtoClass DTO类
     * @param fieldNames 需要解密的字段名列表
     */
    public void registerDtoDecryptFields(Class<?> dtoClass, List<String> fieldNames) {
        dtoDecryptFields.put(dtoClass, fieldNames);
        log.info("注册DTO解密配置: {} -> {}", dtoClass.getName(), fieldNames);
    }
    
    /**
     * 注册DTO解密配置
     * 
     * @param dtoClass DTO类
     * @param fieldNames 需要解密的字段名数组
     */
    public void registerDtoDecryptFields(Class<?> dtoClass, String... fieldNames) {
        registerDtoDecryptFields(dtoClass, Arrays.asList(fieldNames));
    }
    
    /**
     * 批量注册DTO解密配置
     * 
     * @param configMap 配置Map
     */
    public void registerDtoDecryptFields(Map<Class<?>, List<String>> configMap) {
        configMap.forEach(this::registerDtoDecryptFields);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 执行原查询
        Object result = invocation.proceed();

        if (result == null) {
            return null;
        }

        // 处理查询结果
        if (result instanceof List) {
            List<?> list = (List<?>) result;
            for (Object item : list) {
                decryptFields(item);
            }
        } else {
            decryptFields(result);
        }

        return result;
    }

    /**
     * 解密对象中的敏感字段
     */
    private void decryptFields(Object obj) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        
        // 1. 尝试按实体类解密（基于@EncryptField注解）
        if (encryptFieldHelper.needProcess(clazz)) {
            decryptEntityFields(obj);
        }
        
        // 2. 尝试按DTO解密（基于配置的字段列表）
        if (dtoDecryptFields.containsKey(clazz)) {
            decryptDtoFields(obj);
        }
    }
    
    /**
     * 解密实体类字段（基于@EncryptField注解）
     */
    private void decryptEntityFields(Object obj) {
        Class<?> clazz = obj.getClass();
        
        for (Field field : clazz.getDeclaredFields()) {
            // 检查是否有@EncryptField注解
            EncryptField encryptField = field.getAnnotation(EncryptField.class);
            if (encryptField != null && encryptField.value()) {
                decryptField(obj, field);
            }
        }
    }
    
    /**
     * 解密DTO字段（基于配置的字段列表）
     */
    private void decryptDtoFields(Object obj) {
        Class<?> clazz = obj.getClass();
        List<String> fieldNames = dtoDecryptFields.get(clazz);
        
        if (fieldNames == null || fieldNames.isEmpty()) {
            return;
        }
        
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                decryptField(obj, field);
            } catch (NoSuchFieldException e) {
                log.warn("DTO字段不存在: {}.{}, 跳过解密", clazz.getName(), fieldName);
            } catch (Exception e) {
                log.error("解密DTO字段失败: {}.{}, 跳过解密", clazz.getName(), fieldName, e);
            }
        }
    }
    
    /**
     * 解密单个字段
     */
    private void decryptField(Object obj, Field field) {
        try {
            field.setAccessible(true);
            Object value = field.get(obj);
            
            if (value instanceof String) {
                String ciphertext = (String) value;
                if (!ciphertext.isEmpty()) {
                    String plaintext = encryptionService.decrypt(ciphertext);
                    field.set(obj, plaintext);
                    log.debug("解密字段: {}.{} -> {}", obj.getClass().getSimpleName(), field.getName(), plaintext);
                }
            }
        } catch (Exception e) {
            log.error("解密字段失败: {}.{}", obj.getClass().getName(), field.getName(), e);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
