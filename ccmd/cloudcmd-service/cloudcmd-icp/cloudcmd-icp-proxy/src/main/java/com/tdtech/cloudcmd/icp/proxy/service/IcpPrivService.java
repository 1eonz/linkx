package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.service.IIMUserRPCService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.BatchUserPrivQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DeptPrivQO;
import com.tdtech.cloudcmd.icp.proxy.entity.CameraLevel;
import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import com.tdtech.cloudcmd.icp.proxy.entity.DeptIcpCameraPriv;
import com.tdtech.cloudcmd.icp.proxy.entity.DeptIcpUserPriv;
import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import com.tdtech.cloudcmd.icp.proxy.entity.IcpImUser;
import com.tdtech.cloudcmd.icp.proxy.entity.ImUserIcpCameraPriv;
import com.tdtech.cloudcmd.icp.proxy.repo.CameraLevelMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.DepartmentMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.DeptIcpCameraPrivMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.DeptIcpUserPrivMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.DepartmentMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.ImUserIcpCameraPrivMapper;
import com.tdtech.cloudcmd.icp.proxy.entity.ImUserIcpUserPriv;
import com.tdtech.cloudcmd.icp.proxy.repo.ImUserIcpUserPrivMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.IcpImUserMapper;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IcpPrivService {
    private static final String ICP_USER_PRIV_CACHE_PREFIX = "cloudcmd:icp:priv:user:";
    private static final String ICP_CAMERA_PRIV_CACHE_PREFIX = "cloudcmd:icp:priv:camera:";

    /**
     * 批量处理用户时的用户分批大小
     */
    private static final int USER_BATCH_SIZE = 200;
    /**
     * 批量处理权限时的权限分批大小
     */
    private static final int PRIV_BATCH_SIZE = 50;

    private final RedisUtil redisUtil;
    private final ImUserIcpUserPrivMapper userPrivMapper;
    private final ImUserIcpCameraPrivMapper cameraPrivMapper;
    private final DeptIcpCameraPrivMapper deptCameraPrivMapper;
    private final DeptIcpUserPrivMapper deptUserPrivMapper;
    private final IcpImUserMapper icpImUserMapper;
    private final IdWorker idWorker;
    private final ReportUtil reportUtil;
    private final DepartmentMapper departmentMapper;
    private final CameraLevelMapper cameraLevelMapper;
    private final ThreadPoolTaskExecutor asyncMessageTaskExecutor;

    @DubboReference
    private IIMUserRPCService imUserRPCService;

    @DubboReference
    private DepartmentRpcApi departmentRpcApi;

    public void upsertUserPriv(@NotNull Long imUserId, @NotNull List<String> priv) {
        var olds = userPrivMapper.selectListX(ImUserIcpUserPriv::getImUserId, imUserId);
        userPrivMapper.delete(ImUserIcpUserPriv::getImUserId, imUserId);
        if (olds != null && !olds.isEmpty()) {
            Map<String, Long> keyMemberMap = olds.stream()
                .collect(Collectors.toMap(
                    old -> ICP_USER_PRIV_CACHE_PREFIX + old.getDepartmentid(),
                    ImUserIcpUserPriv::getImUserId,
                    (v1, v2) -> v1
                ));
            redisUtil.sRemMultiKeys(keyMemberMap);
        }
        if (priv == null || priv.isEmpty()) {
            reportOperationLog(imUserId, priv, true);
            return;
        }
        var collect =
            priv.stream().map(p -> new ImUserIcpUserPriv(idWorker.nextId(), imUserId, p)).collect(Collectors.toList());
        userPrivMapper.insertBatch(collect);

        Map<String, Long> addKeyMemberMap = priv.stream()
            .collect(Collectors.toMap(
                p -> ICP_USER_PRIV_CACHE_PREFIX + p,
                p -> imUserId,
                (v1, v2) -> v1
            ));
        redisUtil.sAddMultiKeys(addKeyMemberMap);

        reportOperationLog(imUserId, priv, true);
    }

    public void upsertDeptUserPriv(DeptPrivQO deptPrivQO) {
        if (deptPrivQO == null) {
            log.warn("DeptPrivQO is null, skip authorization");
            return;
        }

        List<String> deptCodes = deptPrivQO.getDeptCodes();
        List<String> privs = deptPrivQO.getPrivs();
        Integer isChildren = deptPrivQO.getIsChildren();

        if (deptCodes == null || deptCodes.isEmpty()) {
            log.warn("DeptCodes is empty, skip authorization");
            return;
        }

        List<String> effectiveDeptCodes = getEffectiveDeptCodes(deptCodes, isChildren);

        saveDeptUserPrivConfig(deptCodes, privs, isChildren);

        if (effectiveDeptCodes != null && !effectiveDeptCodes.isEmpty()) {
            List<ImUserDto> imUserDtos = imUserRPCService.listByDeptCodes(effectiveDeptCodes);
            if (imUserDtos != null && !imUserDtos.isEmpty()) {
                List<Long> userIds = imUserDtos.stream()
                    .map(ImUserDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                if (!userIds.isEmpty()) {
                    asyncMessageTaskExecutor.execute(() -> processUserPrivBatch(userIds, privs));
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void upsertUserPrivBatch(BatchUserPrivQO batchUserPrivQO) {
        if (batchUserPrivQO == null) {
            log.warn("BatchUserPrivQO is null, skip authorization");
            reportUtil.saveOperationLog(OperationTypeEnum.EQUIPMENT_BATCH_AUTH_UPDATE, "授权参数为空，跳过授权");
            return;
        }
        
        List<Long> userIds = batchUserPrivQO.getUserIds();
        List<String> privs = batchUserPrivQO.getPrivs();
        
        if (userIds == null || userIds.isEmpty()) {
            log.warn("UserIds is empty, skip authorization");
            reportUtil.saveOperationLog(OperationTypeEnum.EQUIPMENT_BATCH_AUTH_UPDATE, "用户列表为空，跳过授权");
            return;
        }

        List<Long> validUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (validUserIds.isEmpty()) {
            log.warn("No valid user IDs found");
            reportUtil.saveOperationLog(OperationTypeEnum.EQUIPMENT_BATCH_AUTH_UPDATE, "无有效用户ID，跳过授权");
            return;
        }

        processUserPrivBatch(validUserIds, privs);

        OperationLog operationLog = new OperationLog(OperationTypeEnum.EQUIPMENT_BATCH_AUTH_UPDATE);
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }

        List<String> privNames = getPrivNames(privs, true);
        List<IcpImUser> icpImUsers = icpImUserMapper.selectList(new LambdaQueryWrapper<>(IcpImUser.class).in(IcpImUser::getId, validUserIds));
        List<String> userNames = icpImUsers.stream().map(IcpImUser::getName).collect(Collectors.toList());
        String privContent = (privNames == null || privNames.isEmpty()) ? "权限已清空" : privNames.toString();
        String userContent = userNames.toString();
        operationLog.setOperation(String.format(operationLog.getOperation(), userContent, privContent));
        reportUtil.saveOperationLog(operationLog);

    }

    public void upsertCameraPriv(@NotNull Long imUserId, @NotNull List<String> priv) {
        var olds = cameraPrivMapper.selectListX(ImUserIcpCameraPriv::getImUserId, imUserId);
        cameraPrivMapper.delete(ImUserIcpCameraPriv::getImUserId, imUserId);
        if (olds != null && !olds.isEmpty()) {
            Map<String, Long> keyMemberMap = olds.stream()
                .collect(Collectors.toMap(
                    old -> ICP_CAMERA_PRIV_CACHE_PREFIX + old.getCameraLevel(),
                    ImUserIcpCameraPriv::getImUserId,
                    (v1, v2) -> v1  // 如果有重复key，保留第一个
                ));
            redisUtil.sRemMultiKeys(keyMemberMap);
        }
        if (priv == null || priv.isEmpty()) {
            reportOperationLog(imUserId, priv, true);
            return;
        }
        var collect = priv.stream().map(p -> new ImUserIcpCameraPriv(idWorker.nextId(), imUserId, p))
            .collect(Collectors.toList());
        cameraPrivMapper.insertBatch(collect);

        Map<String, Long> addKeyMemberMap = priv.stream()
            .collect(Collectors.toMap(
                p -> ICP_CAMERA_PRIV_CACHE_PREFIX + p,
                p -> imUserId,
                (v1, v2) -> v1
            ));
        redisUtil.sAddMultiKeys(addKeyMemberMap);

        reportOperationLog(imUserId, priv, false);
    }

    public void upsertDeptCameraPriv(DeptPrivQO deptCameraPrivQO) {
        if (deptCameraPrivQO == null) {
            reportUtil.saveOperationLog(OperationTypeEnum.EQUIPMENT_DEPT_AUTH_UPDATE, "部门权限参数为空，跳过授权");
            log.warn("DeptCameraPrivQO is null, skip authorization");
            return;
        }

        List<String> deptCodes = deptCameraPrivQO.getDeptCodes();
        List<String> privs = deptCameraPrivQO.getPrivs();
        Integer isChildren = deptCameraPrivQO.getIsChildren();

        if (deptCodes == null || deptCodes.isEmpty()) {
            reportUtil.saveOperationLog(OperationTypeEnum.EQUIPMENT_DEPT_AUTH_UPDATE, "部门编码为空，跳过授权");
            log.warn("DeptCodes is empty, skip authorization");
            return;
        }

        List<String> effectiveDeptCodes = getEffectiveDeptCodes(deptCodes, isChildren);

        saveDeptCameraPrivConfig(deptCodes, privs, isChildren);

        if (effectiveDeptCodes != null && !effectiveDeptCodes.isEmpty()) {
            List<ImUserDto> imUserDtos = imUserRPCService.listByDeptCodes(effectiveDeptCodes);
            if (imUserDtos != null && !imUserDtos.isEmpty()) {
                List<Long> userIds = imUserDtos.stream()
                    .map(ImUserDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                if (!userIds.isEmpty()) {
                    asyncMessageTaskExecutor.execute(() -> processCameraPrivBatch(userIds, privs));
                }
            }
        }
    }

    private List<String> getEffectiveDeptCodes(List<String> deptCodes, Integer isChildren) {
        if (isChildren == null || isChildren == 0) {
            return deptCodes;
        }

        List<String> allDeptCodes = new ArrayList<>();
        for (String deptCode : deptCodes) {
            List<OrganizationVO> orgList = departmentRpcApi.queryDepartmentForList(deptCode);
            if (orgList != null && !orgList.isEmpty()) {
                allDeptCodes.addAll(orgList.stream()
                    .map(OrganizationVO::getCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            }
        }

        return allDeptCodes.stream().distinct().collect(Collectors.toList());
    }


    @Transactional(rollbackFor = Exception.class)
    public void saveDeptCameraPrivConfig(List<String> deptCodes, List<String> privs, Integer isChildren) {
        Date now = new Date();

        deptCameraPrivMapper.deleteByDeptCodes(deptCodes);

        if (privs != null && !privs.isEmpty()) {
            List<DeptIcpCameraPriv> deptPrivList = new ArrayList<>();
            for (String deptCode : deptCodes) {
                for (String priv : privs) {
                    DeptIcpCameraPriv deptPriv = new DeptIcpCameraPriv();
                    deptPriv.setId(idWorker.nextId());
                    deptPriv.setDeptCode(deptCode);
                    deptPriv.setCameraLevel(priv);
                    deptPriv.setGmtCreated(now);
                    deptPriv.setGmtModified(now);
                    deptPrivList.add(deptPriv);
                }
            }

            int batchSize = 100;
            for (int i = 0; i < deptPrivList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, deptPrivList.size());
                List<DeptIcpCameraPriv> batch = deptPrivList.subList(i, endIndex);
                deptCameraPrivMapper.insertBatch(batch);
            }
        }

        log.info("Saved department camera priv config, deptCodes: {}, privs: {}, isChildren: {}", deptCodes, privs, isChildren);

        asyncMessageTaskExecutor.execute(() -> reportDeptCameraPrivOperationLog(deptCodes, privs, isChildren));
    }

    private void reportDeptCameraPrivOperationLog(List<String> deptCodes, List<String> privs, Integer isChildren) {
        OperationLog operationLog = new OperationLog(OperationTypeEnum.CAMERA_DEPT_AUTH_UPDATE);

        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }

        List<String> deptNames = new ArrayList<>();
        for (String deptCode : deptCodes) {
            try {
                List<OrganizationVO> orgList = departmentRpcApi.queryDepartmentForList(deptCode);
                if (orgList != null && !orgList.isEmpty()) {
                    orgList.stream()
                        .filter(org -> deptCode.equals(org.getCode()))
                        .map(OrganizationVO::getName)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(deptNames::add);
                }
            } catch (Exception e) {
                log.warn("reportDeptCameraPrivOperationLog: query department name failed for code={}", deptCode, e);
            }
        }

        List<String> privNames = getPrivNames(privs, false);

        String deptContent = deptNames.isEmpty() ? deptCodes.toString() : deptNames.toString();
        String privContent = (privNames == null || privNames.isEmpty()) ? "权限已清空" : privNames.toString();
        String childrenNote = (isChildren != null && isChildren == 1) ? "（含子部门）" : "";

        operationLog.setOperation(String.format("部门%s%s摄像头权限已更新，授权范围：%s", deptContent, childrenNote, privContent));
        reportUtil.saveOperationLog(operationLog);
    }

    public List<String> getDeptCameraPriv(String deptCode) {
        if (deptCode == null || deptCode.isEmpty()) {
            return Collections.emptyList();
        }

        List<DeptIcpCameraPriv> deptPrivs = deptCameraPrivMapper.selectByDeptCode(deptCode);
        if (deptPrivs == null || deptPrivs.isEmpty()) {
            return Collections.emptyList();
        }

        return deptPrivs.stream()
            .map(DeptIcpCameraPriv::getCameraLevel)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDeptUserPrivConfig(List<String> deptCodes, List<String> privs, Integer isChildren) {
        Date now = new Date();

        deptUserPrivMapper.deleteByDeptCodes(deptCodes);

        if (privs != null && !privs.isEmpty()) {
            List<DeptIcpUserPriv> deptPrivList = new ArrayList<>();
            for (String deptCode : deptCodes) {
                for (String priv : privs) {
                    DeptIcpUserPriv deptPriv = new DeptIcpUserPriv();
                    deptPriv.setId(idWorker.nextId());
                    deptPriv.setDeptCode(deptCode);
                    deptPriv.setDepartmentId(priv);
                    deptPriv.setGmtCreated(now);
                    deptPriv.setGmtModified(now);
                    deptPrivList.add(deptPriv);
                }
            }

            int batchSize = 100;
            for (int i = 0; i < deptPrivList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, deptPrivList.size());
                List<DeptIcpUserPriv> batch = deptPrivList.subList(i, endIndex);
                deptUserPrivMapper.insertBatch(batch);
            }
        }

        log.info("Saved department user priv config, deptCodes: {}, privs: {}, isChildren: {}", deptCodes, privs, isChildren);

        asyncMessageTaskExecutor.execute(() -> reportDeptUserPrivOperationLog(deptCodes, privs, isChildren));
    }

    private void reportDeptUserPrivOperationLog(List<String> deptCodes, List<String> privs, Integer isChildren) {
        OperationLog operationLog = new OperationLog(OperationTypeEnum.EQUIPMENT_DEPT_AUTH_UPDATE);

        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }

        List<String> deptNames = new ArrayList<>();
        for (String deptCode : deptCodes) {
            try {
                List<OrganizationVO> orgList = departmentRpcApi.queryDepartmentForList(deptCode);
                if (orgList != null && !orgList.isEmpty()) {
                    orgList.stream()
                        .filter(org -> deptCode.equals(org.getCode()))
                        .map(OrganizationVO::getName)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(deptNames::add);
                }
            } catch (Exception e) {
                log.warn("reportDeptUserPrivOperationLog: query department name failed for code={}", deptCode, e);
            }
        }

        List<String> privNames = getPrivNames(privs, true);

        String deptContent = deptNames.isEmpty() ? deptCodes.toString() : deptNames.toString();
        String privContent = (privNames == null || privNames.isEmpty()) ? "权限已清空" : privNames.toString();
        String childrenNote = (isChildren != null && isChildren == 1) ? "（含子部门）" : "";

        operationLog.setOperation(String.format("部门%s%s设备权限已更新，授权范围：%s", deptContent, childrenNote, privContent));
        reportUtil.saveOperationLog(operationLog);
    }

    public List<String> getDeptUserPriv(String deptCode) {
        if (deptCode == null || deptCode.isEmpty()) {
            return Collections.emptyList();
        }

        List<DeptIcpUserPriv> deptPrivs = deptUserPrivMapper.selectByDeptCode(deptCode);
        if (deptPrivs == null || deptPrivs.isEmpty()) {
            return Collections.emptyList();
        }

        return deptPrivs.stream()
            .map(DeptIcpUserPriv::getDepartmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void upsertUserCameraPrivBatch(BatchUserPrivQO userCameraPrivQO) {
        if (userCameraPrivQO == null) {
            log.warn("UserCameraPrivQO is null, skip authorization");
            reportUtil.saveOperationLog(OperationTypeEnum.CAMERA_BATCH_AUTH_UPDATE, "授权参数为空，跳过授权");
            return;
        }
        
        List<Long> userIds = userCameraPrivQO.getUserIds();
        List<String> privs = userCameraPrivQO.getPrivs();
        
        if (userIds == null || userIds.isEmpty()) {
            log.warn("UserIds is empty, skip authorization");
            reportUtil.saveOperationLog(OperationTypeEnum.CAMERA_BATCH_AUTH_UPDATE, "用户列表为空，跳过授权");
            return;
        }

        List<Long> validUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (validUserIds.isEmpty()) {
            log.warn("No valid user IDs found");
            reportUtil.saveOperationLog(OperationTypeEnum.CAMERA_BATCH_AUTH_UPDATE, "无有效用户ID，跳过授权：" + userIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            return;
        }

        processCameraPrivBatch(validUserIds, privs);

        OperationLog operationLog = new OperationLog(OperationTypeEnum.CAMERA_BATCH_AUTH_UPDATE);
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }

        List<String> privNames = getPrivNames(privs, false);
        List<IcpImUser> icpImUsers = icpImUserMapper.selectList(new LambdaQueryWrapper<>(IcpImUser.class).in(IcpImUser::getId, validUserIds));
        List<String> userNames = icpImUsers.stream().map(IcpImUser::getName).collect(Collectors.toList());
        String privContent = (privNames == null || privNames.isEmpty()) ? "权限已清空" : privNames.toString();
        String userContent = userNames.toString();
        operationLog.setOperation(String.format(operationLog.getOperation(), userContent, privContent));
        reportUtil.saveOperationLog(operationLog);

    }

    public Collection<String> getUserPrivByUser(Long userId) {
        var imUserIcpPrivs = userPrivMapper.selectListX(ImUserIcpUserPriv::getImUserId, userId);
        if (imUserIcpPrivs == null || imUserIcpPrivs.isEmpty()) {
            return Collections.emptyList();
        }
        return imUserIcpPrivs.stream().map(ImUserIcpUserPriv::getDepartmentid).collect(Collectors.toList());
    }


    public Collection<String> getCameraPrivByUser(Long userId) {
        var imUserIcpPrivs = cameraPrivMapper.selectListX(ImUserIcpCameraPriv::getImUserId, userId);
        if (imUserIcpPrivs == null || imUserIcpPrivs.isEmpty()) {
            return Collections.emptyList();
        }
        return imUserIcpPrivs.stream().map(ImUserIcpCameraPriv::getCameraLevel).collect(Collectors.toList());
    }

    public void clear() {
        log.info("Clearing all department and camera level privileges");
        userPrivMapper.clear();
        cameraPrivMapper.clear();
    }

    public void clearAllUserPriv() {
        userPrivMapper.delete(new LambdaQueryWrapper<>());
        Set<String> keys = redisUtil.scan(ICP_USER_PRIV_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisUtil.del(keys);
        }
        log.info("Cleared all department privileges");
    }

    public void clearAllCameraPriv() {
        cameraPrivMapper.delete(new LambdaQueryWrapper<>());
        Set<String> keys = redisUtil.scan(ICP_CAMERA_PRIV_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisUtil.del(keys);
        }
        log.info("Cleared all camera level privileges");
    }

    /**
     * 清理无效的摄像头层级权限
     * 删除 camera_level 不在 validCameraLevels 集合中的权限记录
     *
     * @param validCameraLevels 有效的摄像头层级编号集合
     */
    public void clearInvalidCameraPriv(Set<String> validCameraLevels) {
        if (validCameraLevels == null || validCameraLevels.isEmpty()) {
            return;
        }

        List<ImUserIcpCameraPriv> invalidPrivs = cameraPrivMapper.selectList(
                Wrappers.lambdaQuery(ImUserIcpCameraPriv.class)
                        .notIn(ImUserIcpCameraPriv::getCameraLevel, validCameraLevels)
        );

        if (invalidPrivs == null || invalidPrivs.isEmpty()) {
            return;
        }

        cameraPrivMapper.delete(
                Wrappers.lambdaQuery(ImUserIcpCameraPriv.class)
                        .notIn(ImUserIcpCameraPriv::getCameraLevel, validCameraLevels)
        );
        log.info("Cleared {} invalid camera level privileges", invalidPrivs.size());

        Map<String, Long> keyMemberMap = invalidPrivs.stream()
            .collect(Collectors.toMap(
                priv -> ICP_CAMERA_PRIV_CACHE_PREFIX + priv.getCameraLevel(),
                ImUserIcpCameraPriv::getImUserId,
                (v1, v2) -> v1
            ));
        redisUtil.sRemMultiKeys(keyMemberMap);
    }

    /**
     * 清理无效的用户部门权限
     * 删除 depatrmentid 不在 validDepartmentIds 集合中的权限记录
     *
     * @param validDepartmentIds 有效的部门编号集合
     */
    public void clearInvalidUserPriv(Set<String> validDepartmentIds) {
        if (validDepartmentIds == null || validDepartmentIds.isEmpty()) {
            return;
        }

        List<ImUserIcpUserPriv> invalidPrivs = userPrivMapper.selectList(
                Wrappers.lambdaQuery(ImUserIcpUserPriv.class)
                        .notIn(ImUserIcpUserPriv::getDepartmentid, validDepartmentIds)
        );

        if (invalidPrivs == null || invalidPrivs.isEmpty()) {
            return;
        }

        userPrivMapper.delete(
                Wrappers.lambdaQuery(ImUserIcpUserPriv.class)
                        .notIn(ImUserIcpUserPriv::getDepartmentid, validDepartmentIds)
        );
        log.info("Cleared invalid department privileges: {}", invalidPrivs);

        Map<String, Long> keyMemberMap = invalidPrivs.stream()
            .collect(Collectors.toMap(
                priv -> ICP_USER_PRIV_CACHE_PREFIX + priv.getDepartmentid(),
                ImUserIcpUserPriv::getImUserId,
                (v1, v2) -> v1
            ));
        redisUtil.sRemMultiKeys(keyMemberMap);
    }

    public void setDefault() {
        log.info("开始设置默认ICP部门权限");
        var defaults = userPrivMapper.getDefaults();
        if (defaults == null || defaults.isEmpty()) {
            log.warn("未生成默认权限数据，可能原因：1.tb_isdn表为空 2.tb_im_user_icp表为空 3.两表ISDN无匹配");
            // 记录当前数据状态用于诊断
            long userCount = userPrivMapper.selectCount(new LambdaQueryWrapper<>());
            log.info("当前tr_im_user_icp_priv表记录数: {}", userCount);
            return;
        }
        log.info("生成了 {} 条默认权限记录", defaults.size());
        insertDefaults(defaults);
    }

    public void setDefault(Long userId) {
        if (userId == null) {
            log.info("Skip setting default ICP department privilege because userId is null");
            return;
        }
        var defaults = userPrivMapper.getDefaultsByUser(userId);
        if (defaults == null || defaults.isEmpty()) {
            log.info("No default ICP department privilege generated for user {}", userId);
        } else {
            log.info("Generated {} default ICP department privilege records for user {}: {}", defaults.size(), userId,
                    defaults.stream().map(ImUserIcpUserPriv::getDepartmentid).collect(Collectors.toList()));
        }
        insertDefaults(defaults);
    }

    private void insertDefaults(List<ImUserIcpUserPriv> defaults) {
        if (defaults == null || defaults.isEmpty()) {
            return;
        }
        for (var aDefault : defaults) {
            aDefault.setId(idWorker.nextId());
        }
        userPrivMapper.insertBatch(defaults);
        var group = defaults.stream().collect(Collectors.groupingBy(ImUserIcpUserPriv::getDepartmentid,
            Collectors.mapping(ImUserIcpUserPriv::getImUserId, Collectors.toList())));
        group.forEach((k, v) -> {
            redisUtil.sAddAll(ICP_USER_PRIV_CACHE_PREFIX + k, v);
        });
    }

    private void reportOperationLog(Long userId, List<String> privs, boolean isUser) {
        OperationTypeEnum operationType = isUser ? OperationTypeEnum.EQUIPMENT_AUTH_UPDATE : OperationTypeEnum.CAMERA_AUTH_UPDATE;
        OperationLog operationLog = new OperationLog(operationType);

        UserInfo user = SecurityUtils.getUser();
        if (Objects.nonNull(user)) {
            operationLog.setOperator(user.getUserName());
        }

        List<IcpImUser> icpImUsers = icpImUserMapper.selectList(new LambdaQueryWrapper<IcpImUser>().eq(IcpImUser::getId, userId));
        if (icpImUsers.isEmpty()) {
            return;
        }
        IcpImUser imUser = icpImUsers.get(0);

        List<String> privNames = getPrivNames(privs, isUser);
        String privContent = (privNames == null || privNames.isEmpty()) ? "权限已清空" : privNames.toString();

        operationLog.setOperation(String.format(operationLog.getOperation(), imUser.getName(), privContent));
        reportUtil.saveOperationLog(operationLog);
    }

    private List<String> getPrivNames(List<String> privs, boolean isUser) {
        if (privs == null || privs.isEmpty()) {
            return Collections.emptyList();
        }

        if (isUser) {
            List<Department> departments = departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                    .in(Department::getDepartmentid, privs));
            return departments.stream().map(Department::getDepartmentname).collect(Collectors.toList());
        } else {
            List<CameraLevel> icpCameras = cameraLevelMapper.selectList(new LambdaQueryWrapper<CameraLevel>()
                    .in(CameraLevel::getLevelNumber, privs));
            return icpCameras.stream().map(CameraLevel::getNodeName).collect(Collectors.toList());
        }
    }

    private void processDeptPriv(List<String> deptCodes, List<String> privs, boolean isUserPriv, OperationTypeEnum operationType) {
        if (deptCodes == null || deptCodes.isEmpty()) {
            log.warn("DeptIds is empty, skip authorization");
            return;
        }

        List<ImUserDto> imUserDtos = imUserRPCService.listByDeptCodes(deptCodes);
        if (imUserDtos == null || imUserDtos.isEmpty()) {
            log.info("No users found in departments: {}", deptCodes);
            return;
        }

        List<Long> userIds = imUserDtos.stream()
                .map(ImUserDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            log.warn("No valid user IDs found");
            return;
        }

        if (isUserPriv) {
            processUserPrivBatch(userIds, privs);
        } else {
            processCameraPrivBatch(userIds, privs);
        }

        OperationLog operationLog = new OperationLog(operationType);
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }

        List<String> privNames = getPrivNames(privs, isUserPriv);
        List<String> deptNames = imUserDtos.stream().map(ImUserDto::getDepartmentName).collect(Collectors.toList());
        String privContent = (privNames == null || privNames.isEmpty()) ? "权限已清空" : privNames.toString();
        String deptContent = deptNames.isEmpty() ? "" : deptNames.toString();
        operationLog.setOperation(String.format(operationLog.getOperation(), deptContent, privContent));
        reportUtil.saveOperationLog(operationLog);

    }

    @Transactional(rollbackFor = Exception.class)
    public void processUserPrivBatch(List<Long> userIds, List<String> privs) {
        List<ImUserIcpUserPriv> oldPrivs = userPrivMapper.selectList(
                new LambdaQueryWrapper<ImUserIcpUserPriv>()
                        .in(ImUserIcpUserPriv::getImUserId, userIds)
        );

        if (oldPrivs != null && !oldPrivs.isEmpty()) {
            userPrivMapper.delete(
                    new LambdaQueryWrapper<ImUserIcpUserPriv>()
                            .in(ImUserIcpUserPriv::getImUserId, userIds)
            );

            Map<String, Long> keyMemberMap = oldPrivs.stream()
                .collect(Collectors.toMap(
                    old -> ICP_USER_PRIV_CACHE_PREFIX + old.getDepartmentid(),
                    ImUserIcpUserPriv::getImUserId,
                    (v1, v2) -> v1
                ));
            redisUtil.sRemMultiKeys(keyMemberMap);
        }

        if (privs != null && !privs.isEmpty()) {
            // 双重分批处理，避免内存溢出
            int userCount = userIds.size();
            int privCount = privs.size();

            for (int i = 0; i < userCount; i += USER_BATCH_SIZE) {
                int userEndIndex = Math.min(i + USER_BATCH_SIZE, userCount);
                List<Long> batchUserIds = userIds.subList(i, userEndIndex);

                for (int j = 0; j < privCount; j += PRIV_BATCH_SIZE) {
                    int privEndIndex = Math.min(j + PRIV_BATCH_SIZE, privCount);
                    List<String> batchPrivs = privs.subList(j, privEndIndex);

                    List<ImUserIcpUserPriv> newPrivs = batchUserIds.stream()
                            .flatMap(userId -> batchPrivs.stream()
                                    .map(priv -> new ImUserIcpUserPriv(idWorker.nextId(), userId, priv)))
                            .collect(Collectors.toList());

                    userPrivMapper.insertBatch(newPrivs);
                }
            }

            for (String priv : privs) {
                redisUtil.sAddAll(ICP_USER_PRIV_CACHE_PREFIX + priv, userIds);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processCameraPrivBatch(List<Long> userIds, List<String> privs) {
        List<ImUserIcpCameraPriv> oldPrivs = cameraPrivMapper.selectList(
                new LambdaQueryWrapper<ImUserIcpCameraPriv>()
                        .in(ImUserIcpCameraPriv::getImUserId, userIds)
        );

        if (oldPrivs != null && !oldPrivs.isEmpty()) {
            cameraPrivMapper.delete(
                    new LambdaQueryWrapper<ImUserIcpCameraPriv>()
                            .in(ImUserIcpCameraPriv::getImUserId, userIds)
            );

            Map<String, Long> keyMemberMap = oldPrivs.stream()
                .collect(Collectors.toMap(
                    old -> ICP_CAMERA_PRIV_CACHE_PREFIX + old.getCameraLevel(),
                    ImUserIcpCameraPriv::getImUserId,
                    (v1, v2) -> v1
                ));
            redisUtil.sRemMultiKeys(keyMemberMap);
        }

        if (privs != null && !privs.isEmpty()) {
            // 双重分批处理，避免内存溢出
            int userCount = userIds.size();
            int privCount = privs.size();

            for (int i = 0; i < userCount; i += USER_BATCH_SIZE) {
                int userEndIndex = Math.min(i + USER_BATCH_SIZE, userCount);
                List<Long> batchUserIds = userIds.subList(i, userEndIndex);

                for (int j = 0; j < privCount; j += PRIV_BATCH_SIZE) {
                    int privEndIndex = Math.min(j + PRIV_BATCH_SIZE, privCount);
                    List<String> batchPrivs = privs.subList(j, privEndIndex);

                    List<ImUserIcpCameraPriv> newPrivs = batchUserIds.stream()
                            .flatMap(userId -> batchPrivs.stream()
                                    .map(priv -> new ImUserIcpCameraPriv(idWorker.nextId(), userId, priv)))
                            .collect(Collectors.toList());

                    cameraPrivMapper.insertBatch(newPrivs);
                }
            }

            for (String priv : privs) {
                redisUtil.sAddAll(ICP_CAMERA_PRIV_CACHE_PREFIX + priv, userIds);
            }
        }
    }
}