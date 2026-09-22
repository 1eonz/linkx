package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

/**
 * 协同任务统计明细聚合 VO。
 *
 * <p>承载 listStaticTaskByCursor 一条 SQL 关联查询出的全部字段：
 * <ul>
 *   <li>tb_task 主表字段（id/task_id/user_id/...）</li>
 *   <li>tb_collaboration_post.post_name（LEFT JOIN by post_id）</li>
 *   <li>tb_task_expired.create_time as expired_time（LEFT JOIN by task_id）</li>
 *   <li>tb_task_response 首次回复（gmt_created/user_id/user_name，子查询取最早一条）</li>
 *   <li>tb_task_status_history track(2)/finish(3)/ignore(4) 各最早一条
 *       （gmt_created/user_id/user_name，子查询取最早一条）</li>
 * </ul>
 *
 * <p>字段命名与 SQL 别名一一对应（mybatis map-underscore-to-camel-case 自动映射）。
 * 警信用户姓名（fallback 场景）无法 SQL 关联，仍由应用层按 user_id 批量查警信补充。
 */
@Data
public class StaticTaskAggVO {

    private Long id;
    private Long taskId;
    private Long userId;
    private Long fromUserId;
    private String fromUserName;
    private Long fromUserDepartmentId;
    private String fromUserDepartmentName;
    private Integer status;
    private Date msgSentTime;
    private Long seqid;
    private Long postId;
    private String postName;
    private Date gmtCreated;
    private Date gmtModified;

    private Date expiredTime;

    private Date responseTime;
    private Long responseUserId;
    private String responseUserName;

    private Date trackTime;
    private Long trackUserId;
    private String trackUserName;

    private Date finishTime;
    private Long finishUserId;
    private String finishUserName;

    private Date ignoreTime;
    private Long ignoreUserId;
    private String ignoreUserName;
}