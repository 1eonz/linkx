package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserGroupCare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Mapper
public interface UserGroupCareMapper extends BaseMapper<UserGroupCare> {
    /**
     * 根据用户ID查询关注的群组ID列表
     * @param userId 用户ID
     * @return 群组ID列表
     */
    List<Long> selectGroupIdsByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否已关注群组
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 存在数量
     */
    int countByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 批量取消关注群组
     * @param userId 用户ID
     * @param groupIds 群组ID列表
     * @return 影响行数
     */
    int deleteByUserIdAndGroupIds(@Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);
}