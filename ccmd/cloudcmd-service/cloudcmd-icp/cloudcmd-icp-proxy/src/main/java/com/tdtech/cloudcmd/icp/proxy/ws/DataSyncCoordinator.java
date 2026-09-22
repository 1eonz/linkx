package com.tdtech.cloudcmd.icp.proxy.ws;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.icp.proxy.entity.Camera;
import com.tdtech.cloudcmd.icp.proxy.entity.User;
import com.tdtech.cloudcmd.icp.proxy.repo.CameraMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.UserMapper;
import com.tdtech.cloudcmd.icp.proxy.service.*;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.icp.proxy.ws.event.WsAuthSucceedEvent;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 数据同步协调器
 * 统一管理各 Service 的数据同步顺序，确保依赖关系正确
 * 执行顺序：
 * 1. 第一批（并行）：DepartmentService.pull() + CameraLevelService.pull()
 * 2. 第二批（并行）：CameraService.pull() + UserService.pull()
 * 3. 第三批：ImUserService.refreshImUser()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncCoordinator {

    private final DepartmentService departmentService;
    private final CameraLevelService cameraLevelService;
    private final CameraService cameraService;
    private final UserService userService;
    private final ImUserService imUserService;
    private final CameraMapper cameraMapper;
    private final UserMapper userMapper;
    private final OnlineStatusService onlineStatusService;
    private final GisService gisService;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;

    @Async
    @EventListener({WsAuthSucceedEvent.class})
    public void syncAll() {
        log.info("开始数据同步...");

        // 第一批：并行执行部门、层级数据同步
        CompletableFuture<Void> deptFuture = runSafe("Department", () -> {
            departmentService.pull();
            SyncUtil.initDept(departmentService.reqAllDepts());
        });
        CompletableFuture<Void> levelFuture = runSafe("CameraLevel", () -> {
            cameraLevelService.pull();
            SyncUtil.initCameraLevel(cameraLevelService.reqAllCameraLevel());
        });

        // 等待第一批完成
        CompletableFuture.allOf(deptFuture, levelFuture).join();
        log.info("基础数据（Department、CameraLevel）同步完成");

        // 第二批：并行执行用户、摄像头数据同步
        CompletableFuture<Void> cameraFuture = runSafe("Camera", () -> {
            cameraService.pull();
            reSubscribeCamera();
        });
        CompletableFuture<Void> userFuture = runSafe("User", () -> {
            userService.pull();
            reSubscribeUser();
        });

        // 等待第二批完成
        CompletableFuture.allOf(cameraFuture, userFuture).join();
        log.info("依赖数据（Camera、User）同步完成");

        // 第三批：IM用户同步
        runSafe("ImUser", imUserService::refreshImUser).join();
        log.info("IM用户数据同步完成");

        log.info("数据同步全部完成");
    }

    /**
     * 安全执行任务，捕获异常并记录日志，不影响其他任务
     *
     * @param taskName 任务名称，用于日志标识
     * @param task     要执行的任务
     * @return CompletableFuture
     */
    private CompletableFuture<Void> runSafe(String taskName, Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("{} 同步失败", taskName, e);
            }
        }, asyncMessageTaskExecutor.getThreadPoolExecutor());
    }

    private void reSubscribeCamera() {
        log.info("re subscribe camera");
        for (long pageNum = 1L, pageSize = 150L; ; pageNum++) {
            var cameraCcmdPage =
                cameraMapper.selectPageX(new CcmdPageParam(pageNum, pageSize), Wrappers.lambdaQuery(Camera.class));
            log.info("re sub camera:{}", cameraCcmdPage.getRecords().size());
            if (cameraCcmdPage.isEmpty()) {
                break;
            }
            var isdns = cameraCcmdPage.getRecords().stream().map(Camera::getIsdn).collect(Collectors.toList());
            onlineStatusService.doSub(isdns);
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                log.error("interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void reSubscribeUser() {
        log.info("re subscribe user");
        for (long pageNum = 1L, pageSize = 150L; ; pageNum++) {
            var userCcmdPage =
                userMapper.selectPageX(new CcmdPageParam(pageNum, pageSize), Wrappers.lambdaQuery(User.class));
            log.info("re sub user:{}", userCcmdPage.getRecords().size());
            if (userCcmdPage.isEmpty()) {
                break;
            }
            var isdns = userCcmdPage.getRecords().stream().map(User::getIsdn).collect(Collectors.toList());
            onlineStatusService.doSub(isdns);
            gisService.doSub(isdns);
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                log.error("interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
