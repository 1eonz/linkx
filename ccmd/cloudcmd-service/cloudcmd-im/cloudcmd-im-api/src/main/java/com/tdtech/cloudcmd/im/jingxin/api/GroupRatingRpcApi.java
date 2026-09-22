package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatQueryQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatRespVO;

/**
 * 群组评价统计 RPC 接口
 *
 * @author
 * @date: 2026-06-08
 */
public interface GroupRatingRpcApi {

    /**
     * 获取协同案件支撑评分（按标签统计）
     *
     * @param query 查询参数
     * @return 评分统计结果
     */
    GroupRatingStatRespVO getGroupTagRating(GroupRatingStatQueryQO query);

    /**
     * 获取协同岗评分
     *
     * @param query 查询参数
     * @return 评分统计结果
     */
    GroupRatingStatRespVO getCoopUserRating(GroupRatingStatQueryQO query);
}
