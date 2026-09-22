package com.tdtech.cloudcmd.linkx.dashboard.job;

import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.linkx.dashboard.service.IStaticSyncService;
import com.tdtech.cloudcmd.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 协同监测统计明细同步定时任务（合并 5 张表的同步）。
 *
 * <p>5 张统计明细表：tb_static_task / tb_static_create_group / tb_static_task_response /
 * tb_static_photo_check / tb_static_coop_duty_switch。
 *
 * <p>调度设计：
 * <ul>
 *   <li>调度频率：每分钟触发一次（{@code fixedDelay=60s}），由本类统一入口触发</li>
 *   <li>实际执行频率：从系统配置 {@code dashboard.static-sync.refresh-interval}（单位秒）读取，
 *       用 Redis 记录每张表的上次执行时间戳，距上次执行未满刷新间隔则跳过（节流）</li>
 *   <li>异步执行：5 张表的同步任务提交到 {@code linkxDashboardTaskExecutorService} 线程池，
 *       不阻塞调度线程，单表故障不影响其他表</li>
 * </ul>
 *
 * <p>Redis key 格式遵循项目规范 {@code {服务名}:{模块}:{业务标识}}，
 * 用 hash 存 5 张表的上次执行时间戳，field 为表标识。
 */
@Slf4j
@Component
public class StaticSyncJob {

    /**
     * 系统配置 key：统计同步刷新间隔，单位秒。
     */
    private static final String CONFIG_KEY_REFRESH_INTERVAL = "DASHBOARD_STATIC_REFRESH_INTERVAL";

    /**
     * 默认刷新间隔：30 分钟，系统配置缺失或解析失败时兜底。
     */
    private static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 1800;

    /**
     * Redis hash key：存各表上次执行完成的时间戳（毫秒）。
     */
    private static final String REDIS_KEY_LAST_EXECUTE_TIME = "cloudcmd:dashboard:static-sync:last-execute-time";

    @Resource
    private IStaticSyncService staticSyncService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private CachedImConfig cachedImConfig;

    @Resource
    @Qualifier("linkxDashboardTaskExecutorService")
    private ThreadPoolTaskExecutor taskExecutor;

    private List<SyncTaskDef> taskDefs;

    /**
     * 5 张表的同步任务定义延迟到注入完成后构建，避免字段初始化时机早于 {@code @Resource} 注入导致 NPE。
     */
    @javax.annotation.PostConstruct
    private void initTaskDefs() {
        taskDefs = Arrays.asList(
                new SyncTaskDef("static_task", IStaticSyncService::syncStaticTask),
                new SyncTaskDef("static_create_group", IStaticSyncService::syncStaticCreateGroup),
                new SyncTaskDef("static_task_response", IStaticSyncService::syncStaticTaskResponse),
                new SyncTaskDef("static_photo_check", IStaticSyncService::syncStaticPhotoCheck),
                new SyncTaskDef("static_coop_duty_switch", IStaticSyncService::syncStaticCoopDutySwitch)
        );
    }

    /**
     * 每分钟触发一次，按刷新间隔节流后异步执行各表同步。
     * <p>
     * {@code initialDelay=10s} 避开服务启动高峰，{@code fixedDelay=60s} 上次执行结束后等 60s 再触发。
     */
    @Scheduled(initialDelay = 10_000L, fixedDelay = 60_000L)
    public void sync() {
        log.info("[static-sync] 开始同步");
        int refreshSeconds = resolveRefreshIntervalSeconds();
        long now = System.currentTimeMillis();
        long refreshMillis = TimeUnit.SECONDS.toMillis(refreshSeconds);
        for (SyncTaskDef def : taskDefs) {
            try {
                Long lastTime = redisUtil.hGet(REDIS_KEY_LAST_EXECUTE_TIME, def.tableKey, Long.class);
                lastTime = lastTime == null ? 0L : lastTime;
                if (lastTime + refreshMillis > now) {
                    continue;
                }
                taskExecutor.execute(() -> executeSync(def, now));
            } catch (Exception e) {
                log.error("[static-sync] 提交同步任务异常, tableKey={}", def.tableKey, e);
            }
        }
        log.info("[static-sync] 同步完成");
    }

    /**
     * 执行单张表同步，完成后更新 Redis 时间戳。
     * 在工作线程中运行，异常不影响其他表。
     */
    private void executeSync(SyncTaskDef def, long startTime) {
        try {
            log.info("[static-sync] 开始同步, tableKey={}", def.tableKey);
            def.action.accept(staticSyncService);
            redisUtil.hSet(REDIS_KEY_LAST_EXECUTE_TIME, def.tableKey, startTime);
            log.info("[static-sync] 同步完成, tableKey={}, cost={}ms",
                    def.tableKey, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[static-sync] 同步失败, tableKey={}, cost={}ms",
                    def.tableKey, System.currentTimeMillis() - startTime, e);
        }
    }

    /**
     * 从系统配置取刷新间隔（秒），解析失败或未配置用默认值。
     */
    private int resolveRefreshIntervalSeconds() {
        try {
            String value = cachedImConfig.getSysConfig(CONFIG_KEY_REFRESH_INTERVAL);
            if (StringUtils.isBlank(value)) {
                return DEFAULT_REFRESH_INTERVAL_SECONDS;
            }
            int seconds = Integer.parseInt(value.trim());
            return seconds > 0 ? seconds : DEFAULT_REFRESH_INTERVAL_SECONDS;
        } catch (Exception e) {
            log.warn("[static-sync] 系统配置 {} 读取或解析失败, 使用默认值 {}s",
                CONFIG_KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL_SECONDS, e);
            return DEFAULT_REFRESH_INTERVAL_SECONDS;
        }
    }

    /**
     * 同步任务定义：表标识 + 执行动作。
     */
    private static class SyncTaskDef {
        final String tableKey;
        final Consumer<IStaticSyncService> action;

        SyncTaskDef(String tableKey, Consumer<IStaticSyncService> action) {
            this.tableKey = tableKey;
            this.action = action;
        }
    }
}