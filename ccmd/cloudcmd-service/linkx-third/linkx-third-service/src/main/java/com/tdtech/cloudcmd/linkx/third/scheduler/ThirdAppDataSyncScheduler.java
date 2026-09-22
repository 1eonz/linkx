package com.tdtech.cloudcmd.linkx.third.scheduler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.encryptor.service.CachedConfig;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppDataToDbDto;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallableSync;
import com.tdtech.cloudcmd.linkx.third.enums.AppTypeEnum;
import com.tdtech.cloudcmd.linkx.third.enums.Constants;
import com.tdtech.cloudcmd.linkx.third.enums.SyncRunningEnum;
import com.tdtech.cloudcmd.linkx.third.service.IAppCallableService;
import com.tdtech.cloudcmd.linkx.third.service.IAppCallableSyncService;
import com.tdtech.cloudcmd.linkx.third.service.impl.ApiAppCallableDataSyncServiceImpl;
import com.tdtech.cloudcmd.linkx.third.utils.DateFormatFieldUtils;
import com.tdtech.cloudcmd.linkx.third.utils.DateTimeDimensionUtils;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import com.tdtech.cloudcmd.linkx.third.vo.DateFormatFieldInfo;
import com.tdtech.cloudcmd.linkx.third.vo.DateFormatFieldInfo.DateDimension;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 南向应用数据同步调度器
 * <p>
 * 拆分为三个独立调度方法：
 * <ul>
 *     <li>forwardScheduled: API类型 - forward执行器，向未来滚动拉取数据</li>
 *     <li>backwardScheduled: API类型 - backward执行器，向历史回溯拉取数据</li>
 *     <li>dbScheduled: DB类型 - 使用原有执行逻辑</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
public class ThirdAppDataSyncScheduler {
    private static final String REDIS_KEY = "cloudcmd:linkx:third:AppCallable:execute-time";
    private static final String FORWARD_LOCK = "cloudcmd:linkx:third:AppCallable:forward:lock";
    private static final String BACKWARD_LOCK = "cloudcmd:linkx:third:AppCallable:backward:lock";
    private static final String DB_LOCK = "cloudcmd:linkx:third:AppCallable:lock";

    /**
     * forward 执行器类型
     */
    private static final int EXECUTOR_TYPE_FORWARD = 1;
    /**
     * backward 执行器类型
     */
    private static final int EXECUTOR_TYPE_BACKWARD = 2;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private IAppCallableService iAppCallableService;

    @Resource
    private RedisLockFactory redisLockFactory;

    @Autowired
    private IAppCallableSyncService iAppCallableSyncService;

    @Resource
    @Qualifier("linkxThirdTaskExecutorService")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private ApiAppCallableDataSyncServiceImpl apiSyncService;

    @Autowired
    private CachedConfig cachedConfig;

    /**
     * Forward 执行器调度：API类型应用，向未来滚动拉取数据
     * <p>
     * 调度频率：每60秒执行一次
     * </p>
     */
    @Scheduled(initialDelay = 10200L, fixedDelay = 60L * 1000L)
    public void forwardScheduled() {
        log.info("[forward] 开始执行forward调度任务");
        List<AppCallable> appCallables = iAppCallableService.list(Wrappers.lambdaQuery(AppCallable.class)
                .eq(AppCallable::getIsDeleted, Constants.VALID)
                .eq(AppCallable::getType, AppTypeEnum.API.getCode()));
        if (CollectionUtils.isEmpty(appCallables)) {
            log.info("[forward] 没有配置forward执行的应用");
            return;
        }
        RedisLockFactory.RedisLock lock = redisLockFactory.newRedisLock(FORWARD_LOCK, Duration.ofMinutes(60L));
        if (!lock.tryLock(10L, TimeUnit.SECONDS)) {
            log.warn("[forward] 有正在执行的forward调度任务，等待下次调度");
            return;
        }
        try {
            long now = System.currentTimeMillis();
            for (AppCallable vo : appCallables) {
                taskExecutor.execute(() -> {
                    Long lastTime = redisUtil.hGet(REDIS_KEY, vo.getId() + ":forward", Long.class);
                    lastTime = lastTime == null ? 0L : lastTime;
                    if (TimeUnit.MINUTES.toMillis(vo.getPeriod()) + lastTime < now) {
                        forwardExecute(vo);
                        redisUtil.hSet(REDIS_KEY, vo.getId() + ":forward", now);
                    }
                });
            }
        } catch (Exception e) {
            log.error("[forward] 调度器执行异常", e);
        } finally {
            lock.unlock();
        }
        log.info("[forward] forward调度任务执行完成");
    }

