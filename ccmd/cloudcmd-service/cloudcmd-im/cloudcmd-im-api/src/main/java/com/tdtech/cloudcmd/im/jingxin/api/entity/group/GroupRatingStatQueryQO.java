package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRatingStatQueryQO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 评分类型（1-直接评分，2-成员评分）
     */
    private Integer type;

    /**
     * 部门编码（可选）
     */
    private String departmentCode;

    /**
     * 开始时间（可选，格式：yyyy-MM-dd）
     */
    private String startTime;

    /**
     * 结束时间（可选，格式：yyyy-MM-dd）
     */
    private String endTime;

    /**
     * 开始时间（LocalDateTime格式，用于数据库查询）
     */
    @Builder.Default
    private LocalDateTime startTimeDateTime = null;

    /**
     * 结束时间（LocalDateTime格式，用于数据库查询）
     */
    @Builder.Default
    private LocalDateTime endTimeDateTime = null;

    /**
     * 部门id集合
     */
    private List<Long> departmentIds;

    /**
     * 部门编码集合
     */
    private List<String> departmentCodes;

    /**
     * 群组ID列表（用于筛选特定群组的评价）
     */
    private List<Long> groupIds;

    /**
     * 协同岗ID列表
     */
    private List<Long> postIds;

    /**
     * 将字符串时间转换为 LocalDateTime（在使用前调用）
     */
    public void convertTimeStrings() {
        if (startTime != null && !startTime.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(startTime, DATE_FORMATTER);
                this.startTimeDateTime = LocalDateTime.of(date, LocalTime.MIN);
            } catch (Exception ignored) {
                throw new IllegalArgumentException("开始时间格式错误，必须为yyyy-MM-dd");
            }
        }
        if (endTime != null && !endTime.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(endTime, DATE_FORMATTER);
                this.endTimeDateTime = LocalDateTime.of(date, LocalTime.MAX);
            } catch (Exception ignored) {
                throw new IllegalArgumentException("结束时间格式错误，必须为yyyy-MM-dd");
            }
        }
    }
}
