package com.tdtech.cloudcmd.im.jingxin.server.config;


import com.tdtech.cloudcmd.encryptor.service.CachedConfig;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.tdtech.cloudcmd.encryptor.service.impl.EncryptionServiceImpl.LOCK_PREFIX;
import static com.tdtech.cloudcmd.im.jingxin.server.enums.Constant.ENCRYPT_DIR;
import static com.tdtech.cloudcmd.im.jingxin.server.util.EncryptImUtil.READ_TMP_DIR;
import static com.tdtech.cloudcmd.im.jingxin.server.util.EncryptImUtil.WRITE_TMP_DIR;

@Slf4j
@Component
public class ImEncryptTempClearScheduler {

    @Resource
    @Qualifier("imEncryptTaskExecutorService")
    private ThreadPoolTaskExecutor taskExecutor;


    @Autowired
    private EncryptionService encryptionService;


    @Autowired
    private RedisLockFactory redisLockFactory;

    /**
     * 临时文件有效期，10分钟，避免频繁清理数据，保证读数据的性能
     */
    @Value("${encrypt.im.tmp.expire.minutes:10}")
    private int tmpExpireMinutes;

    @Autowired
    private CachedConfig config;


    /**
     * 定时清理临时目录
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void scheduled() {
        if (!encryptionService.encryptEnabled()) {
            log.warn("加密功能未启用, 跳过临时文件清理");
            return;
        }

        // 执行清理
        taskExecutor.execute(this::cleanTempDirectories);
    }


    /**
     * 获取分布式锁的key
     */
    private String getRedisLockKey(String fileName) {
        return LOCK_PREFIX + ":" + DigestUtils.md5Hex(fileName);
    }


    public void cleanTempDirectories() {
        log.info("开始清理临时目录...");
        try {
            // 清理readtmp和writetmp目录
            String dbpath = config.getGlobalConfig("DBPATH");
            String dbPathUserCard = dbpath + "/userCard";
            cleanTmpDirectory(dbPathUserCard);
            cleanTmpDirectory(dbpath);
            log.info("临时目录清理完成");
        } catch (Exception e) {
            log.error("清理临时目录失败", e);
        }
    }


    /**
     * 清理过期文件
     */
    private void cleanExpiredFiles(Path dir) {
        Path path = dir.resolve(ENCRYPT_DIR);
        if (Files.notExists(path)) {
            log.warn("临时目录不存在: {}", path);
            return;
        }
        Path readTemp = path.resolve(READ_TMP_DIR);
        Path writeTemp = path.resolve(WRITE_TMP_DIR);
        try {
            doCleanExpiredFiles(readTemp);
            doCleanExpiredFiles(writeTemp);
        } catch (Exception e) {
             log.error("清理临时目录失败", e);
        }
}

    private void doCleanExpiredFiles(Path dir) throws IOException {
        if (Files.notExists(dir)) {
            log.warn("临时目录不存在: {}", dir);
            return;
        }
        File[] files = dir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                String lockKey = getRedisLockKey(file.getCanonicalPath());
                RedisLockFactory.RedisLock lock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(5));
                if (lock.tryLock(5L, TimeUnit.SECONDS)) {
                    try {
                        Path filePath = file.toPath();
                        if (isFileExpired(filePath)) {
                            Files.delete(filePath);
                            log.debug("删除过期临时文件: {}", file.getCanonicalPath());
                        }
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.warn("获取分布式锁失败, 跳过文件清理: {}", file.getCanonicalPath());
                }
            }
        }
    }

    /**
     * 检查文件是否过期
     */
    private boolean isFileExpired(Path file) throws IOException {
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        long expireTime = Instant.now().minus(Duration.ofMinutes(tmpExpireMinutes)).toEpochMilli();
        return lastModified < expireTime;
    }

    /**
     * 清理指定类型的临时目录
     */
    private void cleanTmpDirectory(String tmpDirName) throws IOException {
        Path baseDir = Paths.get(tmpDirName);
        if (!Files.exists(baseDir)) {
            log.warn("临时目录不存在: {}, 跳过文件清理", baseDir);
            return;
        }

        // 遍历所有groupId目录, 逐步清理
        try (var groupStream = Files.list(baseDir)) {
            groupStream.filter(f -> Files.isDirectory(f) && isGroupDir(f.getFileName().toFile().getName()))
                    .forEach(groupDir -> {
                        Path tmpDir = groupDir.resolve(groupDir);
                        if (Files.exists(tmpDir)) {
                            cleanExpiredFiles(tmpDir);
                        }
                    });
        }
    }

    private boolean isGroupDir(String fileDirName) {
        try {
            Long.parseLong(fileDirName);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
