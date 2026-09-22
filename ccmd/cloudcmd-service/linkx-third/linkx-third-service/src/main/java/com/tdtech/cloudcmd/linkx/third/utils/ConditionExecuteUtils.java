package com.tdtech.cloudcmd.linkx.third.utils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 条件执行工具类
 * 根据布尔值决定是否执行传入的代码逻辑
 */
public class ConditionExecuteUtils {

    /**
     * 条件执行：无返回值、无参数
     * @param condition 执行条件
     * @param runnable 待执行的代码块
     */
    public static void execute(boolean condition, Runnable runnable) {
        if (condition && runnable != null) {
            runnable.run();
        }
    }

    /**
     * 条件执行：有返回值
     * @param condition 执行条件
     * @param supplier 待执行的代码（带返回值）
     * @return 执行结果 / null（不执行时）
     */
    public static <T> T execute(boolean condition, Supplier<T> supplier) {
        if (condition && supplier != null) {
            return supplier.get();
        }
        return null;
    }

    /**
     * 条件执行：带一个参数
     * @param condition 执行条件
     * @param param 参数
     * @param consumer 待执行的代码（接收一个参数）
     * @param <T> 参数类型
     */
    public static <T> void execute(boolean condition, T param, Consumer<T> consumer) {
        if (condition && consumer != null) {
            consumer.accept(param);
        }
    }
}