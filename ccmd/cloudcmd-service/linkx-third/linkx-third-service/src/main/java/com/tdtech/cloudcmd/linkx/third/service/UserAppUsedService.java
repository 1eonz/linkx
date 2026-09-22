package com.tdtech.cloudcmd.linkx.third.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.linkx.third.entity.UserAppUsed;

import java.util.List;

/**
 * 北向应用使用记录表 服务类
 *
 * @author wb
 * @since 2026-05-09
 */
public interface UserAppUsedService {

    /**
     * 记录应用使用
     */
    UserAppUsed create(UserAppUsed userAppUsed);

    /**
     * 根据ID查询
     */
    UserAppUsed getById(Long id);

    /**
     * 分页查询
     */
    Page<UserAppUsed> page(Integer current, Integer size, Long userId, Long appId);

    /**
     * 查询用户使用的应用列表
     */
    List<UserAppUsed> listByUserId(Long userId);

    /**
     * 统计应用使用次数
     */
    Long countByAppId(Long appId);
}
