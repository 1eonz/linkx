package com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author syf
 * @date 2025/7/16
 **/
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_collaboration_attendance")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CollaborationAttendance implements Serializable {
    @Schema(description = "主键")
    @ExcelIgnore
    private Long id;
    /**
     * 协同岗id
     */
    @Schema(description = "协同岗id")
    @ExcelIgnore
    private Long postId;
    /**
     * 协同岗名称
     */
    @Schema(description = "协同岗名称")
    @ExcelProperty("协同岗名称")
    private String postName;
    /**
     * 所属组织ID
     */
    @Schema(description = "所属组织ID")
    @ExcelIgnore
    private Long orgId;
    /**
     * 所属组织名称
     */
    @Schema(description = "所属组织")
    @ExcelProperty("所属组织")
    private String orgName;
    /**
     * 人员ID
     */
    @Schema(description = "人员ID")
    @ExcelIgnore
    private Long personId;
    /**
     * 人员名称
     */
    @Schema(description = "协同岗人员")
    @ExcelProperty("协同岗人员")
    private String personName;
    /**
     * 剩余人数(冗余字段，如需展示每条记录对应快照的人数信息则在插库时实时计算该字段)
     */
    @Schema(description = "剩余在岗人数")
    @ExcelProperty("剩余在岗人数")
    private Integer lastPeopleNum;
    /**
     * 剩余人员(冗余字段，如需展示每条记录对应快照的人数信息则在插库时实时计算该字段)
     */
    @Schema(description = "剩余在岗人员")
    @ExcelProperty("剩余在岗人员")
    private String lastPeople;
    /**
     * 上下岗类型：上岗、下岗
     */
    @Schema(description = "上下岗类型")
    @ExcelProperty("上下岗类型")
    private String type;
    /**
     * 创建时间
     */
    @Schema(description = "上下岗时间")
    @ExcelProperty("上下岗时间")
    private Date createTime;
    /**
     * 备注
     */
    @Schema(description = "备注")
    @ExcelIgnore
    private String remark;

    /**
     * 数据来源：0手工切换（default），1IM状态变化切换，2值班自动上下岗，3其他
     */
    @Schema(description = "数据来源")
    @ExcelProperty("数据来源")
    private Integer switchType;
}