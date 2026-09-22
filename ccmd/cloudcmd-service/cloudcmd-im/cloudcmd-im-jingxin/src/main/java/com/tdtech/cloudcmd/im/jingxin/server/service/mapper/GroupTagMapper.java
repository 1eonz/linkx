package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupTagVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.GroupTag;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TagCountDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Mapper
public interface GroupTagMapper extends BaseMapper<GroupTag> {

    /**
     * Delete tag relations by group ID
     * @param groupId group ID
     * @return number of deleted records
     */
    int deleteByGroupId(Long groupId);

    int updateByGroupId(Long groupId);

    List<TagCountDTO> getCountGroupTags(
            @Param("userId") Long userId,
            @Param("departmentCodeList") List<String> departmentCodeList,
            @Param("orgIds") List<String> orgIds,
            @Param("startTime")String startTime,
            @Param("endTime")String endTime,
            @Param("postIdList") List<String> postIdList);

    List<Long> selectListByOrgIds(@Param("orgIds") List<String> orgIds, @Param("userId")Long userId);

    TagCountDTO getAllGroup(
            @Param("userId") Long userId,
            @Param("departmentCodeList") List<String> departmentCodeList,
            @Param("orgIds") List<String> orgIds,
            @Param("startTime")String startTime,
            @Param("endTime")String endTime,
            @Param("postIdList") List<String> postIdList);

    TagCountDTO getAllGroupCount(
            @Param("userId") Long userId,
            @Param("departmentCodeList") List<String> departmentCodeList,
            @Param("orgIds") List<String> orgIds,
            @Param("startTime")String startTime,
            @Param("endTime")String endTime,
            @Param("postIdList") List<String> postIdList);

    List<GroupTagVO> listTagsByGroupIds(@Param("groupIds")List<Long> groupIds);

    /**
     * 查询指定时间范围存在的群组标签关联信息
     *
     * @param endTime 关联关系存在的结束时间
     * @return 群组标签关联信息列表
     */
    List<GroupTagVO> listTagsByTimeRange(@Param("endTime") LocalDateTime endTime,
                                         @Param("departmentCodes") List<String> departmentCodes);
}

