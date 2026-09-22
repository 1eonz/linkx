package com.tdtech.cloudcmd.auth.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.auth.entity.RoleDataPriv;
import com.tdtech.cloudcmd.constant.AuthConstants;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoleDataPrivCacheService {

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration LOCK_EXPIRE = Duration.ofSeconds(30);
    private static final long LOCK_WAIT_SECONDS = 10L;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisLockFactory redisLockFactory;

    public List<RoleDataPriv> getOrBuild(Long roleId, Supplier<List<RoleDataPriv>> builder) {
        String cacheKey = AuthConstants.DATA_PRIV_TREE_CACHE + roleId;

        List<RoleDataPriv> cached = getFromCache(cacheKey);
        if (cached != null) {
            log.info("命中角色数据权限树缓存, roleId={}", roleId);
            return cached;
        }

        String lockKey = AuthConstants.DATA_PRIV_TREE_LOCK + roleId;
        RedisLockFactory.RedisLock lock = redisLockFactory.newRedisLock(lockKey, LOCK_EXPIRE);

        try {
            if (lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    List<RoleDataPriv> doubleChecked = getFromCache(cacheKey);
                    if (doubleChecked != null) {
                        log.debug("双重检查命中角色数据权限树缓存, roleId={}", roleId);
                        return doubleChecked;
                    }

                    List<RoleDataPriv> tree = builder.get();
                    putToCache(cacheKey, tree);
                    return tree;
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取角色数据权限树分布式锁超时, roleId={}, 降级为直接构建", roleId);
                return builder.get();
            }
        } catch (Exception e) {
            log.error("获取角色数据权限树分布式锁异常, roleId={}, 降级为直接构建", roleId, e);
            return builder.get();
        }
    }

    public void evict(Long roleId) {
        String cacheKey = AuthConstants.DATA_PRIV_TREE_CACHE + roleId;
        redisUtil.del(cacheKey);
        log.info("清理角色数据权限树缓存, roleId={}", roleId);
    }

    public void evict(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<String> keys = roleIds.stream()
                .map(id -> AuthConstants.DATA_PRIV_TREE_CACHE + id)
                .collect(Collectors.toList());
        redisUtil.del(keys);
        log.info("批量清理角色数据权限树缓存, roleIds={}", roleIds);
    }

    private List<RoleDataPriv> getFromCache(String cacheKey) {
        try {
            return redisUtil.get(cacheKey, new TypeReference<List<RoleDataPriv>>() {});
        } catch (Exception e) {
            log.error("读取角色数据权限树缓存异常, key={}", cacheKey, e);
            return null;
        }
    }

    private void putToCache(String cacheKey, List<RoleDataPriv> tree) {
        try {
            redisUtil.set(cacheKey, tree, CACHE_TTL);
            log.debug("写入角色数据权限树缓存, key={}", cacheKey);
        } catch (Exception e) {
            log.error("写入角色数据权限树缓存异常, key={}", cacheKey, e);
        }
    }
}