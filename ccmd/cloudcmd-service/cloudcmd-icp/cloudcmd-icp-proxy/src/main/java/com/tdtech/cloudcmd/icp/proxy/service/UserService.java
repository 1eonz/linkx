package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.icp.proxy.client.IcpHttpClient;
import com.tdtech.cloudcmd.icp.proxy.client.entity.UserResp;
import com.tdtech.cloudcmd.icp.proxy.client.entity.ResponseObject;
import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.icp.proxy.entity.User;
import com.tdtech.cloudcmd.icp.proxy.repo.UserMapper;
import com.tdtech.cloudcmd.icp.proxy.util.SeriaExecutor;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.Notify;
import com.tdtech.cloudcmd.icp.proxy.ws.entity.UserNotify;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String PULL_LOCK_KEY = "cloudcmd:icp:user:pulllock";

    private static final int PAGE_SIZE = 200;

    private final UserMapper userMapper;
    private final IcpHttpClient icpHttpClient;
    private final AuthService authService;
    private final IdWorker idWorker;
    private final StreamBridge streamBridge;
    private final GisService gisService;
    private final OnlineStatusService onlineStatusService;
    private final SeriaExecutor seriaExecutor;
    private final DepartmentService departmentService;
    private final ImUserService imUserService;
    private final IcpPrivService icpPrivService;
    private final IsdnTypeService isdnTypeService;

    public void tryInitData() {
        try {
            log.info("Pulling user data from ICP...");
            pull();
        } catch (RuntimeException e) {
            log.error("pull data error", e);
        }
    }

    public void pull() {
        seriaExecutor.run(PULL_LOCK_KEY, () -> {
            var now = new Date(System.currentTimeMillis() / 1000L * 1000L);//处理MYSQL精度问题
            List<Department> departmentList = departmentService.selectAll(new LambdaQueryWrapper<>());

            var firstResp = listPagedRaw(null, 0, PAGE_SIZE);
            Integer totalNum = firstResp.getTotalNum();
            if (totalNum == null || totalNum == 0) {
                log.info("用户数据为空，无需同步");
                return;
            }

            int totalPages = (int) Math.ceil((double) totalNum / PAGE_SIZE);
            log.info("ICP服务器中用户总数: {}, 预计查询 {} 页", totalNum, totalPages);

            // 收集所有远程 isdn（用于后续删除判断）
            Set<String> remoteIsdnSet = new HashSet<>();
            List<User> toInsertList = new ArrayList<>();

            // 处理第一页数据
            processRemoteUsers(firstResp.getList(), departmentList, remoteIsdnSet, toInsertList, now);

            // 继续查询剩余页
            for (int page = 1; page < totalPages; page++) {
                int offset = page * PAGE_SIZE;
                var resp = listPagedRaw(null, offset, PAGE_SIZE);
                processRemoteUsers(resp.getList(), departmentList, remoteIsdnSet, toInsertList, now);

                //访问慢点 免得SDKServer爆炸
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    log.error("interrupted", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            log.info("远程用户数量: {}, 需要新增: {}", remoteIsdnSet.size(), toInsertList.size());

            boolean hasChange = false;

            // 批量插入或更新（使用insertOrUpdateBatch处理逻辑删除后重新插入的场景）
            // 注意：此处直接调用 userMapper.insertOrUpdateBatch，不再触发批内 IsdnType 增量同步；
            // IsdnType 全量同步统一在 pull() 末尾通过 selectDistinctIsdnType() 完成。
            if (!toInsertList.isEmpty()) {
                int batchSize = 500;
                for (int i = 0; i < toInsertList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, toInsertList.size());
                    List<User> batch = toInsertList.subList(i, end);
                    userMapper.insertOrUpdateBatch(batch);
                }
                hasChange = true;
                log.info("User sync: inserted {} records", toInsertList.size());
            }

            // 删除：分批查询本地数据，逐批判断是否需要删除
            long deletedCount = deleteNotInRemote(remoteIsdnSet);
            if (deletedCount > 0) {
                hasChange = true;
            }
            // tb_isdn 全量同步完成后，统一基于去重结果同步 IsdnType
            List<IsdnType> userIsdnTypes = selectDistinctIsdnType();
            if (!userIsdnTypes.isEmpty()) {
                log.info("开始全量同步 tb_isdn 对应的 IsdnType，数量: {}", userIsdnTypes.size());
                isdnTypeService.syncIsdnType(userIsdnTypes);
            }
            if (hasChange) {
                // 订阅在线状态和位置（仅订阅新增的用户）
                if (!toInsertList.isEmpty()) {
                    List<String> newIsdns = toInsertList.stream()
                            .map(User::getIsdn)
                            .filter(isdn -> isdn != null && !isdn.isBlank())
                            .collect(Collectors.toList());
                    gisService.doSub(newIsdns);
                    onlineStatusService.doSub(newIsdns);
                    syncImUsersByIsdn(newIsdns);
                    icpPrivService.setDefault();
                }

                log.info("用户数据同步完成，ICP服务器中用户总数：{}，新增 {}，删除 {}", totalNum, toInsertList.size(), deletedCount);

                // 修复：在tb_isdn数据拉取完成后，强制重新初始化IM用户和权限
                // 解决回滚后升级导致的数据不一致问题
                log.info("开始重新同步IM用户数据并设置默认权限");
                try {
                    imUserService.refreshImUser();
                } catch (Exception e) {
                    log.error("刷新IM用户数据失败", e);
                }
            } else {
                log.info("User sync: no changes, skipped");
            }
        });
    }

    private void insertOrUpdateBatch(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        userMapper.insertOrUpdateBatch(users);
        Set<String> seenKeys = ConcurrentHashMap.newKeySet();
        List<IsdnType> newIsdnTypes = users.stream()
                .filter(user -> seenKeys.add(isdnTypeService.buildIsdnTypeKey(user.getCategory(), user.getSubusercategory(), user.getApptype())))
                .map(user -> BeanCopyUtils.copyBean(user, IsdnType::new))
                .collect(Collectors.toList());
        
        if (!newIsdnTypes.isEmpty()) {
            log.info("user同步过来的IsdnType，数量: {}", newIsdnTypes.size());
            isdnTypeService.syncIsdnType(newIsdnTypes);
        }
    }

    private void processRemoteUsers(List<UserResp> remoteUsers, List<Department> departmentList,
                                    Set<String> remoteIsdnSet, List<User> toInsertList, Date now) {
        if (remoteUsers == null || remoteUsers.isEmpty()) {
            return;
        }

        // 过滤掉不在部门列表中的用户
        List<User> filteredUsers = filterUserBydepartmentList(remoteUsers, departmentList);
        if (filteredUsers.isEmpty()) {
            return;
        }

        // 提取当前批次的 isdn
        List<String> batchIsdns = filteredUsers.stream()
                .map(User::getIsdn)
                .filter(isdn -> isdn != null && !isdn.isBlank())
                .collect(Collectors.toList());

        // 查询当前批次在本地存在的 isdn（包含逻辑删除的记录）
        // 用于处理逻辑删除后重新插入的场景
        Map<String, User> existingIsdnMap = new HashMap<>();
        if (!batchIsdns.isEmpty()) {
            List<User> existingUsers = userMapper.selectByIsdnListIncludeDeleted(batchIsdns);
            existingIsdnMap = existingUsers.stream()
                    .collect(Collectors.toMap(User::getIsdn, u -> u, (a, b) -> a));
        }

        // 判断哪些需要新增或更新
        for (User user : filteredUsers) {
            String isdn = user.getIsdn();
            if (isdn == null || isdn.isBlank()) {
                continue;
            }

            remoteIsdnSet.add(isdn);
            User existing = existingIsdnMap.get(isdn);
            if (existing == null) {
                // 本地不存在，新增
                user.setId(idWorker.nextId());
                user.setCreateTime(now);
                user.setStatus(0);
                toInsertList.add(user);
            } else {
                // 本地存在，更新（保留原ID，如果已逻辑删除则恢复）
                user.setId(existing.getId());
                user.setCreateTime(now);
                user.setStatus(0); // 确保状态为可用（恢复逻辑删除的记录）
                if (existing.getStatus() != 0) {
                    toInsertList.add(user);
                    log.info("恢复逻辑删除的用户: isdn={}", isdn);
                }
            }
        }
    }

    private long deleteNotInRemote(Set<String> remoteIsdnSet) {
        long batchSize = 1000;
        long totalDeleted = 0;

        for (long pageNum = 1L; ; pageNum++) {
            // 分页查询本地数据
            var localPage = userMapper.selectPageX(
                    new CcmdPageParam(pageNum, batchSize),
                    Wrappers.lambdaQuery(User.class).select(User::getIsdn)
            );

            if (localPage.isEmpty()) {
                break;
            }

            // 筛选出需要删除的（本地有，远程没有）
            List<String> toDelete = localPage.getRecords().stream()
                    .map(User::getIsdn)
                    .filter(isdn -> isdn != null && !remoteIsdnSet.contains(isdn))
                    .collect(Collectors.toList());

            if (!toDelete.isEmpty()) {
                userMapper.delete(Wrappers.lambdaQuery(User.class).in(User::getIsdn, toDelete));
                totalDeleted += toDelete.size();
            }
        }

        if (totalDeleted > 0) {
            log.info("User sync: deleted {} records", totalDeleted);
        }
        return totalDeleted;
    }

    private List<User> processAndSaveUsers(List<UserResp> rawUsers, List<Department> departmentList, Date now) {
        if (rawUsers == null || rawUsers.isEmpty()) {
            return Collections.emptyList();
        }

        var users = filterUserBydepartmentList(rawUsers, departmentList);
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询数据库中已存在的记录，用于保留ID
        Map<String, Long> existingIdMap = getExistingIdMap(users);
        List<String> newIsdns = users.stream()
                .map(User::getIsdn)
                .filter(isdn -> isdn != null && !isdn.isBlank())
                .filter(isdn -> !existingIdMap.containsKey(isdn))
                .distinct()
                .collect(Collectors.toList());

        for (var user : users) {
            // 如果数据库中已存在相同isdn的记录，保留原ID，否则生成新ID
            Long existingId = existingIdMap.get(user.getIsdn());
            user.setId(existingId != null ? existingId : idWorker.nextId());
            user.setCreateTime(now);
        }
        // 批量新增或更新
        userMapper.insertOrUpdateBatch(users);

        List<String> isdns = users.stream()
                .map(User::getIsdn)
                .filter(isdn -> isdn != null && !isdn.isBlank())
                .collect(Collectors.toList());
        gisService.doSub(isdns);
        onlineStatusService.doSub(isdns);
        if (!newIsdns.isEmpty()) {
            syncImUsersByIsdn(newIsdns);
            icpPrivService.setDefault();
        }

        return users;
    }

    private Map<String, Long> getExistingIdMap(List<User> users) {
        Map<String, Long> existingIdMap = new HashMap<>();
        List<String> isdns = users.stream()
                .map(User::getIsdn)
                .filter(isdn -> isdn != null && !isdn.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (!isdns.isEmpty()) {
            List<User> existingRecords = userMapper.selectList(
                    Wrappers.lambdaQuery(User.class)
                            .in(User::getIsdn, isdns)
                            .select(User::getId, User::getIsdn)
            );
            existingRecords.forEach(record ->
                    existingIdMap.put(record.getIsdn(), record.getId())
            );
        }
        return existingIdMap;
    }

    private ResponseObject<UserResp> listPagedRaw(String departmentId, @NotNull Integer offset, @NotNull Integer limit) {
        return icpHttpClient.getUserByDepartment(authService.getSessionStr(), departmentId, offset, limit, null);
    }

    /**
     * 分页查询用户
     *
     * @param pageParam    分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public CcmdPage<User> selectPage(CcmdPageParam pageParam, LambdaQueryWrapper<User> queryWrapper) {
        return userMapper.selectPageX(pageParam, queryWrapper);
    }

    public User selectOne(LambdaQueryWrapper<User> queryWrapper) {
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 分页查询用户
     *
     * @param pageParam    分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public <R> CcmdPage<R> selectPage(CcmdPageParam pageParam, Class<R> cls, MPJLambdaWrapper<User> queryWrapper) {
        return userMapper.selectJoinPage(pageParam, cls, queryWrapper);
    }

    @Async
    public void handleWsNotify(JsonNode jsonNode) {
        var mo = Optional.ofNullable(jsonNode.get("mo")).map(JsonNode::asText).orElse("");
        if (!Objects.equals(mo, "0") && !Objects.equals(mo, "2")) {
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
            case "modify":
            case "create": {
                var userByIsdn = icpHttpClient.getUserByIsdn(authService.getSessionStr(), isdn);
                var user = BeanCopyUtils.copyBean(userByIsdn, User::new);
                if (user.getIsdn() == null || user.getIsdn().isBlank()) {
                    log.warn("invalid user:{}", JsonUtil.toJsonStr(jsonNode));
                    break;
                }
                // 准备用户数据（处理ID和逻辑删除恢复）
                prepareUserForInsertOrUpdate(user);
                // 使用insertOrUpdateBatch处理新增或恢复逻辑删除的记录
                insertOrUpdateBatch(Collections.singletonList(user));
                
                onlineStatusService.doSub(List.of(isdn));
                gisService.doSub(List.of(isdn));
                imUserService.syncImUserByIsdn(isdn);
                icpPrivService.setDefault();
                publishCameraLevelMsg("UPSERT", user);
                break;
            }
            case "delete": {
                userMapper.delete(User::getIsdn, isdn);
                publishCameraLevelMsg("ISDN", isdn);
                break;
            }
            default: {
                log.warn("meet strange notify:{}", JsonUtil.toJsonStr(jsonNode));
            }
        }
    }

    private void publishCameraLevelMsg(String notifyType, Object payload) {
        var build = new CagentMqFrame().toBuilder().typeSubSystemMessage("ICP_USER").multicast()
                .appKeys(List.of("CDC-1000", "CAPP-1000", "CAPP-3000")).build().body("ICP_USER", notifyType, payload)
                .build();
        streamBridge.send("cloudcmd-cagent", build);
        //        log.info("send cagent msg:{} done", build);
    }

    private List<User> filterUserBydepartmentList(List<UserResp> userByDepartment, List<Department> departmentList) {
        if (departmentList == null || departmentList.isEmpty()) {
            return BeanCopyUtils.copyList(userByDepartment, User::new);
        }
        Set<String> deptSet = departmentList.stream()
                .map(Department::getDepartmentid)
                .collect(Collectors.toSet());

        List<UserResp> filtedUserResp = userByDepartment.stream()
                .filter(userResp -> deptSet.contains(userResp.getDepartmentid()))
                .collect(Collectors.toList());
        return BeanCopyUtils.copyList(filtedUserResp, User::new);
    }

    public User selectById(Long id) {
        return userMapper.selectById(id);
    }

    private void syncImUsersByIsdn(List<String> isdns) {
        if (isdns == null || isdns.isEmpty()) {
            return;
        }
        for (String isdn : isdns) {
            try {
                imUserService.syncImUserByIsdn(isdn);
            } catch (RuntimeException e) {
                log.warn("sync IM user by isdn {} failed", isdn, e);
            }
        }
    }

    /**
     * 准备用户数据用于插入或更新
     * 处理逻辑删除后重新插入的场景：
     * - 如果本地不存在，生成新ID
     * - 如果本地存在（包括逻辑删除），保留原ID
     * 
     * @param user 用户数据
     */
    private void prepareUserForInsertOrUpdate(User user) {
        if (user == null || user.getIsdn() == null || user.getIsdn().isBlank()) {
            return;
        }
        
        // 查询是否存在（包含逻辑删除的记录）
        User existing = userMapper.selectByIsdnIncludeDeleted(user.getIsdn());
        if (existing != null) {
            // 已存在，保留原ID（modify场景或恢复逻辑删除的记录）
            user.setId(existing.getId());
        } else {
            // 不存在，生成新ID（create场景）
            user.setId(idWorker.nextId());
        }
        user.setCreateTime(new Date());
        user.setStatus(0); // 确保状态为可用（处理逻辑删除后重新插入的场景）
    }

    public List<IsdnType> selectDistinctIsdnType() {
        List<IsdnType> distinctList = userMapper.selectDistinctKey();
        if (distinctList == null || distinctList.isEmpty()) {
            return Collections.emptyList();
        }
        return distinctList;
    }
}