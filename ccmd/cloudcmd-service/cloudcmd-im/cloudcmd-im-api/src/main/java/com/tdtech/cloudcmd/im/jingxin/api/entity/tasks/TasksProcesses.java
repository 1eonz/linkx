package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.web.utils.JsonMapperHolder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.MappedTypes;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_tasks_processes")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TasksProcesses implements Serializable {

    @Schema(description = "id")
    private Long id;

    /**
     * 任务编号
     */
    @Schema(description = "任务编号")
    private String taskNumber;

    /**
     * 任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；
     * 
     */
    @Schema(description = "任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；")
    private Integer action;

    /**
     * 任务状态
     */
    @Schema(description = "任务状态")
    private String status;

    /**
     * 下一个处理人
     */
    @Schema(description = "下一个处理人信息")
    @TableField(typeHandler = ObjectListTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private List<TasksExecutors> nextExecutors;

    /**
     * 操作人
     */
    @Schema(description = "操作人")
    private String operatorName;

    /**
     * 操作人id
     */
    @Schema(description = "操作人id")
    private Long operatorId;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date gmtCreated;

    @MappedTypes({List.class})
    @MappedJdbcTypes(JdbcType.VARCHAR)
    public static class ObjectListTypeHandler extends AbstractJsonTypeHandler<List<TasksExecutors>> {

        public ObjectListTypeHandler(Class<?> type) {
            super(type);
        }

        public ObjectListTypeHandler(Class<?> type, Field field) {
            super(type, field);
        }

        @Override
        @SneakyThrows
        public List<TasksExecutors> parse(String json) {
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonMapperHolder.getMapper().readValue(json, new TypeReference<>() {});
        }

        @SneakyThrows
        @Override
        public String toJson(List<TasksExecutors> obj) {
            return JsonMapperHolder.getMapper().writeValueAsString(obj);
        }
    }

}