    /**
     * Backward 执行器调度：API类型应用，向历史回溯拉取数据
     * <p>
     * 调度频率：每300秒（5分钟）执行一次
     * </p>
     */
    @Scheduled(initialDelay = 12000L, fixedDelay = 300L * 1000L)
    public void backwardScheduled() {
        log.info("[backward] 开始执行backward调度任务");
        List<AppCallable> appCallables = iAppCallableService.list(Wrappers.lambdaQuery(AppCallable.class)
                .eq(AppCallable::getIsDeleted, Constants.VALID)
                .eq(AppCallable::getType, AppTypeEnum.API.getCode()));
        if (CollectionUtils.isEmpty(appCallables)) {
            log.info("[backward] 没有配置backward执行的应用");
            return;
        }
        RedisLockFactory.RedisLock lock = redisLockFactory.newRedisLock(BACKWARD_LOCK, Duration.ofMinutes(60L));
        if (!lock.tryLock(10L, TimeUnit.SECONDS)) {
            log.warn("[backward] 有正在执行的backward调度任务，等待下次调度");
            return;
        }
        try {
            for (AppCallable vo : appCallables) {
                taskExecutor.execute(() -> backwardExecute(vo));
            }
        } catch (Exception e) {
            log.error("[backward] 调度器执行异常", e);
        } finally {
            lock.unlock();
        }
        log.info("[backward] backward调度任务执行完成");
    }

    /**
     * DB 类型调度：保持原有执行逻辑
     * <p>
     * 调度频率：每60秒执行一次
     * </p>
     */
    @Scheduled(initialDelay = 17000L, fixedDelay = 60L * 1000L)
    public void dbScheduled() {
        log.info("[db] 开始执行db调度任务");
        List<AppCallable> appCallables = iAppCallableService.list(Wrappers.lambdaQuery(AppCallable.class)
                .eq(AppCallable::getIsDeleted, Constants.VALID)
                .eq(AppCallable::getType, AppTypeEnum.DB.getCode()));
        if (CollectionUtils.isEmpty(appCallables)) {
            log.info("[db] 没有配置db执行的应用");
            return;
        }
        RedisLockFactory.RedisLock lock = redisLockFactory.newRedisLock(DB_LOCK, Duration.ofMinutes(60L));
        if (!lock.tryLock(10L, TimeUnit.SECONDS)) {
            log.warn("[db] 有正在执行的db调度任务，等待下次调度");
            return;
        }
        try {
            long now = System.currentTimeMillis();
            for (AppCallable vo : appCallables) {
                taskExecutor.execute(() -> {
                    Long lastTime = redisUtil.hGet(REDIS_KEY, vo.getId() + "", Long.class);
                    lastTime = lastTime == null ? 0L : lastTime;
                    if (TimeUnit.MINUTES.toMillis(vo.getPeriod()) + lastTime < now) {
                        execute(vo);
                        redisUtil.hSet(REDIS_KEY, vo.getId() + "", now);
                    }
                });
            }
        } catch (Exception e) {
            log.error("[db] 调度器执行异常", e);
        } finally {
            lock.unlock();
        }
        log.info("[db] db调度任务执行完成");
    }

