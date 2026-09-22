package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.icp.proxy.client.IcpHttpClient;
import com.tdtech.cloudcmd.icp.proxy.client.entity.CameraLevelResp;
import com.tdtech.cloudcmd.icp.proxy.entity.CameraLevel;
import com.tdtech.cloudcmd.icp.proxy.repo.CameraLevelMapper;
import com.tdtech.cloudcmd.icp.proxy.util.SeriaExecutor;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.CameraLevelNotify;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.Notify;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.TreeUtil;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraLevelService {

    private static final String PULL_LOCK_KEY = "cloudcmd:icp:cameralevel:pulllock";
    private static final String CAMERALEVEL_KEY = "cloudcmd:icp:cameralevel:tree";

    private final CameraLevelMapper cameraLevelMapper;
    private final IcpHttpClient icpHttpClient;
    private final AuthService authService;
    private final IdWorker idWorker;
    private final StreamBridge streamBridge;
    private final SeriaExecutor seriaExecutor;
    private final IcpPrivService icpPrivService;
    private final RedisUtil redisUtil;

    public void tryInitData() {
        log.info("Application ready event received, initializing camera levels...");
        try {
            log.info("Pulling camera levels from ICP...");
            pull();
        } catch (Exception e) {
            log.error("Error during camera levels initialization", e);
        }
    }

    public void pull() {
        seriaExecutor.run(PULL_LOCK_KEY, () -> {
            var cameraLevels = reqFilterCameraLevel();
            if (cameraLevels == null || cameraLevels.isEmpty()) {
                return;
            }
            var remoteList = fixColumns(cameraLevels);
            remoteList.sort(Comparator.comparing(CameraLevel::getNodeName));

            Set<String> remoteLevelNumbers = remoteList.stream()
                    .map(CameraLevel::getLevelNumber)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());

            List<CameraLevel> localList = cameraLevelMapper.selectList(
                    Wrappers.lambdaQuery(CameraLevel.class)
                            .select(CameraLevel::getLevelNumber)
            );
            Set<String> localLevelNumbers = localList.stream()
                    .map(CameraLevel::getLevelNumber)
                    .collect(Collectors.toSet());

            // 需要新增的（ICP有，本地无）
            List<CameraLevel> toInsert = remoteList.stream()
                    .filter(item -> !localLevelNumbers.contains(item.getLevelNumber()))
                    .collect(Collectors.toList());

            // 需要删除的（本地有，ICP无）
            Set<String> toDelete = new HashSet<>(localLevelNumbers);
            toDelete.removeAll(remoteLevelNumbers);

            boolean hasChange = false;

            if (!toDelete.isEmpty()) {
                cameraLevelMapper.delete(
                        Wrappers.lambdaQuery(CameraLevel.class)
                                .in(CameraLevel::getLevelNumber, toDelete)
                );
                hasChange = true;
                log.info("Camera level sync: deleted {} records", toDelete.size());
            }

            if (!toInsert.isEmpty()) {
                var group = CollectionUtils.group(toInsert, 500);
                for (var value : group.values()) {
                    cameraLevelMapper.insertBatch(value);
                }
                hasChange = true;
                log.info("Camera level sync: inserted {} records", toInsert.size());
            }

            List<CameraLevel> allData = cameraLevelMapper.selectList(null);
            List<TreeUtil.TreeNode<CameraLevel>> tree = TreeUtil.buildTreeOptimized(
                    allData,
                    CameraLevel::getLevelNumber,
                    CameraLevel::getHighLevelNumber
            );
            redisUtil.set(CAMERALEVEL_KEY, tree);

            Set<String> cameraLevelNumbers = allData.stream()
                    .map(CameraLevel::getLevelNumber)
                    .collect(Collectors.toSet());
            icpPrivService.clearInvalidCameraPriv(cameraLevelNumbers);
            log.info("Camera level sync: redis cache rebuilt, total {} records", allData.size());
        });
    }

    private List<CameraLevel> fixColumns(List<CameraLevel> departments) {
        var tree = TreeUtil.buildTreeOptimized(
                departments,
                CameraLevel::getLevelNumber,
                CameraLevel::getHighLevelNumber
        );
        var result = new LinkedList<CameraLevel>();
        var now = new Date();

        // 查询数据库中已存在的记录，用于保留ID
        Map<String, Long> existingIdMap = new HashMap<>();
        if (!departments.isEmpty()) {
            List<String> levelNumbers = departments.stream()
                    .map(CameraLevel::getLevelNumber)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            if (!levelNumbers.isEmpty()) {
                List<CameraLevel> existingRecords = cameraLevelMapper.selectList(
                        Wrappers.lambdaQuery(CameraLevel.class)
                                .in(CameraLevel::getLevelNumber, levelNumbers)
                                .select(CameraLevel::getId, CameraLevel::getLevelNumber)
                );
                existingRecords.forEach(record ->
                        existingIdMap.put(record.getLevelNumber(), record.getId())
                );
            }
        }

        TreeUtil.depthFirstTraverse(tree, null, (p, c) -> {
            var data = c.getData();
            // 如果数据库中已存在相同level_number的记录，保留原ID，否则生成新ID
            Long existingId = existingIdMap.get(data.getLevelNumber());
            data.setId(existingId != null ? existingId : idWorker.nextId());
            data.setCreateTime(now);
            if (p == null) {
                data.setLevelNumberPath(data.getLevelNumber());
            } else {
                var pData = p.getData();
                data.setLevelNumberPath(pData.getLevelNumberPath() + ":" + data.getLevelNumber());
            }
            result.add(data);
        });
        return result;
    }

    /**
     * HELP GC
     */
    private List<CameraLevel> reqFilterCameraLevel() {
        var allCameraLevel = icpHttpClient.getAllCameraLevel(authService.getSessionStr());
        if (allCameraLevel == null || allCameraLevel.isEmpty()) {
            return Collections.emptyList();
        }

        String cameraLevelId = SyncUtil.getIcpConfig().getCameraLevelId();
        List<CameraLevelResp> subCameraLevel = findSubCameraLevel(allCameraLevel, cameraLevelId);
        return BeanCopyUtils.copyList(subCameraLevel, CameraLevel::new);
    }

    public List<CameraLevel> reqAllCameraLevel() {
        List<CameraLevelResp> allCameraLevel = icpHttpClient.getAllCameraLevel(authService.getSessionStr());
        if (allCameraLevel == null || allCameraLevel.isEmpty()) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyList(allCameraLevel, CameraLevel::new);
    }

    public CameraLevel selectone(LambdaQueryWrapper<CameraLevel> queryWrapper) {
        return cameraLevelMapper.selectFirstX(queryWrapper);
    }

    public List<CameraLevel> selectAll(LambdaQueryWrapper<CameraLevel> queryWrapper) {
        return cameraLevelMapper.selectList(queryWrapper);
    }

    public List<CameraLevel> selectList(LambdaQueryWrapper<CameraLevel> queryWrapper) {
        return cameraLevelMapper.selectList(queryWrapper);
    }

    public List<TreeUtil.TreeNode<CameraLevel>> getCameraLevelTree() {
        List<TreeUtil.TreeNode<CameraLevel>> list = redisUtil.get(CAMERALEVEL_KEY, List.class);
        if (list == null) {
            throw new BusinessException("摄像头层级查询失败");
        }
        return list;
    }

    /**
     * 分页查询摄像头层级
     *
     * @param pageParam    分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public CcmdPage<CameraLevel> selectPage(CcmdPageParam pageParam, LambdaQueryWrapper<CameraLevel> queryWrapper) {
        return cameraLevelMapper.selectPageX(pageParam, queryWrapper);
    }

    @Async
    public void handleWsNotify(JsonNode jsonNode) {
        var mo = Optional.ofNullable(jsonNode.get("mo")).map(JsonNode::asText).orElse("");
        if (!Objects.equals(mo, "40")) {
            return;
        }
        var notify = JsonUtil.convert(jsonNode, new TypeReference<Notify<CameraLevelNotify>>() {
        });
        var levelNum =
                Optional.ofNullable(notify).map(Notify::getValue).map(CameraLevelNotify::getLevelNumber).orElse(null);
        if (levelNum == null) {
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
                pull();
                publishCameraLevelMsg("REFRESH_ALL", 1);
                break;
            }
            case "create": {
                var cameraLevel = BeanCopyUtils.copyBean(notify.getValue(), CameraLevel::new);
                // 检查数据库中是否已存在相同level_number的记录
                var existing = cameraLevelMapper.selectFirstX(
                        Wrappers.lambdaQuery(CameraLevel.class)
                                .eq(CameraLevel::getLevelNumber, cameraLevel.getLevelNumber())
                                .select(CameraLevel::getId));
                // 如果存在则保留原ID，否则生成新ID
                cameraLevel.setId(existing != null ? existing.getId() : idWorker.nextId());
                cameraLevel.setCreateTime(new Date());
                var highLevelNumber = notify.getValue().getHighLevelNumber();
                if (highLevelNumber == null || Objects.equals(highLevelNumber, "0")) {
                    cameraLevel.setLevelNumberPath(cameraLevel.getLevelNumber());
                } else {
                    var high = cameraLevelMapper.selectFirstX(
                            Wrappers.lambdaQuery(CameraLevel.class).eq(CameraLevel::getLevelNumber, highLevelNumber));
                    if (high == null) {
                        log.warn("no parent found for:{}", notify);
                        return;
                    }
                    cameraLevel.setLevelNumberPath(high.getLevelNumberPath() + ":" + cameraLevel.getLevelNumber());
                }
                // 使用 insertOrUpdateBatch 而不是 insert，避免重复插入
                cameraLevelMapper.insertOrUpdateBatch(List.of(cameraLevel));
                publishCameraLevelMsg("CREATE", cameraLevel);
                break;
            }
            case "delete": {
                cameraLevelMapper.delete(CameraLevel::getLevelNumber, levelNum);
                publishCameraLevelMsg("DELETE", levelNum);
                break;
            }
            default: {
                log.warn("meet strange notify:{}", JsonUtil.toJsonStr(jsonNode));
            }
        }
    }

    private void publishCameraLevelMsg(String notifyType, Object payload) {
        var build = new CagentMqFrame().toBuilder().typeSubSystemMessage("ICP_CAMERA_LEVEL").multicast()
                .appKeys(List.of("CDC-1000", "CAPP-1000", "CAPP-3000")).build()
                .body("ICP_CAMERA_LEVEL", notifyType, payload).build();
        streamBridge.send("cloudcmd-cagent", build);
        //        log.info("send cagent msg:{} done", build);
    }

    @Scheduled(initialDelay = 10000L, fixedDelay = 60L * 1000L)
    public void pullCameraLevel() {
        Long count = redisUtil.exists(CAMERALEVEL_KEY);
        boolean exists = count != null && count > 0;
        if (!exists) {
            log.info("Camera levels not initialized, pulling from ICP...");
            pull();
        }
    }

    private List<CameraLevelResp> findSubCameraLevel(List<CameraLevelResp> allCameraLevel, String levelNumber) {
        if (StringUtils.isBlank(levelNumber)) {
            return allCameraLevel;
        }

        Map<String, List<CameraLevelResp>> upperToSubordinates = new HashMap<>();
        for (CameraLevelResp level : allCameraLevel) {
            String upperId = level.getHighLevelNumber();
            upperToSubordinates.computeIfAbsent(upperId, k -> new ArrayList<>()).add(level);
        }

        List<CameraLevelResp> result = new ArrayList<>();
        Queue<CameraLevelResp> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // 先把当前部门加入结果集
        allCameraLevel.stream()
                .filter(dept -> dept.getLevelNumber().equals(levelNumber))
                .findFirst()
                .ifPresent(result::add);

        List<CameraLevelResp> directSubordinates = upperToSubordinates.getOrDefault(levelNumber, Collections.emptyList());
        for (CameraLevelResp levelResp : directSubordinates) {
            if (!visited.contains(levelResp.getLevelNumber())) {
                queue.add(levelResp);
                visited.add(levelResp.getLevelNumber());
            }
        }

        while (!queue.isEmpty()) {
            CameraLevelResp current = queue.poll();
            result.add(current);

            List<CameraLevelResp> subSubordinates = upperToSubordinates.getOrDefault(current.getLevelNumber(), Collections.emptyList());
            for (CameraLevelResp sub : subSubordinates) {
                if (!visited.contains(sub.getLevelNumber())) {
                    queue.add(sub);
                    visited.add(sub.getLevelNumber());
                }
            }
        }
        return result;
    }
}