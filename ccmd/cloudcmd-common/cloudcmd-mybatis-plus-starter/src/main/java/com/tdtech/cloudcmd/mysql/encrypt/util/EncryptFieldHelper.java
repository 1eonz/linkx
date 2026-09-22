package com.tdtech.cloudcmd.mysql.encrypt.util;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tdtech.cloudcmd.mysql.encrypt.annotation.EncryptField;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密字段辅助工具类
 * 提供判断类是否需要加解密的公共方法
 */
@Slf4j
public class EncryptFieldHelper {
    
    /**
     * 表白名单：只有白名单中的表才进行加解密处理
     */
    private Set<String> tableWhitelist;
    
    /**
     * 缓存：类是否需要加解密处理
     */
    private Map<Class<?>, Boolean> needProcessCache = new ConcurrentHashMap<>();

    public EncryptFieldHelper() {
        // 默认白名单
        this.tableWhitelist = new HashSet<>(List.of(
                "tb_im_user"
        ));
    }
    
    public EncryptFieldHelper(Set<String> tableWhitelist) {
        this.tableWhitelist = tableWhitelist != null ? tableWhitelist : new HashSet<>();
    }
    
    /**
     * 设置表白名单
     */
    public void setTableWhitelist(Set<String> tableWhitelist) {
        this.tableWhitelist = tableWhitelist;
        // 清空缓存
        this.needProcessCache.clear();
    }
    
    /**
     * 检查类是否需要加解密处理（带缓存）
     */
    public boolean needProcess(Class<?> clazz) {
        return needProcessCache.computeIfAbsent(clazz, this::checkNeedProcess);
    }
    
    /**
     * 检查类是否需要加解密处理
     * 1. 先检查表名是否在白名单中
     * 2. 再检查是否有加密字段
     */
    private boolean checkNeedProcess(Class<?> clazz) {
        // 1. 检查表名是否在白名单中
        if (!isInWhitelist(clazz)) {
            return false;
        }
        
        // 2. 检查是否有加密字段
        return hasEncryptField(clazz);
    }
    
    /**
     * 检查表名是否在白名单中
     */
    private boolean isInWhitelist(Class<?> clazz) {
        if (tableWhitelist == null || tableWhitelist.isEmpty()) {
            return false;
        }
        
        // 从TableInfoHelper获取表名
        TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
        if (tableInfo == null) {
            return false;
        }
        
        String tableName = tableInfo.getTableName();
        return tableWhitelist.contains(tableName.toLowerCase());
    }
    
    /**
     * 检查类是否有加密字段
     */
    private boolean hasEncryptField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            EncryptField encryptField = field.getAnnotation(EncryptField.class);
            if (encryptField != null && encryptField.value()) {
                return true;
            }
        }
        return false;
    }
}
