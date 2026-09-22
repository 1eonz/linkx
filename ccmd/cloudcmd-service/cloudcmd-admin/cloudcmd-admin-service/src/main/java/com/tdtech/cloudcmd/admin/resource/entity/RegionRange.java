package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.MappedTypes;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_region_range")
public class RegionRange implements Serializable {


    /**
     * 辖区id
     */
    private Long id;

    /**
     * 地理坐标系(不同的地图有不同的坐标系) 默认值gis
     */
    private String gcs;

    /**
     * 0-未知;4001:固定辖区;4002:巡逻辖区;4003:网格辖区;4004:固定巡控点
     */
    private String category;

    /**
     * 辖区编码 唯一不重复
     */
    @NotBlank
    private String code;

    /**
     * 辖区名称
     */
    @NotBlank
    private String name;

    /**
     * 辖区简称
     */
    private String shortName;

    /**
     * 辖区多边形地理数据
     */
    @TableField(typeHandler = RegionRange.JsonObjectListTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private JSONArray polygon;

    /**
     * 父级辖区parentId
     */
    private Long parentId;

    /**
     * 全路径
     */
    private String fullPath;

    /**
     * 全名称
     */
    private String fullName;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 附加样式
     */
    @NotBlank
    private String style;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：0-正常,1-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtCreated;

    private Long creator;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtModified;

    @TableField(exist = false)
    List<RegionRange> children;

    @TableField(exist = false)
    private String parentCode;


    @MappedTypes({JSONArray.class})
    @MappedJdbcTypes(JdbcType.VARCHAR)
    public static class JsonObjectListTypeHandler extends AbstractJsonTypeHandler<JSONArray> {

        public JsonObjectListTypeHandler() {
            super(JSONArray.class);
        }

        public JsonObjectListTypeHandler(Class<?> type) {
            super(type);
        }

        public JsonObjectListTypeHandler(Class<?> type, Field field) {
            super(type, field);
        }

        @Override
        @SneakyThrows
        public JSONArray parse(String json) {
            if (json == null || json.isBlank()) {
                return null;
            }
            return JSON.parseArray(json);
        }

        @Override
        @SneakyThrows
        public String toJson(JSONArray obj) {
            if (obj == null) {
                return null;
            }
            return JSONObject.toJSONString(obj);
        }
    }

}
