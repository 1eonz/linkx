package com.tdtech.cloudcmd.linkx.third.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * MySQL 字段名校验工具类
 * 防止非法字段、SQL注入、关键字冲突
 */
public class MySqlColumnValidator {

    // MySQL 关键字/保留字（常用高频，可自行扩展）
    private static final Set<String> MYSQL_KEYWORDS = new HashSet<>();

    static {
        // 高频风险关键字
        MYSQL_KEYWORDS.add("select");
        MYSQL_KEYWORDS.add("insert");
        MYSQL_KEYWORDS.add("update");
        MYSQL_KEYWORDS.add("delete");
        MYSQL_KEYWORDS.add("where");
        MYSQL_KEYWORDS.add("from");
        MYSQL_KEYWORDS.add("set");
        MYSQL_KEYWORDS.add("order");
        MYSQL_KEYWORDS.add("group");
        MYSQL_KEYWORDS.add("limit");
        MYSQL_KEYWORDS.add("join");
        MYSQL_KEYWORDS.add("left");
        MYSQL_KEYWORDS.add("right");
        MYSQL_KEYWORDS.add("inner");
        MYSQL_KEYWORDS.add("user");
        MYSQL_KEYWORDS.add("status");
        MYSQL_KEYWORDS.add("desc");
        MYSQL_KEYWORDS.add("asc");
        MYSQL_KEYWORDS.add("primary");
        MYSQL_KEYWORDS.add("key");
        MYSQL_KEYWORDS.add("index");
        MYSQL_KEYWORDS.add("unique");
        MYSQL_KEYWORDS.add("default");
        MYSQL_KEYWORDS.add("null");
        MYSQL_KEYWORDS.add("not");
        MYSQL_KEYWORDS.add("and");
        MYSQL_KEYWORDS.add("or");
    }

    /**
     * 校验 MySQL 字段名是否合法
     *
     * @param columnName 字段名
     * @return 合法返回 true，不合法返回 false
     */
    public static boolean isValidColumnName(String columnName) {
        // 1. 空值校验
        if (columnName == null || columnName.isBlank()) {
            return false;
        }

        // 2. 长度校验 1~64
        int len = columnName.length();
        if (len > 64) {
            return false;
        }

        // 3. 首字符必须是字母
        char first = columnName.charAt(0);
        if (!Character.isLetter(first)) {
            return false;
        }

        // 4. 所有字符只能是：字母、数字、下划线、$
        for (int i = 0; i < len; i++) {
            char c = columnName.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                return false;
            }
        }

        // 5. 不能是 MySQL 关键字（忽略大小写）
        return !MYSQL_KEYWORDS.contains(columnName.toLowerCase());
    }
}