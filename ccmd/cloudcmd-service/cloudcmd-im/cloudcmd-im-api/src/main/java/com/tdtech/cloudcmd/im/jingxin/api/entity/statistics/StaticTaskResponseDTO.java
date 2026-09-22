package com.tdtech.cloudcmd.im.jingxin.api.entity.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务回复统计明细 DTO（im-jingxin 聚合后返回，dashboard 侧直接落库）。
 * <p>
 * 对应统计表 {@code tb_static_task_response}，源表 {@code tb_task_response} + 关联表聚合。
 * 字段与统计表 DDL 一一对应，{@code id} 由 dashboard 侧雪花算法生成，此处不携带。
 *
 * @see com.tdtech.cloudcmd.im.jingxin.api.StatisticsRpcApi#listStaticTaskResponseByCursor
 */
@Data
@Schema(description = "任务回复统计明细")
public class StaticTaskResponseDTO implements Serializable {

    private final static long serialVersionUID = 1L;

    @Schema(description = "回复的任务ID")
    private Long taskId;

    @Schema(description = "任务回复人ID")
    private Long responseUserId;

    @Schema(description = "任务回复人姓名")
    private String responseUserName;

    @Schema(description = "任务回复人部门ID")
    private Long responseUserDepartmentId;

    @Schema(description = "任务回复人部门名称")
    private String responseUserDepartmentName;

    @Schema(description = "回复的任务协同岗ID（源表 tb_task_response.post_id）")
    private Long responseCoopUserId;

    @Schema(description = "回复的任务协同岗名称（关联 tb_collaboration_post.post_name）")
    private String responseCoopUserName;

    @Schema(description = "任务回复时间（= 游标字段）")
    private Date responseTime;

    @Schema(description = "任务回复的警信消息ID")
    private Long responseMsgSeqid;

    @Schema(description = "任务回复源表ID（tb_task_response.id，唯一键）")
    private Long responseId;
}