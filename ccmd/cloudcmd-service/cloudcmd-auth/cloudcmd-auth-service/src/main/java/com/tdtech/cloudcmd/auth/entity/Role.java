package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.type.JdbcType;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tdtech.cloudcmd.util.json.JsonArray;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.MappedTypes;

/**
 * <p>
 * 角色信息表
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "tb_role",autoResultMap = true)
public class Role implements Serializable {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String APPLICATIONID = "application_id";
    public static final String REMARK = "remark";
    public static final String STATUS = "status";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    /**
     * 角色ID（主键）
     */
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * 角色类型（数据字典定义） 0-超级管理员（具有一切权限） 1-管理员（可以管理管辖范围内所有信息） 2-操作员（只能管理和查看自己的信息）
     */
    private Integer type = 1;
    /**
     * icc权限
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> iccPrivJson;
    /**
     * capp权限
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> cappPrivJson;
    /**
     * admin权限
     */
    @TableField(typeHandler = StringListTypeHandler.class)
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

    @MappedTypes({List.class})
    public static class StringListTypeHandler extends AbstractJsonTypeHandler<List<String>> {

        public StringListTypeHandler(Class<?> type) {
            super(type);
        }

        public StringListTypeHandler(Class<?> type, Field field) {
            super(type, field);
        }

        @Override
        public List<String> parse(String json) {
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return JSON.parseArray(json, String.class);
        }

        @Override
        public String toJson(List<String> obj) {
            if (obj == null) {
                return null;
            }
            return JSON.toJSONString(obj);
        }
    }
}