    /**
     * DB 类型原有执行逻辑
     */
    public void execute(AppCallable vo) {
        int runningStatus = SyncRunningEnum.SUCCESS.getCode();
        String result = "执行成功";
        Long id = iAppCallableSyncService.saveCallableSyncData(vo, SyncRunningEnum.NOT_STARTED.getCode());
        try {
            iAppCallableSyncService.upDateCallableSyncData(id, SyncRunningEnum.RUNNING.getCode(), DateUtils.of(new Date()), null, null);
            iAppCallableService.runSync(vo.getId());
        } catch (Exception e) {
            log.error("[db] 南向应用事务定时器执行异常, 应用id:{}", vo.getId(), e);
            runningStatus = SyncRunningEnum.ERROR.getCode();
            result = StringUtils.truncate(e.getMessage(), 200, StringUtils.CHAR_LEN);
        } finally {
            iAppCallableSyncService.upDateCallableSyncData(id, runningStatus, null, DateUtils.of(new Date()), result);
        }
    }

    /**
     * Forward 执行器：向未来滚动拉取数据
     * <p>
     * 逻辑说明：
     * <ul>
     *     <li>无执行记录（首次执行）：timeRangeStart = now - 1个时间单元，timeRangeEnd = now</li>
     *     <li>有成功记录：timeRangeStart = extendsData.latestDateTime，timeRangeEnd = now</li>
     *     <li>有失败记录：timeRangeStart = extendsData.timeRangeStart，timeRangeEnd = now</li>
     *     <li>成功时：取 maxDateTimeSign 作为 latestDateTime 写入 extendsData</li>
     * </ul>
     * </p>
     *
     * @param vo 南向应用配置
     */
    public void forwardExecute(AppCallable vo) {
        log.info("[forward] 开始执行, 应用id={}, 应用name={}", vo.getId(), vo.getName());
        AppCallableDetailVo detailVo = iAppCallableService.getAppCallable(vo.getId());
        if (Objects.isNull(detailVo)) {
            log.warn("[forward] 南向应用不存在, 应用id={}", vo.getId());
            return;
        }

        // 计算时间范围
        DateDimension dimension = resolveDimensionFromApp(detailVo);
        LocalDateTime timeRangeStart;
        LocalDateTime timeRangeEnd;
        if (dimension == null) {
            log.warn("[forward] 南向应用无正确的时间表达式配置, 应用id={}", vo.getId());
            timeRangeStart = null;
            timeRangeEnd = null;
        } else {
            timeRangeStart = calculateForwardTimeRange(vo, dimension);
            timeRangeEnd = LocalDateTime.now();
        }

        // 执行同步并记录
        AppCallableDetailVo appDetail = iAppCallableService.getAppCallable(vo.getId());
        executeForwardSync(vo, appDetail, timeRangeStart, timeRangeEnd);
    }

    private LocalDateTime calculateForwardTimeRange(AppCallable vo, DateDimension dimension) {
        LocalDateTime now = LocalDateTime.now();

        AppCallableSync lastSync = iAppCallableSyncService.getOne(
                Wrappers.lambdaQuery(AppCallableSync.class)
                        .eq(AppCallableSync::getAppCallableId, String.valueOf(vo.getId()))
                        .eq(AppCallableSync::getExecutorType, EXECUTOR_TYPE_FORWARD)
                        .orderByDesc(AppCallableSync::getId)
                        .last("limit 1")
        );

        if (Objects.isNull(lastSync)) {
            LocalDateTime timeRangeStart = DateTimeDimensionUtils.minus(now, dimension, 0);
            log.info("[forward] 首次执行, 应用id={}, timeRangeStart={}, timeRangeEnd={}", vo.getId(), timeRangeStart, now);
            return timeRangeStart;
        } else if (SyncRunningEnum.SUCCESS.getCode().equals(lastSync.getRunning())) {
            LocalDateTime timeRangeStart = parseDateTimeFromExtendsData(lastSync.getExtendsData(), "latestDateTime");
            if (Objects.isNull(timeRangeStart)) {
                log.warn("[forward] 上次成功记录但 extendsData 无 latestDateTime, 应用id={}, 使用默认值", vo.getId());
                timeRangeStart = parseDateTimeFromExtendsData(lastSync.getExtendsData(), "timeRangeEnd");
            }
            log.info("[forward] 上次成功, 应用id={}, latestDateTime={}, timeRangeEnd={}", vo.getId(), timeRangeStart, now);
            return timeRangeStart;
        } else {
            LocalDateTime timeRangeStart = parseDateTimeFromExtendsData(lastSync.getExtendsData(), "timeRangeStart");
            if (Objects.isNull(timeRangeStart)) {
                log.warn("[forward] 上次失败记录但 extendsData 无 timeRangeStart, 应用id={}, 使用默认值", vo.getId());
                timeRangeStart = DateTimeDimensionUtils.minus(now, dimension, 1);
            }
            log.info("[forward] 上次失败, 应用id={}, timeRangeStart={}, timeRangeEnd={}", vo.getId(), timeRangeStart, now);
            return timeRangeStart;
        }
    }

