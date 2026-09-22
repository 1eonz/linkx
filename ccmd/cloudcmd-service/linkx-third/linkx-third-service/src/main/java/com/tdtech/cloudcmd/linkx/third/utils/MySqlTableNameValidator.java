package com.tdtech.cloudcmd.linkx.third.utils;

import java.util.Set;

/**
 * MySQL 表名合法性校验工具类
 * 严格遵循 MySQL 官方表名命名规范
 */
public class MySqlTableNameValidator {

    /** MySQL表名最大长度（官方标准64） */
    private static final int MAX_LENGTH = 64;
    /** 合法字符正则：字母、数字、下划线、美元符 */
    private static final String VALID_CHAR_REGEX = "^[a-zA-Z0-9_$]+$";
    /** MySQL 保留关键字（核心关键字，可自行扩展） */
    private static final Set<String> MYSQL_RESERVED_WORDS;

    static {
        // 初始化MySQL保留关键字（常用核心关键字）
        MYSQL_RESERVED_WORDS = Set.of("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "TABLE",
                "DATABASE", "INDEX", "VIEW", "TRIGGER", "PROCEDURE", "FUNCTION", "FROM", "WHERE", "GROUP", "ORDER",
                "BY", "JOIN", "INNER", "LEFT", "RIGHT", "UNION", "ALL", "DISTINCT", "LIMIT", "AND", "OR", "NOT", "NULL",
                "IS", "IN", "LIKE", "BETWEEN", "SET");
    }

    /**
     * 校验MySQL表名是否合法【对外提供的核心方法】
     * @param tableName 待校验的表名
     * @return 合法返回true，非法返回false
     */
    public static boolean isValidTableName(String tableName) {
        // 1. 空值校验
        if (tableName == null || tableName.isBlank()) {
            return false;
        }

        // 2. 长度校验（1~64位）
        int length = tableName.length();
        if (length > MAX_LENGTH) {
            return false;
        }

        // 3. 首字符校验（不能是数字）
        char firstChar = tableName.charAt(0);
        if (Character.isDigit(firstChar)) {
            return false;
        }

        // 4. 合法字符校验
        if (!tableName.matches(VALID_CHAR_REGEX)) {
            return false;
        }

        // 5. 保留关键字校验（不区分大小写）
        return !MYSQL_RESERVED_WORDS.contains(tableName.toUpperCase());
    }
}