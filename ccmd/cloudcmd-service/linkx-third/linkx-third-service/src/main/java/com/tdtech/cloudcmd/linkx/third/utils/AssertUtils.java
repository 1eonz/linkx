package com.tdtech.cloudcmd.linkx.third.utils;

import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AssertUtils {

    /**
     * 断言，不符合条件抛出业务异常
     * @param expression 表达式结果
     * @param message 错误信息
     */
    public static void check(final boolean expression, final String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言，不符合条件抛出登录异常
     * @param expression 表达式结果
     * @param message 错误信息
     */
    public static void checkAuth(final boolean expression, final String message) {
        if (!expression) {
            throw new SecurityUtils.UnAuthException(message);
        }
    }
}