    private void executeForwardSync(AppCallable vo, AppCallableDetailVo detailVo,
                                     LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) {
        AppCallableSync syncEntity = new AppCallableSync();
        syncEntity.setExecutorType(EXECUTOR_TYPE_FORWARD);
        Long syncId = iAppCallableSyncService.saveCallableSyncData(vo, SyncRunningEnum.NOT_STARTED.getCode(), syncEntity);
        log.info("[forward] 创建 sync 记录, 应用id={}, syncId={}", vo.getId(), syncId);

        int runningStatus = SyncRunningEnum.SUCCESS.getCode();
        String result = "执行成功";
        try {
            iAppCallableSyncService.upDateCallableSyncData(syncId, SyncRunningEnum.RUNNING.getCode(), DateUtils.of(new Date()), null, null);
            AppDataToDbDto data = apiSyncService.doSync(detailVo, timeRangeStart, timeRangeEnd);
            log.info("[forward] 数据同步完成, 应用id={}, 数据条数={}", vo.getId(),
                    CollectionUtils.isEmpty(data.getResultDataList()) ? 0 : data.getResultDataList().size());

            // 无数据时用 timeRangeEnd 作为 latestDateTime，确保下一轮从当前终点继续向前滚动
            String latestDateTime = data.getMaxDateTimeSign();
            if (StringUtils.isBlank(latestDateTime)) {
                latestDateTime = timeRangeEnd.toString();
                log.info("[forward] 本轮无数据, 使用timeRangeEnd作为latestDateTime, 应用id={}, latestDateTime={}",
                        vo.getId(), latestDateTime);
            }
            JSONObject extendsData = new JSONObject();
            extendsData.put("latestDateTime", latestDateTime);
            extendsData.put("timeRangeStart", timeRangeStart == null ? null :timeRangeStart.toString());
            extendsData.put("timeRangeEnd", timeRangeEnd == null ? null :  timeRangeEnd.toString());
            iAppCallableSyncService.upDateCallableSyncExtendsData(syncId, extendsData.toJSONString());
            log.info("[forward] 写入 extendsData, 应用id={}, latestDateTime={}", vo.getId(), latestDateTime);
        } catch (Exception e) {
            log.error("[forward] 执行异常, 应用id={}", vo.getId(), e);
            runningStatus = SyncRunningEnum.ERROR.getCode();
            result = StringUtils.truncate(e.getMessage(), 200, StringUtils.CHAR_LEN);
            JSONObject extendsData = new JSONObject();
            extendsData.put("timeRangeStart", timeRangeStart == null ? null :timeRangeStart.toString());
            extendsData.put("timeRangeEnd", timeRangeEnd == null ? null : timeRangeEnd.toString());
            iAppCallableSyncService.upDateCallableSyncExtendsData(syncId, extendsData.toJSONString());
        } finally {
            iAppCallableSyncService.upDateCallableSyncData(syncId, runningStatus, null, DateUtils.of(new Date()), result);
        }
    }

