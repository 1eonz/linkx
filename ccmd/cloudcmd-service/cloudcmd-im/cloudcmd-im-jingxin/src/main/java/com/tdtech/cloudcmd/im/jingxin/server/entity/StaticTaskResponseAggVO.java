package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

/**
 * 任务回复统计明细聚合 VO。
 *
 * <p>承载 listStaticTaskResponseByCursor 一条 SQL 关联查询出的全部字段：
 * <ul>
 *   <li>tb_task_response 主表字段（id/task_id/user_id/user_name/department_id/department_name/post_id/gmt_created/seqid）</li>
 *   <li>tb_collaboration_post.post_name（LEFT JOIN by post_id）</li>
 * </ul>
 *
 * <p>字段命名与 SQL 别名一一对应（mybatis map-underscore-to-camel-case 自动映射）。
 * post_name 无需别名（本身就是下划线），其余主表字段直接用源表列名。
 */
@Data
public class StaticTaskResponseAggVO {

    private Long id;
    private Long taskId;
    private Long userId;
    private String userName;
    private Long departmentId;
    private String departmentName;
    private Long postId;
    private Date gmtCreated;
    private Long seqid;

    private String postName;
}