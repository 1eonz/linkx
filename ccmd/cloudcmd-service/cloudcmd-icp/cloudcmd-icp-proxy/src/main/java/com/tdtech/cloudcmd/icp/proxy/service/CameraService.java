package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.icp.proxy.client.IcpHttpClient;
import com.tdtech.cloudcmd.icp.proxy.client.entity.CameraResp;
import com.tdtech.cloudcmd.icp.proxy.client.entity.ResponseObject;
import com.tdtech.cloudcmd.icp.proxy.entity.Camera;
import com.tdtech.cloudcmd.icp.proxy.entity.CameraLevel;
import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.icp.proxy.entity.User;
import com.tdtech.cloudcmd.icp.proxy.repo.CameraMapper;
import com.tdtech.cloudcmd.icp.proxy.util.SeriaExecutor;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.GisNotify;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.Notify;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.UserNotify;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraService {

    private static final String PULL_LOCK_KEY = "cloudcmd:icp:camera:pulllock";

    private static final int PAGE_SIZE = 200;

    private final CameraMapper cameraMapper;
    private final IcpHttpClient icpHttpClient;
    private final AuthService authService;
    private final IdWorker idWorker;
    private final StreamBridge streamBridge;
    private final OnlineStatusService onlineStatusService;
    private final SeriaExecutor seriaExecutor;
    private final CameraLevelService cameraLevelService;
    private final IsdnTypeService isdnTypeService;

    public void tryInitData() {
        log.info("Application ready event received, initializing cameras...");
        try {
//            var cnt = cameraMapper.selectCount(Wrappers.lambdaQuery());
//            if (cnt != null && cnt != 0) {
//                log.info("Found {} existing cameras, skipping initialization", cnt);
//                return;
//            }
            log.info("pulling from ICP...");
            pull();
        } catch (Exception e) {
            log.error("Error during cameras initialization", e);
        }
    }

    public void pull() {
        seriaExecutor.run(PULL_LOCK_KEY, () -> {
            var now = new Date(System.currentTimeMillis() / 1000L * 1000L);//处理MYSQL精度问题
            List<CameraLevel> allCameraLevels = cameraLevelService.selectAll(new LambdaQueryWrapper<>());

            var firstResp = listPagedRaw(0, PAGE_SIZE);
            Integer totalNum = firstResp.getTotalNum();
            if (totalNum == null || totalNum == 0) {
                log.info("摄像头数据为空，无需同步");
                return;
            }

            int totalPages = (int) Math.ceil((double) totalNum / PAGE_SIZE);
            log.info("ICP服务器中摄像头总数: {}, 预计查询 {} 页", totalNum, totalPages);

            Set<String> remoteIsdnSet = new HashSet<>();
            List<Camera> toInsertList = new ArrayList<>();

            // 处理第一页
            processRemoteCameras(firstResp.getList(), allCameraLevels, remoteIsdnSet, toInsertList, now);

            // 处理剩余页
            for (int page = 1; page < totalPages; page++) {
                int offset = page * PAGE_SIZE;
                var resp = listPagedRaw(offset, PAGE_SIZE);
                processRemoteCameras(resp.getList(), allCameraLevels, remoteIsdnSet, toInsertList, now);

                //访问慢点 免得SDKServer爆炸
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    log.error("interrupted", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            log.info("远程摄像头数量: {}, 需要新增: {}", remoteIsdnSet.size(), toInsertList.size());

            boolean hasChange = false;
            // 注意：此处仅批量写入摄像头，不再触发批内 IsdnType 增量同步；
            // IsdnType 全量同步统一在 pull() 末尾通过 selectDistinctIsdnType() 完成。
            if (!toInsertList.isEmpty()) {
                int batchSize = 500;
                for (int i = 0; i < toInsertList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, toInsertList.size());
                    List<Camera> batch = toInsertList.subList(i, end);
                    cameraMapper.insertBatch(batch);
                }
                hasChange = true;
                log.info("Camera sync: inserted {} records", toInsertList.size());
            }

            // 删除：分批查询本地数据，逐批判断是否需要删除
            long deletedCount = deleteNotInRemote(remoteIsdnSet);
            if (deletedCount > 0) {
                hasChange = true;
            }
            // tb_camera 全量同步完成后，统一基于去重结果同步 IsdnType
            List<IsdnType> cameraIsdnTypes = selectDistinctIsdnType();
            if (!cameraIsdnTypes.isEmpty()) {
                log.info("开始全量同步 tb_camera 对应的 IsdnType，数量: {}", cameraIsdnTypes.size());
                isdnTypeService.syncIsdnType(cameraIsdnTypes);
            }
            // 订阅新增摄像头的在线状态
            if (!toInsertList.isEmpty()) {
                List<String> newIsdns = toInsertList.stream()
                        .map(Camera::getIsdn)
                        .filter(isdn -> isdn != null && !isdn.isBlank())
                        .collect(Collectors.toList());
                onlineStatusService.doSub(newIsdns);
            }

            if (hasChange) {
                log.info("摄像头数据同步完成，ICP服务器中摄像头总数：{}，新增 {}，删除 {}", totalNum, toInsertList.size(), deletedCount);
            } else {
                log.info("Camera sync: no changes, skipped");
            }
        });
    }

    public void insertOrUpdateIsdnType(List<Camera> cameras) {
        if (cameras == null || cameras.isEmpty()) {
            return;
        }
        Set<String> seenKeys = ConcurrentHashMap.newKeySet();
        List<IsdnType> newIsdnTypes = cameras.stream()
                .filter(camera -> seenKeys.add(isdnTypeService.buildIsdnTypeKey(camera.getCategory(), camera.getSubusercategory(), camera.getApptype())))
                .map(camera -> BeanCopyUtils.copyBean(camera, IsdnType::new))
                .collect(Collectors.toList());

        if (!newIsdnTypes.isEmpty()) {
            log.info("camera同步过来的IsdnType，数量: {}", newIsdnTypes.size());
            isdnTypeService.syncIsdnType(newIsdnTypes);
        }
    }

    public List<IsdnType> selectDistinctIsdnType() {
        List<IsdnType> distinctList = cameraMapper.selectDistinctKey();
        if (distinctList == null || distinctList.isEmpty()) {
            return Collections.emptyList();
        }
        return distinctList;
    }

    private void processRemoteCameras(List<CameraResp> remoteCameras, List<CameraLevel> allCameraLevels,
                                      Set<String> remoteIsdnSet, List<Camera> toInsertList, Date now) {
        if (remoteCameras == null || remoteCameras.isEmpty()) {
            return;
        }

        // 过滤掉不在层级列表中的摄像头
        List<CameraResp> filteredCameras = filterCamerasByLevelList(remoteCameras, allCameraLevels);
        if (filteredCameras.isEmpty()) {
            return;
        }

        // 提取当前批次的 isdn
        List<String> batchIsdns = filteredCameras.stream()
                .map(CameraResp::getIsdn)
                .filter(isdn -> isdn != null && !isdn.isBlank())
                .collect(Collectors.toList());

        // 只查询当前批次在本地存在的 isdn（分批查询，避免全量加载）
        Set<String> existingIsdns = Collections.emptySet();
        if (!batchIsdns.isEmpty()) {
            existingIsdns = cameraMapper.selectList(
                    Wrappers.lambdaQuery(Camera.class)
                            .select(Camera::getIsdn)
                            .in(Camera::getIsdn, batchIsdns)
            ).stream().map(Camera::getIsdn).collect(Collectors.toSet());
        }

        // 判断哪些需要新增
        for (CameraResp cameraResp : filteredCameras) {
            String isdn = cameraResp.getIsdn();
            if (isdn == null || isdn.isBlank()) {
                continue;
            }

            remoteIsdnSet.add(isdn);
            if (!existingIsdns.contains(isdn)) {
                Camera camera = BeanCopyUtils.copyBean(cameraResp, Camera::new);
                camera.setId(idWorker.nextId());
                camera.setCreateTime(now);
                // 解析位置信息
                var location = camera.getLocation();
                String[] split;
                if (location != null && !location.isBlank() && (split = location.split(",")).length >= 3) {
                    camera.setLon(new BigDecimal(split[0]));
                    camera.setLat(new BigDecimal(split[1]));
                    camera.setAlt(new BigDecimal(split[2]));
                }
                toInsertList.add(camera);
            }
        }
    }

    private long deleteNotInRemote(Set<String> remoteIsdnSet) {
        long batchSize = 1000;
        long totalDeleted = 0;

        for (long pageNum = 1L; ; pageNum++) {
            // 分页查询本地数据
            var localPage = cameraMapper.selectPageX(
                    new CcmdPageParam(pageNum, batchSize),
                    Wrappers.lambdaQuery(Camera.class).select(Camera::getIsdn)
            );

            if (localPage.isEmpty()) {
                break;
            }

            // 筛选出需要删除的（本地有，远程没有）
            List<String> toDelete = localPage.getRecords().stream()
                    .map(Camera::getIsdn)
                    .filter(isdn -> isdn != null && !remoteIsdnSet.contains(isdn))
                    .collect(Collectors.toList());

            if (!toDelete.isEmpty()) {
                cameraMapper.delete(Wrappers.lambdaQuery(Camera.class).in(Camera::getIsdn, toDelete));
                totalDeleted += toDelete.size();
            }
        }

        if (totalDeleted > 0) {
            log.info("Camera sync: deleted {} records", totalDeleted);
        }
        return totalDeleted;
    }

    private List<Camera> processAndSaveCameras(List<CameraResp> allCamera, List<CameraLevel> allCameraLevels, Date now) {
        if (allCamera == null || allCamera.isEmpty()) {
            return Collections.emptyList();
        }

        List<CameraResp> filterCamerasByLevelList = filterCamerasByLevelList(allCamera, allCameraLevels);
        if (filterCamerasByLevelList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Camera> cameras = BeanCopyUtils.copyList(filterCamerasByLevelList, Camera::new);

        // 查询数据库中已存在的记录，用于保留ID
        Map<String, Long> existingIdMap = getExistingIdMap(cameras);

        for (var camera : cameras) {
            fixCamera(camera, now, existingIdMap);
        }
        cameraMapper.insertOrUpdateBatch(cameras);
        insertOrUpdateIsdnType(cameras);

        List<String> isdns = cameras.stream()
                .map(Camera::getIsdn)
                .filter(isdn -> isdn != null && !isdn.isBlank())
                .collect(Collectors.toList());
        onlineStatusService.doSub(isdns);

        return cameras;
    }

    /**
     * LITTLE HELP GC
     */
    private ResponseObject<CameraResp> listPagedRaw(@NotNull Integer offset, @NotNull Integer limit) {
        return icpHttpClient.getCamera(authService.getSessionStr(), offset, limit);
    }

    private Map<String, Long> getExistingIdMap(List<Camera> cameras) {
        Map<String, Long> existingIdMap = new HashMap<>();
        List<String> isdns = cameras.stream()
                .map(Camera::getIsdn)
                .filter(isdn -> isdn != null && !isdn.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (!isdns.isEmpty()) {
            List<Camera> existingRecords = cameraMapper.selectList(
                    Wrappers.lambdaQuery(Camera.class)
                            .in(Camera::getIsdn, isdns)
                            .select(Camera::getId, Camera::getIsdn)
            );
            existingRecords.forEach(record ->
                    existingIdMap.put(record.getIsdn(), record.getId())
            );
        }
        return existingIdMap;
    }

    public <R> CcmdPage<R> selectPage(CcmdPageParam pageParam, Class<R> cls, MPJLambdaWrapper<Camera> queryWrapper) {
        return cameraMapper.selectJoinPage(pageParam, cls, queryWrapper);
    }

    public CcmdPage<Camera> selectPage(CcmdPageParam pageParam, LambdaQueryWrapper<Camera> queryWrapper) {
        return cameraMapper.selectPageX(pageParam, queryWrapper);
    }

    @Async
    public void handleGisNotify(JsonNode jsonNode) {
        var mo = Optional.ofNullable(jsonNode.get("mo")).map(JsonNode::asText).orElse("");
        var opt = Optional.ofNullable(jsonNode.get("opt")).map(JsonNode::asText).orElse("");
        if (!Objects.equals(mo, "23") && !Objects.equals(opt, "modify")) {
            return;
        }
        var notify = JsonUtil.convert(jsonNode, new TypeReference<Notify<GisNotify>>() {
        });
        var isdn = Optional.ofNullable(notify).map(Notify::getValue).map(GisNotify::getIsdn).orElse(null);
        if (isdn == null) {
            log.warn("meet strange notify:{}", JsonUtil.toJsonStr(jsonNode));
            return;
        }
        var location = Optional.ofNullable(notify).map(Notify::getValue).map(GisNotify::getLocation).orElse(null);
        if (location == null || location.isBlank()) {
            cameraMapper.update(null, Wrappers.lambdaUpdate(Camera.class)//
                    .eq(Camera::getIsdn, isdn)//
                    .set(Camera::getLocation, null)//
                    .set(Camera::getLon, null)//
                    .set(Camera::getLat, null)//
                    .set(Camera::getAlt, null)//
            );
        } else {
            var split = location.split(",");
            var lon = split[0].isBlank() ? null : new BigDecimal(split[0]);
            var lat = split[1].isBlank() ? null : new BigDecimal(split[1]);
            var alt = split[2].isBlank() ? null : new BigDecimal(split[2]);
            cameraMapper.update(null, Wrappers.lambdaUpdate(Camera.class)//
                    .eq(Camera::getIsdn, isdn)//
                    .set(Camera::getLocation, location)//
                    .set(Camera::getLon, lon)//
                    .set(Camera::getLat, lat)//
                    .set(Camera::getAlt, alt)//
            );
        }
    }

    @Async
    public void handleWsNotify(JsonNode jsonNode) {
        var mo = Optional.ofNullable(jsonNode.get("mo")).map(JsonNode::asText).orElse("");
        if (!Objects.equals(mo, "1") && !Objects.equals(mo, "39")) {
            return;
        }
        var notify = JsonUtil.convert(jsonNode, new TypeReference<Notify<UserNotify>>() {
        });
        var isdn = Optional.ofNullable(notify).map(Notify::getValue).map(UserNotify::getIsdn).orElse(null);
        if (isdn == null) {
            log.warn("meet strange notify:{}", JsonUtil.toJsonStr(jsonNode));
            return;
        }
        var opt = notify.getOpt();
        if (opt == null) {
            log.warn("meet strange notify:{}", JsonUtil.toJsonStr(jsonNode));
            return;
        }
        switch (opt) {
            case "modify": {
                try {
                    // UDC修改摄像头层级后，ICP内部同步会延迟，需要等待一段时间后才能获取到最新层级信息
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                var cameraByIsdn = icpHttpClient.getCameraByIsdn(authService.getSessionStr(), isdn);
                var camera = BeanCopyUtils.copyBean(cameraByIsdn, Camera::new);
                if (camera.getIsdn() == null || camera.getIsdn().isBlank()) {
                    log.warn("invalid camera:{}", JsonUtil.toJsonStr(jsonNode));
                    break;
                }

                Camera oldCamera = cameraMapper.selectOne(new LambdaQueryWrapper<>(Camera.class).eq(Camera::getIsdn, isdn));
                if (oldCamera != null) {
                    camera.setId(oldCamera.getId());
                } else {
                    camera.setId(idWorker.nextId());
                }
                camera.setLocation(null);//更新的时候不更新位置
//                cameraMapper.delete(Camera::getIsdn, isdn);
//                cameraMapper.insert(camera);
                cameraMapper.update(camera, Wrappers.lambdaUpdate(Camera.class).eq(Camera::getIsdn, isdn));
                insertOrUpdateIsdnType(Collections.singletonList(camera));
                onlineStatusService.doSub(List.of(isdn));
                publishCameraLevelMsg("UPSERT", camera);
                break;
            }
            case "create": {
                var cameraByIsdn = icpHttpClient.getCameraByIsdn(authService.getSessionStr(), isdn);
                var camera = BeanCopyUtils.copyBean(cameraByIsdn, Camera::new);
                if (camera.getIsdn() == null || camera.getIsdn().isBlank()) {
                    log.warn("invalid camera:{}", JsonUtil.toJsonStr(jsonNode));
                    break;
                }
                fixCamera(camera, new Date());
                cameraMapper.delete(Camera::getIsdn, isdn);
                cameraMapper.insert(camera);
                insertOrUpdateIsdnType(Collections.singletonList(camera));
                onlineStatusService.doSub(List.of(isdn));
                publishCameraLevelMsg("UPSERT", camera);
                break;
            }
            case "delete": {
                cameraMapper.delete(Camera::getIsdn, isdn);
                publishCameraLevelMsg("DELETE", isdn);
                break;
            }
            default: {
                log.warn("meet strange notify:{}", JsonUtil.toJsonStr(jsonNode));
            }
        }
    }

    private void fixCamera(Camera camera, Date createTime) {
        fixCamera(camera, createTime, Collections.emptyMap());
    }

    private void fixCamera(Camera camera, Date createTime, Map<String, Long> existingIdMap) {
        // 如果数据库中已存在相同isdn的记录，保留原ID，否则生成新ID
        Long existingId = existingIdMap.get(camera.getIsdn());
        camera.setId(existingId != null ? existingId : idWorker.nextId());
        camera.setCreateTime(createTime);
        var location = camera.getLocation();
        String[] split;
        if (location != null && !location.isBlank() && (split = location.split(",")).length >= 3) {
            camera.setLon(new BigDecimal(split[0]));
            camera.setLat(new BigDecimal(split[1]));
            camera.setAlt(new BigDecimal(split[2]));
        }
    }

    private void publishCameraLevelMsg(String notifyType, Object payload) {
        var build = new CagentMqFrame().toBuilder().typeSubSystemMessage("ICP_CAMERA").multicast()
                .appKeys(List.of("CDC-1000", "CAPP-1000", "CAPP-3000")).build().body("ICP_CAMERA", notifyType, payload)
                .build();
        streamBridge.send("cloudcmd-cagent", build);
        //        log.info("send cagent msg:{} done", build);
    }

    public static List<CameraResp> filterCamerasByLevelList(List<CameraResp> allCameraList, List<CameraLevel> filtered) {
        Set<String> levelSet = filtered.stream()
                .map(CameraLevel::getLevelNumber)
                .collect(Collectors.toSet());

        return allCameraList.stream()
                .filter(camera -> levelSet.contains(camera.getLevelNumber()))
                .collect(Collectors.toList());
    }
}