    /**
     * Backward 执行器：向历史回溯拉取数据
     * <p>
     * 逻辑说明：
     * <ul>
     *     <li>无执行记录（首次执行）：timeRangeStart = now - N个时间单元，timeRangeEnd = now</li>
     *     <li>有成功记录：timeRangeEnd = extendsData.earliestDateTime，timeRangeStart = timeRangeEnd - N个时间单元</li>
     *     <li>有失败记录：timeRangeStart = extendsData.timeRangeStart，timeRangeEnd = extendsData.timeRangeEnd（维持原值）</li>
     *     <li>终止判断：timeRangeStart <= dataStartTime 时跳过不执行</li>
     *     <li>成功时：取 minDateTimeSign 作为 earliestDateTime 写入 extendsData</li>
     * </ul>
     * </p>
     *
     * @param vo 南向应用配置
     */
    public void backwardExecute(AppCallable vo) {
        log.info("[backward] 开始执行, 应用id={}, 应用name={}", vo.getId(), vo.getName());
        AppCallableDetailVo detailVo = iAppCallableService.getAppCallable(vo.getId());
        if (Objects.isNull(detailVo)) {
            log.warn("[backward] 南向应用不存在, 应用id={}", vo.getId());
            return;
        }

        // 计算时间范围
        DateDimension dimension = resolveDimensionFromApp(detailVo);
        if (dimension == null) {
            log.info("应用id={}, 无正确的时间表达式配置, 跳过执行", detailVo.getId());
            return;
        }
        int step = DateTimeDimensionUtils.getBackwardStep(cachedConfig, dimension);
        LocalDateTime[] timeRange = calculateBackwardTimeRange(vo, dimension, step);
        LocalDateTime timeRangeStart = timeRange[0];
        LocalDateTime timeRangeEnd = timeRange[1];

        // dataStartTime 边界校验
        if (checkBackwardTermination(vo, detailVo, timeRangeStart, timeRangeEnd)) {
            log.info("[backward] 已拉取到最早时间, 应用id={}, timeRangeStart={}, timeRangeEnd={}",
                    vo.getId(), timeRangeStart, timeRangeEnd);
            return;
        }
        timeRangeStart = adjustBackwardStartTime(vo, detailVo, timeRangeStart);

        // 执行同步并记录
        AppCallableDetailVo appDetail = iAppCallableService.getAppCallable(vo.getId());
        executeBackwardSync(vo, appDetail, timeRangeStart, timeRangeEnd);
    }

    private LocalDateTime[] calculateBackwardTimeRange(AppCallable vo, DateDimension dimension, int step) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeRangeStart;
        LocalDateTime timeRangeEnd;

        AppCallableSync lastSync = iAppCallableSyncService.getOne(
                Wrappers.lambdaQuery(AppCallableSync.class)
                        .eq(AppCallableSync::getAppCallableId, String.valueOf(vo.getId()))
                        .eq(AppCallableSync::getExecutorType, EXECUTOR_TYPE_BACKWARD)
                        .orderByDesc(AppCallableSync::getId)
                        .last("limit 1")
        );

