package com.tdtech.cloudcmd.im.jingxin.api.entity.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 建群记录统计明细 DTO（im-jingxin 聚合后返回，dashboard 侧直接落库）。
 * <p>
 * 对应统计表 {@code tb_static_create_group}，源表 {@code tb_create_group} + 关联表聚合。
 * 字段与统计表 DDL 一一对应，{@code id} 由 dashboard 侧雪花算法生成，此处不携带。
 *
 * @see com.tdtech.cloudcmd.im.jingxin.api.StatisticsRpcApi#listStaticCreateGroupByCursor
 */
@Data
@Schema(description = "建群记录统计明细")
public class StaticCreateGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "创群人ID（源表 tb_create_group.owner_id，String→Long）")
    private Long userId;

    @Schema(description = "创群人姓名（查警信）")
    private String userName;

    @Schema(description = "创群人部门ID（源表 department_id 实为 code，转 id）")
    private Long userDepartmentId;

    @Schema(description = "创群人部门名称")
    private String userDepartmentName;

    @Schema(description = "群组ID（唯一键）")
    private Long groupId;

    @Schema(description = "群组名称")
    private String groupName;

    @Schema(description = "群组类型（关联 tb_group_extends.group_type）")
    private Integer groupType;

    @Schema(description = "建群方式：0其他/1一键/2警单/3自定义/4职能/5调度（源表 source）")
    private Integer groupSubType;

    @Schema(description = "建群入口：0手工建群（默认），1开放接口建群")
    private Integer groupCreateType;

    @Schema(description = "创群时间")
    private Date gmtCreateTime;

    @Schema(description = "群组的更新时间（= 游标字段）")
    private Date groupUpdatedTime;
}