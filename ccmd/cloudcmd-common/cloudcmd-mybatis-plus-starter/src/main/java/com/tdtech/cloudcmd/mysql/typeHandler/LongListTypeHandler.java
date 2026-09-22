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
// * List<Long> 的类型转换器实现类，对应数据库的 varchar 类型
// *
// * @author 芋道源码
// */
//@MappedJdbcTypes(JdbcType.VARCHAR)
//@MappedTypes(List.class)
//public class LongListTypeHandler extends AbstractJsonTypeHandler<List<Long>> {
//
//    public LongListTypeHandler(Class<?> type) {
//        super(type);
//    }
//
//    public LongListTypeHandler(Class<?> type, Field field) {
//        super(type, field);
//    }
//
//    @Override
//    public List<Long> parse(String json) {
//        if (json == null || json.isBlank()) {
//            return null;
//        }
//        return JsonUtil.parseJson(json, new TypeReference<>() {
//        });
//    }
//
//    @Override
//    public String toJson(List<Long> obj) {
//        if (obj == null) {
//            return null;
//        }
//        return JsonUtil.toJsonStr(obj);
//    }
//
//}
