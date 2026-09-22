package com.tdtech.cloudcmd.linkx.third.service;

import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallableSync;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * <p>
 * 南向应用数据同步信息表 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
public interface IAppCallableSyncService extends IService<AppCallableSync> {

    Long saveCallableSyncData(AppCallable vo, Integer running, AppCallableSync entity);

    default Long saveCallableSyncData(AppCallable vo, Integer running) {
        return saveCallableSyncData(vo, running, null);
    }

    void upDateCallableSyncData(Long id, Integer running, LocalDateTime startTime, LocalDateTime endTime, String result);

    /**
     * 更新 sync 记录的 extendsData 字段
     *
     * @param id          sync 记录 ID
     * @param extendsData extendsData JSON 字符串
     */
    void upDateCallableSyncExtendsData(Long id, String extendsData);
}