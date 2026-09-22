package com.tdtech.cloudcmd.admin.resource.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tdtech.cloudcmd.web.utils.JsonMapperHolder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.MappedTypes;

/**
 * <p>
 * 执行者资源信息表（包括人和车辆，车辆作为执行主体，是执行者。作为驾驶工具时，是装备）
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_executor")
public class Executor implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 警员编号
     */
    private String code;
    /**
     * 真实姓名
     */
    private String name;

    /**
     * 名称拼音简写  便于搜索
     */
    private String pyName;
    /**
     * 组织ID
     */
    private Long organizationId;
    /**
     * 扩展字段
     */
    @TableField(typeHandler = ExecutorExtendInfoTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private ExecutorExtendInfo extendInfo;
    /**
     * REPERESERVE
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    /**
     * 创建时间
     */
    private Date gmtCreated;
    /**
     * 修改时间
     */
    private Date gmtModified;
    @TableField(updateStrategy = FieldStrategy.ALWAYS, insertStrategy = FieldStrategy.ALWAYS)
    private String phoneNum;
    @TableField(updateStrategy = FieldStrategy.ALWAYS, insertStrategy = FieldStrategy.ALWAYS)
    private String account;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String emailAddr;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String headShot;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String idCardNum;

    /**
     * 用户类型）0-超级管理员 1-个人用户 2-DELETED
     */
    private Integer type = 1;

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ExecutorExtendInfo {
        @Schema(description = "性别")
        private ExecutorSexEnum sex;// 性别
        @Schema(description = "单位（DEPRECATED）")
        private String company;// 单位
        @Schema(description = "职级")
        private String rank;// 职级
        @Schema(description = "软终端")
        private String softTerminal;// 软终端
        @Schema(description = "座机")
        private String landingPhone;// 座机
        @Schema(description = "是否领导")
        private Integer leader;// 是否领导

        @Override
        public String toString(){
            String strbuf = "'{sex=" + sex + ",leader=" + leader + ",company=" + company + ",rank=" + rank +
                    ",softTerminal=" + softTerminal + ",landingPhone=" + landingPhone + "}'";
            return strbuf;
        }
    }

    public enum ExecutorSexEnum {
        MALE("GENDER_MALE"), FEMALE("GENDER_FEMALE");

        private final String i18n;

        ExecutorSexEnum(String i18n) {
            this.i18n = i18n;
        }

        @JsonValue
        public String i18n() {
            return i18n;
        }

        @JsonCreator
        public static ExecutorSexEnum i18nOf(String i18n) {
            for (ExecutorSexEnum e : ExecutorSexEnum.values()) {
                if (Objects.equals(e.i18n, i18n)) {
                    return e;
                }
            }
            return null;
        }
    }

    @MappedTypes(ExecutorExtendInfo.class)
    public static class ExecutorExtendInfoTypeHandler extends AbstractJsonTypeHandler<ExecutorExtendInfo> {

        public ExecutorExtendInfoTypeHandler() {
            super(ExecutorExtendInfo.class);
        }

        public ExecutorExtendInfoTypeHandler(Class<?> type) {
            super(type);
        }

        @Override
        @SneakyThrows
        public ExecutorExtendInfo parse(String json) {
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonMapperHolder.getMapper().readValue(json, ExecutorExtendInfo.class);
        }

        @Override
        @SneakyThrows
        public String toJson(ExecutorExtendInfo obj) {
            if (obj == null) {
                return null;
            }
            return JsonMapperHolder.getMapper().writeValueAsString(obj);
        }
    }
}
