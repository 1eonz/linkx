package com.tdtech.cloudcmd.linkx.dashboard.service;

import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatRespVO;

/**
 * 群组评价统计 Service 接口
 *
 * @author
 * @date: 2026-06-08
 */
public interface IGroupRatingStatService {

    /**
     * 获取协同案件支撑评分（按标签统计）
     *
     * @param type 评分类型（1-直接评分，2-成员评分）
     * @param departmentCode 部门编码（可选）
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd）
     * @param endTime 结束时间（可选，格式：yyyy-MM-dd）
     * @return 评分统计结果
     */
    GroupRatingStatRespVO getGroupTagRating(Integer type, String departmentCode, String startTime, String endTime);

    /**
     * 获取协同岗评分
     *
     * @param type 评分类型（1-直接评分，2-成员评分）
     * @param departmentCode 部门编码（可选）
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd）
     * @param endTime 结束时间（可选，格式：yyyy-MM-dd）
     * @return 评分统计结果
     */
    GroupRatingStatRespVO getCoopUserRating(Integer type, String departmentCode, String startTime, String endTime);
}
