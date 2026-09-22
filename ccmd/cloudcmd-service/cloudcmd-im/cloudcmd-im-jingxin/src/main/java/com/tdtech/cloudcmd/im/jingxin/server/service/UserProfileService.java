package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile.UserProfileCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile.UserProfileVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserProfile;

/**
 * 用户偏好信息表 服务类
 *
 * @author wb
 * @since 2026-05-09
 */
public interface UserProfileService {

    /**
     * 创建或更新用户偏好
     */
    void createOrUpdate(UserProfileCreateReq createReq);

    UserProfileVo getProfile(Long userId, Integer terminalType,Integer scope);
    /**
     * 根据ID查询
     */
    UserProfile getById(Long id);

    /**
     * 根据用户ID查询
     */
    UserProfile getByUserId(Long userId);

    /**
     * 更新App展示方式
     */
    void updateAppSortType(Long userId, Integer appSortType);

    /**
     * 更新App自定义排序
     */
    void updateAppCustomSort(Long userId, java.util.List<Long> appCustomSort);

    Integer getSortedType(Long userId);
}
