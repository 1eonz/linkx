package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallableSync;
import com.tdtech.cloudcmd.linkx.third.mapper.AppCallableSyncMapper;
import com.tdtech.cloudcmd.linkx.third.service.IAppCallableSyncService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.linkx.third.utils.ConditionExecuteUtils;
import com.tdtech.cloudcmd.util.DateUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * 南向应用数据同步信息表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Service
@Slf4j
public class AppCallableSyncServiceImpl extends ServiceImpl<AppCallableSyncMapper, AppCallableSync> implements IAppCallableSyncService {
    @Autowired
    private IdWorker idWorker;

    @Override
    public Long saveCallableSyncData(AppCallable vo, Integer running, AppCallableSync entity) {
        if (Objects.isNull(entity)) {
            entity = new AppCallableSync();
        }
        long id = idWorker.nextId();
        entity.setId(id);
        entity.setAppCallableId(String.valueOf(vo.getId()));
        entity.setFrequency(vo.getPeriod() * 60);
        entity.setRunning(running);
        entity.setGmtCreated(DateUtils.of(new Date()));
        save(entity);
        return id;
    }

    @Override
    public void upDateCallableSyncData(Long id, Integer running, LocalDateTime startTime, LocalDateTime endTime, String result) {
        LambdaUpdateWrapper<AppCallableSync> updateWrapper = Wrappers.lambdaUpdate(AppCallableSync.class)
                .set(Objects.nonNull(running), AppCallableSync::getRunning, running)
                .set(Objects.nonNull(startTime), AppCallableSync::getStartTime, startTime)
                .set(Objects.nonNull(endTime), AppCallableSync::getEndTime, endTime)
                .set(Objects.nonNull(result), AppCallableSync::getResult, result)
                .eq(AppCallableSync::getId, id);
        update(updateWrapper);
    }

    @Override
    public void upDateCallableSyncExtendsData(Long id, String extendsData) {
        LambdaUpdateWrapper<AppCallableSync> updateWrapper = Wrappers.lambdaUpdate(AppCallableSync.class)
                .set(AppCallableSync::getExtendsData, extendsData)
                .eq(AppCallableSync::getId, id);
        update(updateWrapper);
    }
}