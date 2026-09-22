//package com.tdtech.cloudcmd.mysql.typeHandler;
//
//import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.tdtech.cloudcmd.util.json.JsonUtil;
//import org.apache.ibatis.type.JdbcType;
//import org.apache.ibatis.type.MappedJdbcTypes;
//import org.apache.ibatis.type.MappedTypes;
//
//import java.lang.reflect.Field;
//import java.util.Set;
//
///**
// * Set<Long> 的类型转换器实现类，对应数据库的 varchar 类型
// *
// * @author 芋道源码
// */
//@MappedJdbcTypes(JdbcType.VARCHAR)
//@MappedTypes(Set.class)
//public class LongSetTypeHandler extends AbstractJsonTypeHandler<Set<Long>> {
//
//    public LongSetTypeHandler(Class<?> type) {
//        super(type);
//    }
//
//    public LongSetTypeHandler(Class<?> type, Field field) {
//        super(type, field);
//    }
//
//    @Override
//    public Set<Long> parse(String json) {
//        if (json == null || json.isBlank()) {
//            return null;
//        }
//        return JsonUtil.parseJson(json, new TypeReference<Set<Long>>() {
//        });
//    }
//
//    @Override
//    public String toJson(Set<Long> obj) {
//        if (obj == null) {
//            return null;
//        }
//        return JsonUtil.toJsonStr(obj);
//    }
//}