        if (Objects.isNull(lastSync)) {
            // 首次执行：timeRangeEnd = now - 1个时间单元，与 forward 首次范围错开不重叠
            timeRangeEnd = DateTimeDimensionUtils.minus(now, dimension, 1);
            timeRangeStart = DateTimeDimensionUtils.minus(timeRangeEnd, dimension, step);
            log.info("[backward] 首次执行, 应用id={}, step={}, timeRangeStart={}, timeRangeEnd={}", vo.getId(), step, timeRangeStart, timeRangeEnd);
        } else if (SyncRunningEnum.SUCCESS.getCode().equals(lastSync.getRunning())) {
            // 历史数据的拉取，时间范围不重叠
            timeRangeEnd = DateTimeDimensionUtils.minus(parseDateTimeFromExtendsData(lastSync.getExtendsData(), "earliestDateTime"), dimension, 1);
            if (Objects.isNull(timeRangeEnd)) {
                log.warn("[backward] 上次成功记录但 extendsData 无 earliestDateTime, 应用id={}, 复用上传的值", vo.getId());
                timeRangeStart = parseDateTimeFromExtendsData(lastSync.getExtendsData(), "timeRangeStart");
                timeRangeEnd = parseDateTimeFromExtendsData(lastSync.getExtendsData(), "timeRangeEnd");
            } else {
                timeRangeStart = DateTimeDimensionUtils.minus(timeRangeEnd, dimension, step);
            }
            log.info("[backward] 上次成功, 应用id={}, earliestDateTime={}, step={}, timeRangeStart={}", vo.getId(), timeRangeEnd, step, timeRangeStart);
        } else {
            timeRangeStart = parseDateTimeFromExtendsData(lastSync.getExtendsData(), "timeRangeStart");
            timeRangeEnd = parseDateTimeFromExtendsData(lastSync.getExtendsData(), "timeRangeEnd");
            if (Objects.isNull(timeRangeStart) || Objects.isNull(timeRangeEnd)) {
                log.warn("[backward] 上次失败记录但 extendsData 时间范围不完整, 应用id={}, 使用默认值", vo.getId());
                timeRangeStart = DateTimeDimensionUtils.minus(now, dimension, step);
                timeRangeEnd = DateTimeDimensionUtils.minus(now, dimension, 1);
            }
            log.info("[backward] 上次失败, 应用id={}, timeRangeStart={}, timeRangeEnd={}", vo.getId(), timeRangeStart, timeRangeEnd);
        }
        return new LocalDateTime[]{timeRangeStart, timeRangeEnd};
    }

    private boolean checkBackwardTermination(AppCallable vo, AppCallableDetailVo detailVo,
                                              LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) {
        LocalDateTime dataStartTime = detailVo.getDataStartTime();
        if (Objects.nonNull(dataStartTime) && timeRangeEnd.isBefore(dataStartTime)) {
            log.info("[backward] 已到达数据起始时间, 停止回溯, 应用id={}, timeRangeStart={}, dataStartTime={}",
                    vo.getId(), timeRangeStart, dataStartTime);
            return true;
        }
        return false;
    }

    private LocalDateTime adjustBackwardStartTime(AppCallable vo, AppCallableDetailVo detailVo, LocalDateTime timeRangeStart) {
        LocalDateTime dataStartTime = detailVo.getDataStartTime();
        if (Objects.nonNull(dataStartTime) && timeRangeStart.isBefore(dataStartTime)) {
            log.info("[backward] timeRangeStart早于dataStartTime, 重置timeRangeStart, 应用id={}, 原始={}, 重置为={}",
                    vo.getId(), timeRangeStart, dataStartTime);
            return dataStartTime;
        }
        return timeRangeStart;
    }

    private void executeBackwardSync(AppCallable vo, AppCallableDetailVo detailVo,
                                      LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) {
        AppCallableSync syncEntity = new AppCallableSync();
        syncEntity.setExecutorType(EXECUTOR_TYPE_BACKWARD);
        Long syncId = iAppCallableSyncService.saveCallableSyncData(vo, SyncRunningEnum.NOT_STARTED.getCode(), syncEntity);
        log.info("[backward] 创建 sync 记录, 应用id={}, syncId={}", vo.getId(), syncId);

        int runningStatus = SyncRunningEnum.SUCCESS.getCode();
        String result = "执行成功";
        try {
            iAppCallableSyncService.upDateCallableSyncData(syncId, SyncRunningEnum.RUNNING.getCode(), DateUtils.of(new Date()), null, null);
            AppDataToDbDto data = apiSyncService.doSync(detailVo, timeRangeStart, timeRangeEnd);
            log.info("[backward] 数据同步完成, 应用id={}, 数据条数={}", vo.getId(),
                    CollectionUtils.isEmpty(data.getResultDataList()) ? 0 : data.getResultDataList().size());

            // 无数据时用 timeRangeStart 作为 earliestDateTime，确保下一轮继续往前回溯
            String earliestDateTime = data.getMinDateTimeSign();
            if (StringUtils.isBlank(earliestDateTime)) {
                earliestDateTime = timeRangeStart.toString();
                log.info("[backward] 本轮无数据, 使用timeRangeStart作为earliestDateTime, 应用id={}, earliestDateTime={}",
                        vo.getId(), earliestDateTime);
            }
            JSONObject extendsData = new JSONObject();
            extendsData.put("earliestDateTime", earliestDateTime);
            extendsData.put("timeRangeStart", timeRangeStart.toString());
            extendsData.put("timeRangeEnd", timeRangeEnd.toString());
            iAppCallableSyncService.upDateCallableSyncExtendsData(syncId, extendsData.toJSONString());
            log.info("[backward] 写入 extendsData, 应用id={}, earliestDateTime={}", vo.getId(), earliestDateTime);
        } catch (Exception e) {
            log.error("[backward] 执行异常, 应用id={}", vo.getId(), e);
            runningStatus = SyncRunningEnum.ERROR.getCode();
            result = StringUtils.truncate(e.getMessage(), 200, StringUtils.CHAR_LEN);
            JSONObject extendsData = new JSONObject();
            extendsData.put("timeRangeStart", timeRangeStart.toString());
            extendsData.put("timeRangeEnd", timeRangeEnd.toString());
            iAppCallableSyncService.upDateCallableSyncExtendsData(syncId, extendsData.toJSONString());
        } finally {
            iAppCallableSyncService.upDateCallableSyncData(syncId, runningStatus, null, DateUtils.of(new Date()), result);
        }
    }

    /**
     * 从南向应用配置中解析时间维度
     * <p>
     * 优先从 reqParam 提取日期格式字段，取第一个字段的维度作为调度维度。
     * 无法解析时默认为 DAY 维度。
     * </p>
     *
     * @param detailVo 南向应用详情
     * @return 时间维度
     */
    private DateDimension resolveDimensionFromApp(AppCallableDetailVo detailVo) {
        // 优先从 reqParam 中提取日期字段，没有再从 reqBody 中提取
        List<DateFormatFieldInfo> dateFields = DateFormatFieldUtils.extractDateFormatFields(detailVo.getReqParam());
        boolean fromParam =  CollectionUtils.isNotEmpty(dateFields) && dateFields.size() >=2;
        if (!fromParam) {
            dateFields = DateFormatFieldUtils.extractDateFormatFields(detailVo.getReqBody());
        }
        if (CollectionUtils.isEmpty(dateFields) || dateFields.size() < 2) {
            log.info("应用id={}, 从 reqParam 中解析时间维度失败, 无正确的时间表达式配置", detailVo.getId());
            return null;
        }
        DateDimension dimension = dateFields.get(0).getDimension();
        log.info("解析到时间维度: {}, 应用id={}", dimension, detailVo.getId());
        return dimension;
    }

    /**
     * 从 extendsData JSON 中解析指定时间字段
     *
     * @param extendsDataStr extendsData JSON 字符串
     * @param key            时间字段名，如 "latestDateTime"、"earliestDateTime"、"timeRangeStart"
     * @return 解析后的 LocalDateTime，解析失败返回 null
     */
    private LocalDateTime parseDateTimeFromExtendsData(String extendsDataStr, String key) {
        if (StringUtils.isBlank(extendsDataStr)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(extendsDataStr);
            String value = json.getString(key);
            if (StringUtils.isNotBlank(value)) {
                return com.tdtech.cloudcmd.linkx.third.utils.DateUtils.parseLdt(value);
            }
        } catch (Exception e) {
            log.warn("解析 extendsData 失败, key={}, extendsData={}", key, extendsDataStr, e);
        }
        return null;
    }

}