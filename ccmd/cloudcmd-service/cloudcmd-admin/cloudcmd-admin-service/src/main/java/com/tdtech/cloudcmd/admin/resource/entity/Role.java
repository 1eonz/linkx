package com.tdtech.cloudcmd.admin.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.web.utils.JsonMapperHolder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 角色信息表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "tb_role", autoResultMap = true)
@ToString
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 角色ID（主键）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * 角色类型 0-超级管理员 1-一般管理员 2-协同角色
     */
    private Integer type;
    /**
     * icc权限
     */
    @TableField(typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private List<String> iccPrivJson;
    /**
     * capp权限
     */
    @TableField(typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private List<String> cappPrivJson;
    /**
     * admin权限
     */
    @TableField(typeHandler = StringListTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private List<String> adminPrivJson;

    @TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private JsonArray imOrgPrivJson;
    /**
     * 状态 0-可用,1-禁用
     */
    private Integer status = 0;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 修改时间
     */
    private Date gmtModified;

    @MappedTypes(List.class)
    @MappedJdbcTypes(JdbcType.VARCHAR)
    public static class StringListTypeHandler extends AbstractJsonTypeHandler<List<String>> {

        public StringListTypeHandler() {
            super(List.class);
        }

        public StringListTypeHandler(Class<?> type) {
            super(type);
        }

        public StringListTypeHandler(Class<?> type, Field field) {
            super(type, field);
        }

        @Override
        @SneakyThrows
        public List<String> parse(String json) {
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonMapperHolder.getMapper().readValue(json, new TypeReference<>() {
            });
        }

        @Override
        @SneakyThrows
        public String toJson(List<String> obj) {
            if (obj == null) {
                return null;
            }
            return JsonMapperHolder.getMapper().writeValueAsString(obj);
        }
    }
}
