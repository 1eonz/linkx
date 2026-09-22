package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.IcpImUserVO;
import com.tdtech.cloudcmd.icp.proxy.entity.IcpImUser;
import com.tdtech.cloudcmd.icp.proxy.repo.IcpImUserMapper;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImToken;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImUserService {

    private static final String REDIS_LOCK_KEY_IM_USER_REFRESH = "cloudcmd:icp:imuser:refresh-lock";

    private final ImHttpClient imHttpClient;
    private final IcpImUserMapper icpImUserMapper;
    private final IcpPrivService icpPrivService;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;
    private final RedisLockFactory redisLockFactory;

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledTask() {
        refreshImUser();
    }

    public void refreshImUser() {
        var redisLock = redisLockFactory.newRedisLock(REDIS_LOCK_KEY_IM_USER_REFRESH, Duration.ofMinutes(3L));
        if (!redisLock.tryLock(0L, TimeUnit.MILLISECONDS)) {
            log.warn("lock busy");
            return;
        }
        var lockFlag = new AtomicBoolean(true);
        try {
            asyncMessageTaskExecutor.execute(() -> {
                while (lockFlag.get()) {
                    redisLock.delayTimeOut(Duration.ofMinutes(3L));
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        log.warn("interrupted", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            doRefreshImUser();
        } finally {
            lockFlag.set(false);
            redisLock.unlock();
        }
    }

    private void doRefreshImUser() {
        log.info("定时任务开始执行：同步 IM 用户数据");
        icpImUserMapper.update(Wrappers.lambdaUpdate(IcpImUser.class).set(IcpImUser::getRefreshFlag, 1));
        var stat = syncImUserPage(null);
        icpImUserMapper.delete(Wrappers.lambdaQuery(IcpImUser.class).eq(IcpImUser::getRefreshFlag, 1));
        icpPrivService.clear();
        icpPrivService.setDefault();
        log.info("定时任务执行完成：共处理 {} 条记录，新增 {} 条，更新 {} 条", stat.totalProcessed, stat.totalCreated, stat.totalUpdated);
    }

    private SyncStat syncImUserPage(Long queryTag) {
        var stat = new SyncStat();
        for (int pageNum = 1, pageSize = 50; ; pageNum++) {
            var depCode = Optional.ofNullable(imHttpClient.getToken()).map(ImToken::getDepartment)
                    .map(ImToken.Department::getDepartmentCode).orElse(null);
            var imUserImPage = imHttpClient.userPageByDepartment(pageNum, pageSize, depCode, 1, null, null, null, queryTag);
            if (imUserImPage == null) {
                log.info("第 {} 页返回空响应，结束分页查询", pageNum);
                break;
            }
            var records = imUserImPage.getRecords();
            if (records == null || records.isEmpty()) {
                log.info("第 {} 页无数据，结束分页查询", pageNum);
                break;
            }

            log.info("第 {} 页获取到 {} 条记录", pageNum, records.size());
            stat.totalProcessed += records.size();
            upsertImUsers(pageNum, records, stat);
            // ✅ 关键修复：检查是否已到达最后一页
            if (imUserImPage.getTotal() <= pageNum * pageSize) {
                log.info("已到达最后一页，total={}, 当前页={}, 结束分页查询", imUserImPage.getTotal(), pageNum);
                break;
            }
        }
        return stat;
    }

    public Collection<String> ensureUserPriv(UserInfo user) {
        if (user == null || user.getUserId() == null) {
            log.info("Skip ICP default privilege ensure because current user is null or missing userId");
            return Collections.emptyList();
        }
        var privs = icpPrivService.getUserPrivByUser(user.getUserId());
        if (privs != null && !privs.isEmpty()) {
            log.info("ICP default privilege ensure skipped, user {} already has {} department privileges",
                    user.getUserId(), privs.size());
            return privs;
        }
        log.info("ICP default privilege ensure started for user {}", user.getUserId());
        syncImUserById(user.getUserId());
        icpPrivService.setDefault(user.getUserId());
        var ensuredPrivs = icpPrivService.getUserPrivByUser(user.getUserId());
        log.info("ICP default privilege ensure finished for user {}, privilege count: {}", user.getUserId(),
                ensuredPrivs == null ? 0 : ensuredPrivs.size());
        return ensuredPrivs;
    }

    public void syncImUserByIsdn(String isdn) {
        if (isdn == null || isdn.isBlank()) {
            log.info("Skip IM user on-demand sync because isdn is blank");
            return;
        }
        var localUser = icpImUserMapper.selectList(Wrappers.lambdaQuery(IcpImUser.class).eq(IcpImUser::getIsdn, isdn))
                .stream()
                .findFirst()
                .orElse(null);
        if (localUser != null) {
            log.info("IM user already exists locally by isdn {}, userId: {}", isdn, localUser.getId());
            return;
        }
        var depCode = Optional.ofNullable(imHttpClient.getToken()).map(ImToken::getDepartment)
                .map(ImToken.Department::getDepartmentCode).orElse(null);
        var remoteUser = Optional.ofNullable(imHttpClient.userPageByDepartment(1, 20, depCode, 1, isdn, null, null,
                        null))
                .map(a -> a.getRecords())
                .orElse(Collections.emptyList())
                .stream()
                .filter(a -> isdn.equals(a.getIsdn()))
                .findFirst()
                .orElse(null);
        if (remoteUser == null) {
            log.info("IM user not found by isdn {}, skip on-demand sync", isdn);
            return;
        }
        var stat = new SyncStat();
        upsertImUsers(1, List.of(remoteUser), stat);
        log.info("IM user on-demand sync by isdn {} done, userId: {}, created: {}, updated: {}", isdn,
                remoteUser.getId(), stat.totalCreated, stat.totalUpdated);
    }

    private void syncImUserById(Long userId) {
        var localUser = icpImUserMapper.selectById(userId);
        if (localUser != null) {
            log.info("IM user {} already exists locally, skip on-demand sync, isdn: {}", userId,
                    localUser.getIsdn());
            return;
        }
        log.info("IM user {} not found locally, querying IM by userId", userId);
        var remoteUser = Optional.ofNullable(imHttpClient.userPageById(String.valueOf(userId)))
                .map(a -> a.getResults())
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .orElse(null);
        if (remoteUser == null) {
            log.info("IM user {} not found, skip on-demand sync", userId);
            return;
        }
        log.info("IM user {} found from IM, isdn: {}, name: {}", userId, remoteUser.getIsdn(), remoteUser.getName());
        var stat = new SyncStat();
        upsertImUsers(1, List.of(remoteUser), stat);
        log.info("IM user {} on-demand sync done, created: {}, updated: {}", userId, stat.totalCreated,
                stat.totalUpdated);
    }

    private void upsertImUsers(int pageNum, List<ImUser> records, SyncStat stat) {
        var exists = icpImUserMapper.selectInX(IcpImUser::getId,
                records.stream().map(ImUser::getId).collect(Collectors.toList()));
        var existIds = Optional.ofNullable(exists).stream().flatMap(Collection::stream).map(IcpImUser::getId)
                .collect(Collectors.toList());

        // 新增数据
        var create = records.stream().filter(a -> !existIds.contains(a.getId())).collect(Collectors.toList());
        if (!create.isEmpty()) {
            var icpImUserList = BeanCopyUtils.copyList(create, IcpImUser::new);
            for (var icpImUser : icpImUserList) {
                icpImUser.setRefreshFlag(0);
            }
            icpImUserMapper.insertBatch(icpImUserList);
            log.info("第 {} 页新增 {} 条记录", pageNum, create.size());
            stat.totalCreated += create.size();
        }

        // 更新数据
        var update = records.stream().filter(a -> existIds.contains(a.getId())).collect(Collectors.toList());
        update.forEach(a -> {
            var entity = BeanCopyUtils.copyBean(a, IcpImUser::new);
            entity.setRefreshFlag(0);
            icpImUserMapper.updateById(entity);
        });
        if (!update.isEmpty()) {
            log.info("第 {} 页更新 {} 条记录", pageNum, update.size());
            stat.totalUpdated += update.size();
        }
    }

    public CcmdPage<IcpImUserVO> selectPage(CcmdPageParam pageParam, MPJLambdaWrapper<IcpImUser> wrapper) {
        return icpImUserMapper.selectJoinPage(pageParam, IcpImUserVO.class, wrapper);
    }

    private static class SyncStat {
        private int totalProcessed;
        private int totalCreated;
        private int totalUpdated;
    }
}
