package com.tdtech.cloudcmd.im.jingxin.api.entity.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 协同岗上下岗统计明细 DTO（im-jingxin 聚合后返回，dashboard 侧直接落库）。
 * <p>
 * 对应统计表 {@code tb_static_coop_duty_switch}，源表 {@code tb_collaboration_attendance}。
 * 字段与统计表 DDL 一一对应，{@code id} 由 dashboard 侧雪花算法生成，此处不携带。
 * <p>
 * user_id 策略：仅 switchType=0（手工切换）取 person_id 作为操作人，其余留空。
 *
 * @see com.tdtech.cloudcmd.im.jingxin.api.StatisticsRpcApi#listStaticCoopDutySwitchByCursor
 */
@Data
@Schema(description = "协同岗上下岗统计明细")
public class StaticCoopDutySwitchDTO implements Serializable {
    private final static long serialVersionUID = 1L;

    @Schema(description = "切换发起人ID（仅 switchType=0 取 person_id，其余 null）")
    private Long userId;

    @Schema(description = "切换发起人姓名（仅 switchType=0 取，其余 null）")
    private String userName;

    @Schema(description = "切换发起人部门ID（仅 switchType=0 取 org_id，其余 null）")
    private Long userDepartmentId;

    @Schema(description = "切换发起人部门名称（仅 switchType=0 取，其余 null）")
    private String userDepartmentName;

    @Schema(description = "切换发起的协同岗ID（源表 post_id，一直有值）")
    private Long coopUserId;

    @Schema(description = "切换发起的协同岗名称（源表 post_name，一直有值）")
    private String coopUserName;

    @Schema(description = "切换类型：0手工/1IM状态/2值班自动/3管理员下岗/99其他（源表 switch_type）")
    private Integer swithType;

    @Schema(description = "切换方式：0上岗/1下岗（源表 type 转换）")
    private Integer switchFlag;

    @Schema(description = "切换时间（= 游标字段）")
    private Date gmtCreateTime;

    @Schema(description = "上下岗源表ID（tb_collaboration_attendance.id，唯一键）")
    private Long attendanceId;
}