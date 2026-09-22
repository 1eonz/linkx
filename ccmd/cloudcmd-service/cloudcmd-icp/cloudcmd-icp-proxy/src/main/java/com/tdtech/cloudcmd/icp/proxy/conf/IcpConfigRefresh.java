package com.tdtech.cloudcmd.icp.proxy.conf;

import com.tdtech.cloudcmd.icp.proxy.entity.IcpConfig;
import com.tdtech.cloudcmd.icp.proxy.repo.CameraLevelMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.CameraMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.DepartmentMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.UserMapper;
import com.tdtech.cloudcmd.icp.proxy.service.AuthService;
import com.tdtech.cloudcmd.icp.proxy.service.IcpPrivService;
import com.tdtech.cloudcmd.icp.proxy.service.ImUserService;
import com.tdtech.cloudcmd.icp.proxy.util.CachedGlobalConfig;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.icp.proxy.ws.WsConfiguration;
import com.tdtech.cloudcmd.im.jingxin.client.ClientConfigGroup;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class IcpConfigRefresh {

    private static final List<String> NAMES =
            List.of("ICP_SDKSERVER_WS_URI", "ICP_SDKSERVER_HTTP_HOST", "ICP_PASSWORD", "ICP_ACCOUNT");
    private static final String REDIS_LOCK_KEY = "cloudcmd:icp:conf:refreshlock";

    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(10);

    private final WsConfiguration wsConfiguration;
    private final AuthService authService;
    private final CachedGlobalConfig cachedGlobalConfig;
    private final RedisLockFactory redisLockFactory;
    private final ImUserService imUserService;
    private final IcpPrivService icpPrivService;
    private final DepartmentMapper departmentMapper;
    private final CameraLevelMapper cameraLevelMapper;
    private final CameraMapper cameraMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;

    @PreDestroy
    public void preDestory() {
        scheduledExecutorService.shutdownNow();
    }

    @Bean("onGlobalConfigChangeEvent")
    public Consumer<String> onGlobalConfigChangeEvent() {
        return msg -> {
            log.info("on global config change:{}", msg);
            var jsonObject = JsonUtil.parseJson(msg);
            var name = jsonObject.getString("name");
            cachedGlobalConfig.clear();
            if (NAMES.contains(name)) {
                var redisLock = redisLockFactory.newRedisLock(REDIS_LOCK_KEY, Duration.ofMinutes(10L));
                if (redisLock.tryLock(0L, TimeUnit.SECONDS)) {
                    log.warn("lockbusy");
                    return;
                }
                scheduledExecutorService.schedule(() -> {
                    try {
                        wsConfiguration.disConnect();
                        authService.clearSession();
                        authService.getSessionStr();
                    } finally {
                        redisLock.unlock();
                    }
                }, 5L, TimeUnit.MINUTES);
            } else if (ClientConfigGroup.coopConfigNames().contains(name)) {
                imUserService.refreshImUser();
            }
        };
    }

    @Bean("onIcpConfigChangeEvent")
    public Consumer<String> onIcpConfigChangeEvent() {
        return msg -> {
            log.info("on icp config change:{}", msg);
            IcpConfig newConfig = JsonUtil.parseJson(msg, IcpConfig.class);
            if (newConfig == null) {
                log.warn("Parsed Config is null, skipping");
                return;
            }
            IcpConfig oldConfig = SyncUtil.getIcpConfig();
            
            // 检测IP变动，排除新增的场景
            boolean ipChanged = false;
            if (oldConfig != null && oldConfig.getIp() != null && !oldConfig.getIp().isEmpty()){
                ipChanged = !StringUtils.isEquals(oldConfig.getIp(), newConfig.getIp());
            }
            
            // 当检测到IP变动时，根据根节点是否为空清理权限
            if (ipChanged) {
                if (newConfig.getDepartmentId() == null || newConfig.getDepartmentId().isEmpty()) {
                    log.info("IP changed and department root is empty, clearing department privileges");
                    icpPrivService.clearAllUserPriv();
                }

                if (newConfig.getCameraLevelId() == null || newConfig.getCameraLevelId().isEmpty()) {
                    log.info("IP changed and camera level root is empty, clearing camera level privileges");
                    icpPrivService.clearAllCameraPriv();
                }

                // 更换对接服务器时 清空所有数据
                departmentMapper.delete(null);
                cameraLevelMapper.delete(null);
                cameraMapper.delete(null);
                userMapper.delete(null);
                
                // 清空 CameraLevel Redis 缓存
                redisUtil.del("cloudcmd:icp:cameralevel:tree");
            }
            
            SyncUtil.setIcpConfig(newConfig);
            scheduledExecutorService.schedule(() -> {
                wsConfiguration.disConnect();
                authService.clearSession();
                authService.getSessionStr();
            }, 5L, TimeUnit.MINUTES);
        };
    }
}
