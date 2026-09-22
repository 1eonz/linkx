package com.tdtech.cloudcmd.im.jingxin.api.entity.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 协同任务统计明细 DTO（im-jingxin 聚合后返回，dashboard 侧直接落库）。
 * <p>
 * 对应统计表 {@code tb_static_task}，源表 {@code tb_task} + 关联表聚合。
 * 字段与统计表 DDL 一一对应，{@code id} 由 dashboard 侧雪花算法生成，此处不携带。
 *
 * @see com.tdtech.cloudcmd.im.jingxin.api.StatisticsRpcApi#listStaticTaskByCursor
 */
@Data
@Schema(description = "协同任务统计明细")
public class StaticTaskDTO implements Serializable {

    private final static long serialVersionUID = 1L;

    @Schema(description = "任务ID（源表 tb_task.id）")
    private Long taskId;

    @Schema(description = "任务发起人ID")
    private Long fromUserId;

    @Schema(description = "任务发起人姓名")
    private String fromUserName;

    @Schema(description = "任务发起人部门ID")
    private Long fromUserDepartmentId;

    @Schema(description = "任务发起人部门名称")
    private String fromUserDepartmentName;

    @Schema(description = "任务状态：-1待分配/1待办/2跟踪/3办结/4忽略/7未及时回复/8已逾期")
    private Integer status;

    @Schema(description = "任务关联的警信消息发送时间")
    private Date msgSentTime;

    @Schema(description = "任务关联的警信消息ID")
    private Long msgSeqid;

    @Schema(description = "任务分配的协同岗ID")
    private Long postId;

    @Schema(description = "任务分配的协同岗名称（关联 tb_collaboration_post.post_name）")
    private String postName;

    @Schema(description = "任务创建时间")
    private Date createTime;

    @Schema(description = "任务逾期时间（关联 tb_task_expired）")
    private Date expiredTime;

    @Schema(description = "任务首次回复时间")
    private Date responseTime;

    @Schema(description = "任务首次回复警信用户ID")
    private Long responseUserId;

    @Schema(description = "任务首次回复警信用户姓名")
    private String responseUserName;

    @Schema(description = "任务忽略时间")
    private Date ignoreTime;

    @Schema(description = "任务忽略操作人ID")
    private Long ignoreUserId;

    @Schema(description = "任务忽略操作人姓名")
    private String ignoreUserName;

    @Schema(description = "任务跟踪时间")
    private Date trackTime;

    @Schema(description = "任务跟踪操作人ID")
    private Long trackUserId;

    @Schema(description = "任务跟踪操作人姓名")
    private String trackUserName;

    @Schema(description = "任务完成时间")
    private Date finishTime;

    @Schema(description = "任务完成操作人ID")
    private Long finishUserId;

    @Schema(description = "任务完成操作人姓名")
    private String finishUserName;

    @Schema(description = "任务的更新时间（= 游标字段）")
    private Date taskUpdatedTime;
}