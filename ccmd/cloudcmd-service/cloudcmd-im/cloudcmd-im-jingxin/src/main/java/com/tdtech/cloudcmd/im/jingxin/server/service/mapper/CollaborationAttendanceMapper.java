package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @author syf
 * @date 2025/7/16
 **/
@Mapper
public interface CollaborationAttendanceMapper extends BaseMapper<CollaborationAttendance> {
    /**
     * 分页条件查询
     * 
     * @param page 分页条件
     * @param postName 协同岗名称
     * @param orgName 组织名称
     * @param personName 人员姓名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 上下岗记录列表
     */
    Page<CollaborationAttendance> selectPageWithCondition(Page<CollaborationAttendance> page,
        @Param("postName") String postName, @Param("orgName") String orgName, @Param("orgIds") List<Long> orgIds,
        @Param("personName") String personName, @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 根据协同岗id统计剩余人数
     * 
     * @param postId 协同岗id
     * @return 剩余人数
     */
    Integer getLastNumByPostId(@Param("postId") Long postId);

    /**
     * 根据协同岗id获取剩余人员
     * 
     * @param postId 协同岗id
     * @return 剩余人员
     */
    List<String> listLastPeopleByPostId(@Param("postId") Long postId);

    /**
     *
     *
     * @param postIdList 协同岗idList
     * @return 剩余人员
     */
    List<CollaborationAttendance> lastPeopleListByPostIdList(@Param("postIdList") List<Long> postIdList);

    /**
     * 根据协同岗id获取剩余推荐人员名称
     * 
     * @param postId 协同岗id
     * @param userId 排除人员id
     * @return 剩余人数
     */
    String getLastRandomPerson(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 根据协同岗id统计剩余人数
     * 
     * @param postIds 协同岗ids
     * @return 剩余人数
     */
    List<CollaborationAttendance> getLastNumByPostIds(@Param("postIds") Set<Long> postIds);

    /**
     * 根据协同岗id统计最后离岗时间
     * 
     * @param postIds 协同岗ids
     * @return 剩余人数
     */
    List<CollaborationAttendance> getLastOffDutyTimeByPostIds(@Param("postIds") Set<Long> postIds);

    /**
     * 条件查询
     * 
     * @param postName 协同岗名称
     * @param orgName 组织名称
     * @param personName 人员姓名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 上下岗记录列表
     */
    List<CollaborationAttendance> selectListByCondition(@Param("postName") String postName,
        @Param("orgName") String orgName, @Param("orgIds") List<String> orgIds, @Param("personName") String personName,
        @Param("startTime") String startTime, @Param("endTime") String endTime);

    CollaborationAttendance selectLatestRecordByPostAndPerson(@Param("postId") Long postId,
        @Param("personId") Long personId);

    /**
     * 查询最新记录为"上岗"的 personId 列表（使用窗口函数）
     *
     * @param postId 协同岗ID
     * @param personIds 人员ID列表
     * @return 最新记录为"上岗"的人员ID列表
     */
    List<Long> selectPersonIdsWithLatestOnDuty(@Param("postId") Long postId,
        @Param("personIds") List<Long> personIds);

    List<Long> selectPostIdsWithOnDutyPeople(@Param("postIds") Set<Long> postIds,
                                             @Param("personIds") Set<Long> personIds);
}
