package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserGroupCare;

import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
public interface UserGroupCareService extends IService<UserGroupCare> {
    /**
     * 关注群组
     *
     * @param userId  用户ID
     * @param groupId 群组ID
     * @return 操作结果
     * @throws Exception 异常信息
     */
    boolean careGroup(Long userId, Long groupId) throws Exception;

    /**
     * 取消关注群组
     *
     * @param userId  用户ID
     * @param groupId 群组ID
     * @return 操作结果
     * @throws Exception 异常信息
     */
    boolean uncareGroup(Long userId, Long groupId) throws Exception;

    /**
     * 批量取消关注群组
     *
     * @param userId   用户ID
     * @param groupIds 群组ID列表
     * @return 操作结果
     * @throws Exception 异常信息
     */
    boolean batchUncareGroups(Long userId, List<Long> groupIds) throws Exception;

    /**
     * 查询用户关注的群组ID列表
     *
     * @param userId 用户ID
     * @return 群组ID列表
     * @throws Exception 异常信息
     */
    List<Long> getCaredGroupIds(Long userId) throws Exception;

    /**
     * 分页查询用户关注的群组列表
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页群组信息
     * @throws Exception 异常信息
     */
    IPage<Long> getCaredGroupsPage(Long userId, Integer pageNum, Integer pageSize) throws Exception;

    /**
     * 检查用户是否已关注群组
     *
     * @param userId  用户ID
     * @param groupId 群组ID
     * @return true=已关注 false=未关注
     * @throws Exception 异常信息
     */
    boolean checkIsCared(Long userId, Long groupId) throws Exception;
}
