package com.tdtech.cloudcmd.mysql.enhance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;

import java.util.Collection;
import java.util.List;

public interface ExBaseMapper<T> extends BaseMapper<T> {

    default CcmdPage<T> selectPageX(CcmdPageParam pageParam, LambdaQueryWrapper<T> queryWrapper) {
        var total = selectCount(queryWrapper);
        queryWrapper.last(
            " LIMIT " + (pageParam.getPageNum() - 1) * pageParam.getPageSize() + "," + pageParam.getPageSize());
        var list = selectList(queryWrapper);
        // 转换返回
        return new CcmdPage<>(pageParam.getPageNum(), pageParam.getPageSize(), total.longValue(), list);
    }

    default T selectFirstX(LambdaQueryWrapper<T> queryWrapper) {
        List<T> list = selectList(queryWrapper.last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    default T selectFirstX(SFunction<T, ?> field, Object value) {
        List<T> list = selectList(new LambdaQueryWrapper<T>().eq(field, value).last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    default T selectFirstX(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        List<T> list = selectList(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2).last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    default T selectFirstX(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2,
        SFunction<T, ?> field3, Object value3) {
        List<T> list = selectList(
            new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2).eq(field3, value3).last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    default Long selectCountX(SFunction<T, ?> field, Object value) {
        return selectCount(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectInX(SFunction<T, ?> field, Collection<?> value) {
        return selectList(new LambdaQueryWrapper<T>().in(field, value));
    }

    default List<T> selectListX(SFunction<T, ?> field, Object value) {
        return selectList(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectListX(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        return selectList(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    default int delete(SFunction<T, ?> field, Object value) {
        return delete(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default int deleteIn(SFunction<T, ?> field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<T>().in(field, values));
    }

}
