package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.linkx.third.entity.UserAppUsed;
import com.tdtech.cloudcmd.linkx.third.mapper.UserAppUsedMapper;
import com.tdtech.cloudcmd.linkx.third.service.UserAppUsedService;
import com.tdtech.cloudcmd.util.DateUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 北向应用使用记录表 服务实现类
 *
 * @author wb
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class UserAppUsedServiceImpl extends ServiceImpl<UserAppUsedMapper, UserAppUsed> implements UserAppUsedService {

    private final IdWorker idWorker;

    @Override
    public UserAppUsed create(UserAppUsed userAppUsed) {
        userAppUsed.setId(idWorker.nextId());
        if (userAppUsed.getTime() == null) {
            userAppUsed.setTime(DateUtils.of(new Date()));
        }
        userAppUsed.setGmtCreated(DateUtils.of(new Date()));
        save(userAppUsed);
        return userAppUsed;
    }

    @Override
    public UserAppUsed getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Page<UserAppUsed> page(Integer current, Integer size, Long userId, Long appId) {
        LambdaQueryWrapper<UserAppUsed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, UserAppUsed::getUserId, userId)
                .eq(appId != null, UserAppUsed::getAppId, appId)
                .orderByDesc(UserAppUsed::getTime);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public List<UserAppUsed> listByUserId(Long userId) {
        return lambdaQuery()
                .eq(UserAppUsed::getUserId, userId)
                .orderByDesc(UserAppUsed::getTime)
                .list();
    }

    @Override
    public Long countByAppId(Long appId) {
        return lambdaQuery()
                .eq(UserAppUsed::getAppId, appId)
                .count();
    }
}
