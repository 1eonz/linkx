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
// * List<String> 的类型转换器实现类，对应数据库的 varchar 类型
// *
// * @author 永不言败
// * @since 2022 3/23 12:50:15
// */
//@MappedJdbcTypes(JdbcType.VARCHAR)
//@MappedTypes(List.class)
//public class StringListTypeHandler extends AbstractJsonTypeHandler<List<String>> {
//    public StringListTypeHandler(Class<?> type) {
//        super(type);
//    }
//
//    public StringListTypeHandler(Class<?> type, Field field) {
//        super(type, field);
//    }
//
//    @Override
//    public List<String> parse(String json) {
//        if (json == null || json.isBlank()) {
//            return null;
//        }
//        return JsonUtil.parseJson(json, new TypeReference<>() {
//        });
//    }
//
//    @Override
//    public String toJson(List<String> obj) {
//        if (obj == null) {
//            return null;
//        }
//        return JsonUtil.toJsonStr(obj);
//    }
//}
