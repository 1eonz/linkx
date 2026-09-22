package com.tdtech.cloudcmd.im.jingxin.api.entity.tasks;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author ly
 * @date 2025/8/26 16:39
 */
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_tasks_executors")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TasksExecutors implements Serializable {

    @JsonIgnore
    private Long id;

    /**
     * 任务ID
     */
    @JsonIgnore
    private Long taskId;

    /**
     * 执行人姓名
     */
    @Schema(description = "执行人姓名")
    private String name;

    /**
     * 身份证号
     */
    @Schema(description = "身份证号")
    private String idCard;

    /**
     * 执行人所属部门
     */
    @Schema(description = "执行人所属部门")
    private String department;

    /**
     * 执行人所属部门ID
     */
    @Schema(description = "执行人所属部门ID")
    private String departmentId;

    /**
     * 执行人所属部门编码
     */
    @Schema(description = "执行人所属部门编码")
    private String departmentCode;

    /**
     * 操作时间
     */
    @Schema(description = "操作时间")
    private Date operateTime;
}