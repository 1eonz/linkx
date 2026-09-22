package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.icp.proxy.client.IcpHttpClient;
import com.tdtech.cloudcmd.icp.proxy.client.entity.DepartmentResp;
import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import com.tdtech.cloudcmd.icp.proxy.repo.DepartmentMapper;
import com.tdtech.cloudcmd.icp.proxy.util.SeriaExecutor;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.DepartmentNotify;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.Notify;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.TreeUtil;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final String PULL_LOCK_KEY = "cloudcmd:icp:department:pulllock";

    private final DepartmentMapper departmentMapper;
    private final IcpHttpClient icpHttpClient;
    private final AuthService authService;
    private final IdWorker idWorker;
    private final StreamBridge streamBridge;
    private final SeriaExecutor seriaExecutor;
    private final IcpPrivService icpPrivService;

    public void tryInitData() {
        try {
            pull();
        } catch (RuntimeException e) {
            log.error("pull data error", e);
        }
    }

    public void pull() {
        seriaExecutor.run(PULL_LOCK_KEY, () -> {
            var departments = reqAll();
            if (departments == null || departments.isEmpty()) {
                return;
            }
            var remoteList = fixColumns(departments);

            Set<String> remoteDepartmentIds = remoteList.stream()
                    .map(Department::getDepartmentid)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());

            List<Department> localList = departmentMapper.selectList(
                    Wrappers.lambdaQuery(Department.class)
                            .select(Department::getDepartmentid)
            );
            Set<String> localDepartmentIds = localList.stream()
                    .map(Department::getDepartmentid)
                    .collect(Collectors.toSet());

            List<Department> toInsert = remoteList.stream()
                    .filter(item -> !localDepartmentIds.contains(item.getDepartmentid()))
                    .collect(Collectors.toList());

            Set<String> toDelete = new HashSet<>(localDepartmentIds);
            toDelete.removeAll(remoteDepartmentIds);

            boolean hasChange = false;

            if (!toDelete.isEmpty()) {
                departmentMapper.delete(
                        Wrappers.lambdaQuery(Department.class)
                                .in(Department::getDepartmentid, toDelete)
                );
                hasChange = true;
                log.info("Department sync: deleted {} records", toDelete.size());
            }

            if (!toInsert.isEmpty()) {
                var group = CollectionUtils.group(toInsert, 500);
                for (var value : group.values()) {
                    departmentMapper.insertBatch(value);
                }
                hasChange = true;
                log.info("Department sync: inserted {} records", toInsert.size());
            }

            if (hasChange) {
                icpPrivService.clearInvalidUserPriv(remoteDepartmentIds);
                log.info("Department sync: completed, total {} records", remoteList.size());
            } else {
                log.info("Department sync: no changes, skipped");
            }
        });
    }

    public List<Department> selectAll(Wrapper<Department> wrapper) {
        return departmentMapper.selectList(wrapper);
    }

    private List<Department> fixColumns(List<Department> departments) {
        var tree =
                TreeUtil.buildTreeOptimized(departments, Department::getDepartmentid, Department::getUpperdepartmentId);
        var result = new LinkedList<Department>();
        var now = new Date();

        // 查询数据库中已存在的记录，用于保留ID
        Map<String, Long> existingIdMap = new HashMap<>();
        if (!departments.isEmpty()) {
            List<String> departmentIds = departments.stream()
                    .map(Department::getDepartmentid)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            if (!departmentIds.isEmpty()) {
                List<Department> existingRecords = departmentMapper.selectList(
                        Wrappers.lambdaQuery(Department.class)
                                .in(Department::getDepartmentid, departmentIds)
                                .select(Department::getId, Department::getDepartmentid)
                );
                existingRecords.forEach(record ->
                        existingIdMap.put(record.getDepartmentid(), record.getId())
                );
            }
        }

        TreeUtil.depthFirstTraverse(tree, null, (p, c) -> {
            var data = c.getData();
            // 如果数据库中已存在相同departmentid的记录，保留原ID，否则生成新ID
            Long existingId = existingIdMap.get(data.getDepartmentid());
            data.setId(existingId != null ? existingId : idWorker.nextId());
            data.setCreateTime(now);
            if (p == null) {
                data.setDepartmentIdPath(data.getDepartmentid());
                data.setDepartmentNamePath(data.getDepartmentname());
            } else {
                var pData = p.getData();
                data.setDepartmentIdPath(pData.getDepartmentIdPath() + ":" + data.getDepartmentid());
                data.setDepartmentNamePath(pData.getDepartmentNamePath() + ":" + data.getDepartmentname());
            }
            result.add(data);
        });
        return result;
    }

    /**
     * HELP GC
     */
    private List<Department> reqAll() {
        List<DepartmentResp> allDepartment = icpHttpClient.getAllDepartment(authService.getSessionStr());
        if (allDepartment == null || allDepartment.isEmpty()) {
            return Collections.emptyList();
        }

        List<DepartmentResp> subDepartment = findSubDepartment(allDepartment, SyncUtil.getIcpConfig().getDepartmentId());
        return BeanCopyUtils.copyList(subDepartment, Department::new);
    }

    public List<Department> reqAllDepts() {
        List<DepartmentResp> allDepartment = icpHttpClient.getAllDepartment(authService.getSessionStr());
        if (allDepartment == null || allDepartment.isEmpty()) {
            return Collections.emptyList();
        }

        return BeanCopyUtils.copyList(allDepartment, Department::new);
    }

    public Department selectOne(LambdaQueryWrapper<Department> queryWrapper) {
        return departmentMapper.selectFirstX(queryWrapper);
    }

    public CcmdPage<Department> selectPage(CcmdPageParam pageParam, LambdaQueryWrapper<Department> queryWrapper) {
        return departmentMapper.selectPageX(pageParam, queryWrapper);
    }

    public List<Department> selectAll(LambdaQueryWrapper<Department> queryWrapper) {
        return departmentMapper.selectList(queryWrapper);
    }

    public List<Department> selectList(LambdaQueryWrapper<Department> queryWrapper) {
        return departmentMapper.selectList(queryWrapper);
    }

    @Async
    public void handleWsNotify(JsonNode jsonNode) {
        var mo = Optional.ofNullable(jsonNode.get("mo")).map(JsonNode::asText).orElse("");
        if (!Objects.equals(mo, "47")) {
            return;
        }
        var notify = JsonUtil.convert(jsonNode, new TypeReference<Notify<DepartmentNotify>>() {
        });
        var departmentId =
                Optional.ofNullable(notify).map(Notify::getValue).map(DepartmentNotify::getDepartmentid).orElse(null);
        if (departmentId == null) {
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
                var department = BeanCopyUtils.copyBean(notify.getValue(), Department::new);
                department.setId(idWorker.nextId());
                department.setCreateTime(new Date());
                var upperdepartmentId = notify.getValue().getUpperdepartmentId();
                if (upperdepartmentId != null && !Objects.equals(upperdepartmentId, "0")) {
                    var uper = departmentMapper.selectFirstX(
                            Wrappers.lambdaQuery(Department.class).eq(Department::getDepartmentid, upperdepartmentId));
                    if (uper == null) {
                        log.warn("no parent found for:{}", notify);
                        return;
                    }
                    department.setDepartmentIdPath(uper.getDepartmentIdPath() + ":" + department.getDepartmentid());
                    department.setDepartmentNamePath(
                            uper.getDepartmentNamePath() + ":" + department.getDepartmentname());
                } else {
                    department.setDepartmentIdPath(department.getDepartmentid());
                    department.setDepartmentNamePath(department.getDepartmentname());
                }
                departmentMapper.insert(department);
                publishCameraLevelMsg("CREATE", department);
                break;
            }
            case "delete": {
                departmentMapper.delete(Department::getDepartmentid, departmentId);
                publishCameraLevelMsg("DELETE", departmentId);
                break;
            }
            default: {
                log.warn("meet strange notify:{}", JsonUtil.toJsonStr(jsonNode));
            }
        }
    }

    private void publishCameraLevelMsg(String notifyType, Object payload) {
        var build = new CagentMqFrame().toBuilder().typeSubSystemMessage("ICP_DEPARTMENT").multicast()
                .appKeys(List.of("CDC-1000", "CAPP-1000", "CAPP-3000")).build().body("ICP_DEPARTMENT", notifyType, payload)
                .build();
        streamBridge.send("cloudcmd-cagent", build);
        //        log.info("send cagent msg:{} done", build);
    }

    private List<DepartmentResp> findSubDepartment(List<DepartmentResp> allDepartment, String departmentId) {
        if (StringUtils.isBlank(departmentId)) {
            return allDepartment;
        }

        Map<String, List<DepartmentResp>> upperToSubordinates = new HashMap<>();
        for (DepartmentResp dept : allDepartment) {
            String upperId = dept.getUpperdepartmentId();
            upperToSubordinates.computeIfAbsent(upperId, k -> new ArrayList<>()).add(dept);
        }

        List<DepartmentResp> result = new ArrayList<>();
        Queue<DepartmentResp> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // 先把当前部门加入结果集
        allDepartment.stream()
                .filter(dept -> dept.getDepartmentid().equals(departmentId))
                .findFirst()
                .ifPresent(result::add);

        List<DepartmentResp> directSubordinates = upperToSubordinates.getOrDefault(departmentId, Collections.emptyList());
        for (DepartmentResp dept : directSubordinates) {
            if (!visited.contains(dept.getDepartmentid())) {
                queue.add(dept);
                visited.add(dept.getDepartmentid());
            }
        }

        while (!queue.isEmpty()) {
            DepartmentResp current = queue.poll();
            result.add(current);

            List<DepartmentResp> subSubordinates = upperToSubordinates.getOrDefault(current.getDepartmentid(), Collections.emptyList());
            for (DepartmentResp sub : subSubordinates) {
                if (!visited.contains(sub.getDepartmentid())) {
                    queue.add(sub);
                    visited.add(sub.getDepartmentid());
                }
            }
        }
        return result;
    }
}
