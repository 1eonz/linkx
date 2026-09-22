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
//import java.util.List;
//
///**
// * List<Integer> 的类型转换器实现类，对应数据库的 varchar 类型
// *
// * @author jason
// */
//@MappedJdbcTypes(JdbcType.VARCHAR)
//@MappedTypes(List.class)
//public class IntegerListTypeHandler extends AbstractJsonTypeHandler<List<Integer>> {
//    public IntegerListTypeHandler(Class<?> type) {
//        super(type);
//    }
//
//    public IntegerListTypeHandler(Class<?> type, Field field) {
//        super(type, field);
//    }
//
//    @Override
//    public List<Integer> parse(String json) {
//        if (json == null || json.isBlank()) {
//            return null;
//        }
//        return JsonUtil.parseJson(json, new TypeReference<>() {
//        });
//    }
//
//    @Override
//    public String toJson(List<Integer> obj) {
//        if (obj == null) {
//            return null;
//        }
//        return JsonUtil.toJsonStr(obj);
//    }
//}
