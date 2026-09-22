package com.tdtech.cloudcmd.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;

import com.google.common.collect.Iterables;

/**
 * Static convenience methods for JavaBeans: copy source JavaBean properties to target JavaBean properties.
 *
 */
public class BeanCopyUtils {

    private BeanCopyUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 对象转换
     *
     * @param source 待转换源对象
     * @param target 目标对象
     * @param <T> 目标对象类型
     */
    public static <T> T copyBean(Object source, T target) {
        if (source == null || target == null) {
            return null;
        }
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * 转换对象
     *
     * @param source 源对象
     * @param targetSupplier 目标对象供应方
     * @param callBack 回调方法
     * @param <S> 源对象类型
     * @param <T> 目标对象类型
     * @return 目标对象
     */
    public static <S, T> T copyBean(S source, Supplier<T> targetSupplier, ConvertCallBack<S, T> callBack) {
        if (null == source || null == targetSupplier) {
            return null;
        }
        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target);
        if (callBack != null) {
            callBack.callBack(source, target);
        }
        return target;
    }

    public static <S, T> T copyBean(S source, Supplier<T> targetSupplier) {
        return copyBean(source, targetSupplier, null);
    }


    public static <S, T> List<T> copyList(Collection<S> source, Supplier<T> targetSupplier, boolean retainNull) {
        return copyList(source, targetSupplier, retainNull, null);
    }

    public static <S, T> List<T> copyList(Collection<S> source, Supplier<T> targetSupplier,
        ConvertCallBack<S, T> callBack) {
        return copyList(source, targetSupplier, false, callBack);
    }

    public static <S, T> List<T> copyList(Collection<S> source, Supplier<T> targetSupplier) {
        return copyList(source, targetSupplier, false);
    }

    /**
     * 转换集合
     *
     * @param sources 源对象list
     * @param targetSupplier 目标对象供应方
     * @param retainNull 返回结果是否保留null值
     * @param callBack 回调方法
     *
     * @param <S> 源对象类型
     * @param <T> 目标对象类型
     * @return 目标对象list
     */
    public static <S, T> List<T> copyList(Collection<S> sources, Supplier<T> targetSupplier, boolean retainNull,
        ConvertCallBack<S, T> callBack) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(targetSupplier);
        var result = new ArrayList<T>();
        for (S s : sources) {
            var t = targetSupplier.get();
            if (s == null || t == null) {
                result.add(null);
                if (!retainNull) {
                    Iterables.removeIf(result, Objects::isNull);
                }
                continue;
            }
            BeanUtils.copyProperties(s, t);
            if (callBack != null) {
                callBack.callBack(s, t);
            }
            result.add(t);
        }
        return result;
    }

    /**
     * 回调接口 --用于定制化转换
     *
     * @param <S> 源对象类型
     * @param <T> 目标对象类型
     */
    @FunctionalInterface
    public interface ConvertCallBack<S, T> {
        void callBack(S t, T s);
    }
}
