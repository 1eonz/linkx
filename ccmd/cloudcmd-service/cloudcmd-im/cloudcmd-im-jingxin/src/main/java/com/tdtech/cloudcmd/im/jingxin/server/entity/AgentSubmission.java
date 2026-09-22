package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@TableName(value = "tb_ai_agent_submission",autoResultMap = true)
@Data
@ToString
@Schema(description = "申请实体")
public class AgentSubmission {

    @Schema(description = "主键ID")
    @TableId
    private Long id;

    @Schema(description = "申请人ID")
    private Long fromId;

    @Schema(description = "申请人名称")
    private String fromName;

    @Schema(description = "申请人部门ID")
    private Long fromDepId;

    @Schema(description = "申请人部门名称")
    private String fromDepName;

    @Schema(description = "审批人ID")
    private Long toId;

    @Schema(description = "审批人姓名")
    private String toName;

    @Schema(description = "申请资源")
    @TableField(typeHandler = ResourcesTypeHandler.class)
    private List<SubmissionResource> resources;

    @Schema(description = "申请开始日期")
    private LocalDate fromDate;

    @Schema(description = "申请结束日期")
    private LocalDate toDate;

    @Schema(description = "申请状态")
    @TableField(typeHandler = AgentSubmissionStatusEnumTypeHandler.class)
    private AgentSubmissionStatusEnum status;

    @TableField("`desc`")
    @Schema(description = "申请描述")
    private String desc;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "审批时间")
    private LocalDateTime submissionTime;

    @Schema(description = "扩展字段")
    private String ext;

    @Schema(description = "回复")
    private String reply;

    @Data
    @ToString
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class SubmissionResource {
        @Schema(description = "类型")
        private Integer type;
        @Schema(description = "扩展字段，想存啥存啥")
        private String ext;
    }

    @RequiredArgsConstructor
    @Schema(description = "智能体申请状态枚举")
    public enum AgentSubmissionStatusEnum {
        @Schema(description = "初始化(提交)") INIT(0), @Schema(description = "接受") ACCEPT(1),
        @Schema(description = "拒绝") REFUSE(2);

        private final int code;

        @JsonCreator
        public static AgentSubmissionStatusEnum fromCode(Integer code) {
            if (code == null) {
                return null;
            }
            for (var value : AgentSubmissionStatusEnum.values()) {
                if (value.code == code) {
                    return value;
                }
            }
            return null;
        }

        @JsonGetter
        @JsonValue
        public Integer code() {
            return this.code;
        }

    }

    public static class AgentSubmissionStatusEnumTypeHandler extends BaseTypeHandler<AgentSubmissionStatusEnum> {

        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, AgentSubmissionStatusEnum parameter,
            JdbcType jdbcType) throws SQLException {
            ps.setInt(i, parameter.code);
        }

        @Override
        public AgentSubmissionStatusEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
            var code = rs.getInt(columnName);
            return AgentSubmissionStatusEnum.fromCode(code);
        }

        @Override
        public AgentSubmissionStatusEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            var code = rs.getInt(columnIndex);
            return AgentSubmissionStatusEnum.fromCode(code);
        }

        @Override
        public AgentSubmissionStatusEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            var code = cs.getInt(columnIndex);
            return AgentSubmissionStatusEnum.fromCode(code);
        }
    }

    @MappedTypes({List.class})
    public static class ResourcesTypeHandler extends AbstractJsonTypeHandler<List<SubmissionResource>> {

        public ResourcesTypeHandler(Class<?> type) {
            super(type);
        }

        public ResourcesTypeHandler(Class<?> type, Field field) {
            super(type, field);
        }

        @Override
        public List<SubmissionResource> parse(String json) {
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtil.parseArrayJson(json, SubmissionResource.class);
        }

        @Override
        public String toJson(List<SubmissionResource> obj) {
            if (obj == null) {
                return null;
            }
            return JsonUtil.toJsonStr(obj);
        }
    }

}
