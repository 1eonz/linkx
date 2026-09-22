package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

import java.util.Date;

/**
 * 建群记录统计明细聚合 VO。
 *
 * <p>承载 listStaticCreateGroupByCursor 一条 SQL 关联查询出的全部字段：
 * <ul>
 *   <li>tb_create_group 主表字段（group_id/owner_id/department_id/...）</li>
 *   <li>tb_group_extends.group_type（LEFT JOIN by group_id）</li>
 * </ul>
 *
 * <p>字段命名与 SQL 别名一一对应（mybatis map-underscore-to-camel-case 自动映射）。
 * 警信用户姓名、部门 code→id 转换均需跨服务查警信 IM，无法 SQL 关联，
 * 由应用层按 owner_id / department_id 批量查 imService 补充。
 */
@Data
public class StaticCreateGroupAggVO {

    /** tb_create_group.group_id（游标 ID + 业务唯一键） */
    private Long groupId;

    /** tb_create_group.update_time（游标时间，群信息会被修改） */
    private Date updateTime;

    private String ownerId;

    private String ownerName;


    /** 源表字段，实为 IM 部门 code（非 id），需应用层查警信转 id */
    private String departmentId;

    private String departmentName;

    private Date createTime;

    private String groupName;

    /**
     * 来源 0 其他 1 一键建群 2 警单建群 3 自定义建群 4 职能建群 5 一键调度
     */
    private Integer source;

    /** tb_group_extends.group_type（LEFT JOIN by group_id） */
    private Integer groupType;
}