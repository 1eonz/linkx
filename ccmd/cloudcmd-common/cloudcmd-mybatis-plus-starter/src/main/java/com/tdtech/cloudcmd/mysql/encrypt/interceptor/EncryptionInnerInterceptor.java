package com.tdtech.cloudcmd.mysql.encrypt.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.mysql.encrypt.annotation.EncryptField;
import com.tdtech.cloudcmd.mysql.encrypt.util.EncryptFieldHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MyBatis-Plus加密拦截器
 * 拦截insert和update操作，自动加密带有@EncryptField注解的字段
 * 
 * 使用Guava Cache避免重复加密：
 * - 记录已加密对象的引用，防止同一对象被多次加密
 * - 缓存有效期5分钟，自动过期清理
 */
@Slf4j
public class EncryptionInnerInterceptor implements InnerInterceptor {

    private final EncryptionService encryptionService;
    private final EncryptFieldHelper encryptFieldHelper;
    
    /**
     * 已加密对象缓存
     * Key: 对象的identityHashCode（基于对象引用）
     * Value: 占位符（只需要知道是否加密过）
     * 有效期：1分钟自动过期
     */
    private static final Cache<Integer, Boolean> ENCRYPTED_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)  // 最多缓存10000个对象
            .build();

    public EncryptionInnerInterceptor(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        this.encryptFieldHelper = new EncryptFieldHelper();
    }
    
    /**
     * 设置表白名单
     */
    public void setTableWhitelist(java.util.Set<String> tableWhitelist) {
        this.encryptFieldHelper.setTableWhitelist(tableWhitelist);
    }
    
    /**
     * 检查对象是否已被加密
     */
    private boolean isAlreadyEncrypted(Object obj) {
        if (obj == null) {
            return false;
        }
        return ENCRYPTED_CACHE.getIfPresent(System.identityHashCode(obj)) != null;
    }
    
    /**
     * 标记对象已被加密
     */
    private void markAsEncrypted(Object obj) {
        if (obj == null) {
            return;
        }
        ENCRYPTED_CACHE.put(System.identityHashCode(obj), Boolean.TRUE);
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 查询操作不处理
    }

    @Override
    public boolean willDoUpdate(Executor executor, MappedStatement ms, Object parameter) {
        // 更新前的处理
        String sqlCommandType = ms.getSqlCommandType().name();
        
        // 只处理INSERT和UPDATE操作
        if (!"INSERT".equals(sqlCommandType) && !"UPDATE".equals(sqlCommandType)) {
            return true;
        }

        // 加密参数对象中的敏感字段
        if (parameter != null) {
            // 处理Map参数（MyBatis-Plus将实体包装在Map中）
            if (parameter instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) parameter;
                // 只处理 "et" key（MyBatis-Plus 标准实体参数名），自定义方法不处理
                if (map.containsKey("et")) {
                    Object etValue = map.get("et");
                    if (etValue != null && encryptFieldHelper.needProcess(etValue.getClass())) {
                        // 检查是否已被加密
                        if (!isAlreadyEncrypted(etValue)) {
                            encryptFields(etValue);
                            // 标记为已加密
                            markAsEncrypted(etValue);
                        } else {
                            log.debug("对象已被加密，跳过: {}", etValue.getClass().getSimpleName());
                        }
                    }
                }
            } else {
                // 直接处理实体对象,检查是否已被加密
                if (!isAlreadyEncrypted(parameter)) {
                    encryptFields(parameter);
                    // 标记为已加密
                    markAsEncrypted(parameter);
                } else {
                    log.debug("对象已被加密，跳过: {}", parameter.getClass().getSimpleName());
                }
            }
        }

        return true;
    }

    /**
     * 加密对象中的敏感字段
     */
    private void encryptFields(Object obj) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        
        // 检查该类是否需要加密处理
        if (!encryptFieldHelper.needProcess(clazz)) {
            return;
        }
        
        // 处理实体对象
        for (Field field : clazz.getDeclaredFields()) {
            // 检查是否有@EncryptField注解
            EncryptField encryptField = field.getAnnotation(EncryptField.class);
            if (encryptField != null && encryptField.value()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    
                    if (value instanceof String) {
                        String plaintext = (String) value;
                        if (plaintext != null && !plaintext.isEmpty()) {
                            String ciphertext = encryptionService.encrypt(plaintext);
                            field.set(obj, ciphertext);
                            log.debug("加密字段: {} -> {}", field.getName(), ciphertext);
                        }
                    }
                } catch (Exception e) {
                    log.error("加密字段失败: {}", field.getName(), e);
                }
            }
        }
    }
}
