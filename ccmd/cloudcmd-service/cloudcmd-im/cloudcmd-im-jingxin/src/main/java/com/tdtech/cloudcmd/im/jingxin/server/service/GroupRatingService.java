package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.GroupRating;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupRatingReqDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.GroupRatingListRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.GroupRatingRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.GroupRatingStatusRespVO;

import java.util.List;

/**
 * 群组评价服务接口
 * 提供群组协同岗评价相关功能
 */
public interface GroupRatingService extends IService<GroupRating> {

    /**
     * 获取当前用户对指定群组的已评价列表
     *
     * @param groupId 群组ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param currentUserId 当前用户ID
     * @return 评价列表响应对象
     */
    GroupRatingListRespVO getRatedList(Long groupId, Integer pageNum, Integer pageSize, Long currentUserId);

    /**
     * 对已归档群组的协同岗发起评分
     *
     * @param groupId 群组ID
     * @param reqDTO 评价请求对象
     * @param currentUserId 当前用户ID
     * @return 评价响应对象
     */
    GroupRatingRespVO createRating(Long groupId, GroupRatingReqDTO reqDTO, Long currentUserId);

    /**
     * 获取指定群组的协同岗列表
     *
     * @param groupId 群组ID
     * @return 协同岗列表
     */
    List<CoopUserVO> getCoopUserList(Long groupId);

    /**
     * 查询当前用户对指定群组的评价状态
     *
     * @param groupId 群组ID
     * @param currentUserId 当前用户ID
     * @return 评价状态响应对象
     */
    GroupRatingStatusRespVO getRatingStatus(Long groupId, Long currentUserId);
}
