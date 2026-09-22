package com.tdtech.cloudcmd.cagent.service.distributed;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.conf.SystemProperties;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LeaderStatusMachine implements ApplicationRunner {

    private static final String LEADER_LOCK_KEY = "cloudcmd:cagent:leader-lock";

    @Resource
    private SystemProperties systemProperties;
    @Resource
    private RedisLockFactory lockFactory;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private LeaderStatusObserver leaderStatusObserver;

    private RedisLockFactory.RedisLock lock;
    private volatile LeaderStatus currentLeaderStatus = LeaderStatus.INIT;
    private String currentLeader;
    private boolean isRunning = false;
    private Thread checkLoopThread;

    @PreDestroy
    public void preDestroy() {
        if (lock != null) {
            lock.unlock();
        }
        isRunning = false;
        checkLoopThread.interrupt();
    }

    /**
     * init delay to service ready
     */
    @Override
    public void run(ApplicationArguments args) {
        if (isRunning) {
            log.error("init already done");
            return;
        }
        log.info("on leader predication start");
        lock = lockFactory.newRedisLock(LEADER_LOCK_KEY, Duration.ofSeconds(10L), systemProperties.getIpPort());
        try {
            onInit();
        } finally {
            isRunning = true;
        }
        checkLoopThread = new Thread(() -> {
            while (isRunning) {
                try {
                    this.leaderCheck();
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    log.info("interrupted", e);
                } catch (Throwable e) {
                    log.error("error", e);
                }
            }
        });
        checkLoopThread.setName("leader-check-loop");
        checkLoopThread.setUncaughtExceptionHandler((t, e) ->
                log.error("uncaught exception in thread {}", t.getName(), e));
        checkLoopThread.start();
    }

    /**
     * try own leader, then call leader/follower call back
     *
     * leader status may not change to leader/follower when exception occured, will retry in scheduler
     */
    private void onInit() {
        log.info("on init status");
        // need init status to block request
        wrap(leaderStatusObserver::onInit);
        // fail fast
        if (lock.tryLock(0L, TimeUnit.MILLISECONDS)) {
            // Leader
            log.debug("lock succeed");
            onLeader();
        } else {
            // follower
            log.debug("lock failed");
            String host = redisUtil.get(LEADER_LOCK_KEY, String.class);
            if (host == null) {
                // just possible, but need to reInit
                onInit();
            }
            log.debug("localhost:{} leaderhost:{}", systemProperties.getIpPort(), host);
            if (Objects.equals(systemProperties.getIpPort(), host)) {
                // fix pod restart
                onLeader();
            } else {
                onFollower(host);
            }
        }
    }

    private void onLeader() {
        try {
            log.info("on leader status");
            currentLeader = systemProperties.getIpPort();
            wrap(leaderStatusObserver::onLeader);
            currentLeaderStatus = LeaderStatus.LEADER;
        } catch (Exception e) {
            log.error("change status error", e);
            lock.unlock();
            throw e;
        }
    }

    private void onFollower(String leaderHost) {
        log.info("on follower status,leader:{}", leaderHost);
        currentLeader = leaderHost;
        wrap(() -> leaderStatusObserver.onFollower(leaderHost));
        currentLeaderStatus = LeaderStatus.FOLLOWER;
    }

    private void wrap(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void leaderCheck() {
        switch (currentLeaderStatus) {
            case LEADER: {
                String host = redisUtil.get(LEADER_LOCK_KEY, String.class);
                if (!Objects.equals(systemProperties.getIpPort(), host)) {
                    // lost leader then reinit
                    log.debug("lost leader:{}", host);
                    currentLeaderStatus = LeaderStatus.INIT;
                    onInit();
                } else {
                    // still leader
                    redisUtil.expire(LEADER_LOCK_KEY, Duration.ofSeconds(10L));
                    wrap(leaderStatusObserver::onLeader);
                }
                break;
            }
            case FOLLOWER: {
                String host = redisUtil.get(LEADER_LOCK_KEY, String.class);
                if (host == null) {
                    log.debug("lost leader");
                    // lost leader then reinit
                    currentLeaderStatus = LeaderStatus.INIT;
                    onInit();
                } else if (!Objects.equals(currentLeader, host)) {
                    log.info("change currentLeader:{} leader:{}", currentLeader, host);
                    // leader change, then refollow
                    onFollower(host);
                } else {
                    // still follower
                    wrap(() -> leaderStatusObserver.onFollower(host));
                }
                break;
            }
            case INIT: {
                if (isRunning) {
                    // RETRY ON FAIL
                    onInit();
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

}
