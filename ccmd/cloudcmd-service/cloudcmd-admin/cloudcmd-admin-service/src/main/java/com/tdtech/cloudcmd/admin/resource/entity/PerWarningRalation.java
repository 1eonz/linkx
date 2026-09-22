package com.tdtech.cloudcmd.admin.resource.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * @author: S063874
 * @date: 2026-01-13 14:45
 */
@TableName("tb_pre_warning_ralation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerWarningRalation {
    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 预警类型：1、任务逾期，2、无人值守
     */
    private Integer classify;
    /**
     * 警信组织部门id或协调岗id
     */
    private Long businessId;
    /**
     * 预警通知的目标：1、用户，2、群组
     */
    private Integer targetType;
    /**
     * 需要被通知的用户id或群组id
     */
    private Long targetId;

    /**
     * 警信组织部门名称或协同岗名称
     */
    private String businessName;

    /**
     * 用户或者群组名称
     */
    private String targetName;

    /**
     * 父组织或所属组织名称，1、任务逾期时为父组织名称，2、无人值守时为协同岗组织名称
     */
    private String orgName;

    /**
     *  身份证号，目标类型为用户时需要用来发送消息
     */
    private String idCard;

}
