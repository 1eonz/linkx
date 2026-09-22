package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.Data;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

@Data
@TableName(value = "tb_label_binding", autoResultMap = true)
public class LabelBinding {

    @TableId
    private Long id;
    private Long labelId;
    private Long departmentId;
    private String departmentName;
    private String departmentPath;
    @TableField(value = "post_ids", typeHandler = PostIdsTypeHandler.class)
    private List<Long> postIds;

    @MappedTypes({List.class})
    public static class PostIdsTypeHandler extends AbstractJsonTypeHandler<List<Long>> {

        public PostIdsTypeHandler(Class<?> type) {
            super(type);
        }

        public PostIdsTypeHandler(Class<?> type, Field field) {
            super(type, field);
        }

        @Override
        public List<Long> parse(String json) {
            return JsonUtil.parseArrayJson(json, Long.class);
        }

        //MYSQL 在JSON_SEARCH的时候不能处理数字的数组，需要转换成字符串处理
        @Override
        public String toJson(List<Long> obj) {
            if (obj == null) {
                return null;
            }
            var collect = obj.stream().map(Object::toString).collect(Collectors.toList());
            return JsonUtil.toJsonStr(collect);
        }
    }
}
