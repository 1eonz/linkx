package com.tdtech.cloudcmd.auth.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.auth.PwdUtil;
import com.tdtech.cloudcmd.auth.Utils.HistoryPwdUtils;
import com.tdtech.cloudcmd.auth.dto.ImUserDeptQO;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.dto.ImUserVO;
import com.tdtech.cloudcmd.auth.dto.UserImUserRelDTO;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.OrganizationUser;
import com.tdtech.cloudcmd.auth.entity.UserAdmin;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.mapper.OrganizationUserMapper;
import com.tdtech.cloudcmd.auth.mapper.UserAdminMapper;
import com.tdtech.cloudcmd.auth.service.IRoleService;
import com.tdtech.cloudcmd.auth.service.ImUserEsService;
import com.tdtech.cloudcmd.auth.service.ImUserService;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserGetVo;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.PBKDF2Util;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImUserServiceImpl implements ImUserService {
    private final String ALLOW_SIMPLE_PASSWORD = "ALLOW_SIMPLE_PASSWORD";
    private final String AUTH_PWD_REPETITION_COUNT = "AUTH_PWD_REPETITION_COUNT";

    private final ImUserMapper imUserMapper;
    private final IRoleService roleService;
    private final OAuthLoginServiceImpl oAuthLoginService;
    private final ImHttpClient imHttpClient;

    private final RedisUtil redisUtil;

    @Resource
    private IdWorker idWorker;

    @Resource
    private OrganizationUserMapper organizationUserMapper;

    @Resource
    private UserAdminMapper userAdminMapper;

    @Resource
    private LicenseUtil licenseUtil;

    @Resource
    private ReportUtil reportUtil;

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @DubboReference
    private DepartmentRpcApi departmentRpcApi;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ImUserEsService imUserEsService;

    @Resource
    private HistoryPwdUtils historyPwdUtils;

    @Override
    public ImUserDO getById(Long id) {
        return imUserMapper.selectById(id);
    }

    @Override
    public List<ImUserDO> getByIdList(List<Long> idList) {
        return imUserMapper.selectBatchIds(idList);
    }

    @Override
    public IPage<ImUserDO> queryPaged(IPage<ImUserDO> page, ImUserQO qo) {
        // 判断是否需要使用ES查询
        boolean useES = encryptionService.encryptEnabled() && needESQuery(qo);

        if (useES) {
            // 使用ES查询（仅针对name/mobile/idCard模糊查询）
            return imUserEsService.queryPaged(page, qo);
        } else {
            // 使用MySQL查询（默认）
            return queryPagedFromMySQL(page, qo);
        }
    }

    @Override
    public IPage<ImUserDO> queryPagedWithChildren(IPage<ImUserDO> page, ImUserDeptQO qo) {
        boolean useES = encryptionService.encryptEnabled() && needESQuery(qo);

        if (useES) {
            return imUserEsService.queryPagedWithChildren(page, qo);
        } else {
            return queryPagedWithChildrenFromMySQL(page, qo);
        }
    }

    /**
     * 判断是否需要使用ES查询
     * 只有当查询条件包含name/mobile/idCard的模糊查询时才使用ES
     */
    private boolean needESQuery(ImUserQO qo) {
        return (qo.getName() != null && !qo.getName().isBlank()) ||
               (qo.getMobile() != null && !qo.getMobile().isBlank()) ||
               (qo.getIdCard() != null && !qo.getIdCard().isBlank());
    }

    /**
     * 从MySQL分页查询
     */
    private IPage<ImUserDO> queryPagedFromMySQL(IPage<ImUserDO> page, ImUserQO qo) {
        List<String> priv = null;
        if (qo.getPrivString() != null && !qo.getPrivString().isBlank()) {
            priv = Arrays.asList(qo.getPrivString().split(","));
        }
        return imUserMapper.selectPage(page,
            Wrappers.lambdaQuery(ImUserDO.class)
                .like(qo.getName() != null && !qo.getName().isBlank(), ImUserDO::getName, qo.getName())//
                .like(qo.getCode() != null && !qo.getCode().isBlank(), ImUserDO::getCode, qo.getCode())//
                .like(qo.getIdCard() != null && !qo.getIdCard().isBlank(), ImUserDO::getIdCard, qo.getIdCard())//
                .like(qo.getMobile() != null && !qo.getMobile().isBlank(), ImUserDO::getMobile, qo.getMobile())//
                .like(qo.getEmail() != null && !qo.getEmail().isBlank(), ImUserDO::getEmail, qo.getEmail())//
                .like(qo.getDepartmentCode() != null && !qo.getDepartmentCode().isBlank(), ImUserDO::getDepartmentCode,
                    qo.getDepartmentCode())//
                .like(qo.getDepartmentName() != null && !qo.getDepartmentName().isBlank(), ImUserDO::getDepartmentName,
                    qo.getDepartmentName())//
                .in(qo.getPrivString() != null && !qo.getPrivString().isBlank(), ImUserDO::getDepartmentId, priv)
                .ne(ImUserDO::getType, 0)//
                .orderByDesc(ImUserDO::getGmtUpdated));
    }

    private IPage<ImUserDO> queryPagedWithChildrenFromMySQL(IPage<ImUserDO> page, ImUserDeptQO qo) {
        boolean hasDeptCode = qo.getDepartmentCode() != null && !qo.getDepartmentCode().isBlank();
        boolean expandChildren = hasDeptCode && qo.getIsChildren() != null && qo.getIsChildren() == 1;

        List<String> effectiveDeptCodes = null;
        List<Long> effectiveDeptIds = null;
        if (expandChildren) {
            DeptExpansion expansion = expandDepartment(qo.getDepartmentCode());
            effectiveDeptIds = expansion.getIds();
        } else if (hasDeptCode) {
            effectiveDeptCodes = List.of(qo.getDepartmentCode());
        }

        List<String> priv = null;
        if (!expandChildren && qo.getPrivString() != null && !qo.getPrivString().isBlank()) {
            priv = Arrays.asList(qo.getPrivString().split(","));
        }

        return imUserMapper.selectPage(page,
            Wrappers.lambdaQuery(ImUserDO.class)
                .like(qo.getName() != null && !qo.getName().isBlank(), ImUserDO::getName, qo.getName())
                .like(qo.getCode() != null && !qo.getCode().isBlank(), ImUserDO::getCode, qo.getCode())
                .like(qo.getIdCard() != null && !qo.getIdCard().isBlank(), ImUserDO::getIdCard, qo.getIdCard())
                .like(qo.getMobile() != null && !qo.getMobile().isBlank(), ImUserDO::getMobile, qo.getMobile())
                .like(qo.getEmail() != null && !qo.getEmail().isBlank(), ImUserDO::getEmail, qo.getEmail())
                .in(!expandChildren && hasDeptCode && effectiveDeptCodes != null && !effectiveDeptCodes.isEmpty(),
                    ImUserDO::getDepartmentCode, effectiveDeptCodes)
                .in(expandChildren && effectiveDeptIds != null && !effectiveDeptIds.isEmpty(),
                    ImUserDO::getDepartmentId, effectiveDeptIds)
                .like(!expandChildren && qo.getDepartmentName() != null && !qo.getDepartmentName().isBlank(),
                    ImUserDO::getDepartmentName, qo.getDepartmentName())
                .in(!expandChildren && priv != null, ImUserDO::getDepartmentId, priv)
                .ne(ImUserDO::getType, 0)
                .orderByDesc(ImUserDO::getGmtUpdated));
    }

    private DeptExpansion expandDepartment(String departmentCode) {
        List<OrganizationVO> orgList = departmentRpcApi.queryDepartmentForList(departmentCode);

        if (orgList != null) {
            orgList.forEach(org -> log.info("expandDepartment: org id={}, code={}, name={}, parentCode={}",
                org.getId(), org.getCode(), org.getName(), org.getParentCode()));
        }
        if (orgList == null || orgList.isEmpty()) {
            return new DeptExpansion(List.of(departmentCode), null);
        }
        List<String> codes = orgList.stream()
            .map(OrganizationVO::getCode)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        List<Long> ids = orgList.stream()
            .map(OrganizationVO::getId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        return new DeptExpansion(codes, ids);
    }

    @Data
    @AllArgsConstructor
    private static class DeptExpansion {
        private List<String> codes;
        private List<Long> ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteImUserById(List<Long> ids) {
        int i = imUserMapper.deleteBatchIds(ids);
        if (encryptionService.encryptEnabled()) {
            imUserEsService.deleteByIds(ids);
        }
        return i;
    }

    @Override
    @LogReport(type = OperationTypeEnum.USER_DELETE)
    @Transactional(rollbackFor = Exception.class)
    public int deleteImUser(@LogReportParam List<ImUserDO> userList) {
        List<Long> userIds = userList.stream().map(ImUserDO::getId).collect(Collectors.toList());
        return deleteImUserById(userIds);
    }

    @Override
    @LogReport(type = OperationTypeEnum.USER_INSERT)
    public void addImUsers(Long roleId, @LogReportParam List<ImUserDO> imUsers) throws NoSuchAlgorithmException {
        // 检查是否已达license接入数量
        oAuthLoginService.checkLimitUserCount(imUsers.size());

        // 根据身份证号码校验是否存在重复数据
        checkByIdCard(imUsers);

        var ids = imUsers.stream().map(ImUserDO::getId).collect(Collectors.toList());
        var existUser = imUserMapper.selectBatchIds(ids);
        var exitstIds = Optional.ofNullable(existUser).stream().flatMap(Collection::stream).map(ImUserDO::getId)
                .collect(Collectors.toSet());
        // fix direct leaders
        var lids = imUsers.stream().map(ImUserDO::getDirectLeaderId).filter(lid -> lid != null && lid != 0L)
                .map(Object::toString).collect(Collectors.joining(","));
        var leaderMap = new LinkedHashMap<Long, String>();
        if (!lids.isBlank()) {
            var userIDNameInfos = imHttpClient.queryUserDetail(lids);
            if (userIDNameInfos != null && !userIDNameInfos.isEmpty()) {
                for (var userIDNameInfo : userIDNameInfos) {
                    leaderMap.put(userIDNameInfo.getId(), userIDNameInfo.getName());
                }
            }
        }
        // save
        for (var imUserDO : imUsers) {
            if (exitstIds.contains(imUserDO.getId())) {
                continue;
            }
            if (imUserDO.getDirectLeaderId() != null && !Objects.equals(imUserDO.getDirectLeaderId(), 0L)) {
                var lname = leaderMap.get(imUserDO.getDirectLeaderId());
                imUserDO.setDirectLeaderName(lname);
            }
            imUserDO.setPwdTime(new Date()).setType(1).setStatus(0)
                    .setPassword(
                            PBKDF2Util.PBKDF2ForPassStandard(
                                    imUserDO.getPassword() == null || imUserDO.getPassword().isBlank()
                                            ? OAuthLoginServiceImpl.DEFAULT_PASSWORD : imUserDO.getPassword(),
                                    PBKDF2Util.generateSalt()))
                    .setGmtCreated(new Date()).setGmtUpdated(new Date());
            if (encryptionService.encryptEnabled()) {
                // 先写es，再写数据库, 避免被加密后写入es
                imUserEsService.insert(Collections.singletonList(imUserDO));
            }
            imUserMapper.insert(imUserDO);
            roleService.setRole(imUserDO.getId(), roleId);
        }

        if (existUser != null && !existUser.isEmpty()) {
            var names = existUser.stream().map(ImUserDO::getName).collect(Collectors.joining(","));
            throw new BusinessException(I18nUtil.get("USER_ALREADY_EXISTS") + names);
        }
    }

    private void checkByIdCard(List<ImUserDO> imUsers) {
        Set<String> idCards = new HashSet<>();
        for (ImUserDO user : imUsers) {
            String idCard = user.getIdCard();
            if (idCard != null && !idCards.add(idCard)) {
                throw new BusinessException(I18nUtil.get("USER_IDCARD_EXISTS"));
            }
        }
    }

    @Override
    public void changeStatus(Long userId, Integer status) {
        imUserMapper.update(null, Wrappers.lambdaUpdate(ImUserDO.class).eq(ImUserDO::getId, userId)
            .set(ImUserDO::getStatus, status).set(ImUserDO::getGmtUpdated, new Date()));
        if (status == 1) {
            oAuthLoginService.kickOutUserForRPC(userId + "");
        }
        if (encryptionService.encryptEnabled()) {
            ImUserDO userDO = imUserMapper.selectById(userId);
            if (userDO != null) {
                imUserEsService.update(userDO);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogReport(type = OperationTypeEnum.USER_ENABLE)
    public void enable(@LogReportParam ImUserDO imUserDO) {
        changeStatus(imUserDO.getId(), imUserDO.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogReport(type = OperationTypeEnum.USER_DISABLE)
    public void disable(@LogReportParam ImUserDO imUserDO) {
        changeStatus(imUserDO.getId(), imUserDO.getStatus());
    }

    @Override
    @LogReport(type = OperationTypeEnum.RESET_PASSWORD)
    @Transactional(rollbackFor = Exception.class)
    public void changePwd(@LogReportParam ImUserDO imUserDO) throws NoSuchAlgorithmException {
        Long userId = imUserDO.getId();
        String pwd = imUserDO.getPassword();
        // 校验密码非空
        PwdUtil.checkPwdNotEmpty(pwd);
        // check new pwd
        String allowSimplePassword = globalsRpcService.getGlobalsValueByName(ALLOW_SIMPLE_PASSWORD);
        if (allowSimplePassword != null && Integer.parseInt(allowSimplePassword) == 0) {
            // 获取用户信息以校验密码不能与用户名相同
            ImUserDO user = imUserMapper.selectById(userId);
            PwdUtil.checkPwdFormat(pwd, user != null ? user.getIdCard() : null);
        }
        String newPwd = PBKDF2Util.PBKDF2ForPassStandard(pwd, PBKDF2Util.generateSalt());

        String historyLimitConfig = globalsRpcService.getGlobalsValueByName(AUTH_PWD_REPETITION_COUNT);
        int historyLimit = StringUtils.isEmpty(historyLimitConfig) ? 3 : Integer.parseInt(historyLimitConfig);
        // 校验新密码是否与最近N次历史密码重复
        historyPwdUtils.checkHistoryPassword(userId, pwd, historyLimit);

        var i = imUserMapper.update(null,
            Wrappers.lambdaUpdate(ImUserDO.class).eq(ImUserDO::getId, userId)
                .set(ImUserDO::getPassword, newPwd)
                .set(ImUserDO::getGmtUpdated, new Date()));
        if (i != 0) {
            redisUtil.sAdd("RESET_PASSWORD_USERS", userId);
            oAuthLoginService.kickOutUserForRPC(userId + "");
            historyPwdUtils.saveHistoryPassword(userId, newPwd, historyLimit);
        }
        // 修改密码新增同步修改adminUser
        updateAdminUser(userId, newPwd);
        if (encryptionService.encryptEnabled()) {
            ImUserDO user = imUserMapper.selectById(imUserDO.getId());
            if (user != null) {
                imUserEsService.update(user);
            }
        }
    }

    private void updateAdminUser(Long userId, String pwd) {
        try {
            UserAdmin userAdmin = new UserAdmin();
            userAdmin.setId(userId);
            userAdmin.setPassword(pwd);
            userAdminMapper.updatePwdById(userAdmin);
        } catch (Exception e) {
             log.error("修改密码同步修改adminUser失败", e);
        }
    }

    /**
     * 只更新修改时间，排序用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGmtUpdateForRole(List<Long> userIds) {
        imUserMapper.update(null,
            Wrappers.lambdaUpdate(ImUserDO.class).in(ImUserDO::getId, userIds).set(ImUserDO::getGmtUpdated, new Date()));
        for (var userId : userIds) {
            oAuthLoginService.kickOutUserForRPC(userId + "");
        }
        if (encryptionService.encryptEnabled()) {
            List<ImUserDO> userDOS = imUserMapper.selectByIds(userIds);
            if (CollectionUtils.isNotEmpty(userDOS)) {
                imUserEsService.updateByIds(userDOS);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAdmin(ImUserVO imUserVO) throws NoSuchAlgorithmException {
        String detail = buildAdminUserInsertDetail(imUserVO);
        executeAdminUserOperation(OperationTypeEnum.USER_INSERT, imUserVO.getIdCard(), detail, () -> {
            // 先查询账户是否已存在
            var imUserDO = imUserMapper.selectOne(Wrappers.lambdaQuery(ImUserDO.class).eq(ImUserDO::getIdCard, imUserVO.getIdCard()));
            if (imUserDO != null) {
                throw new BusinessException("用户已存在");
            }
            imUserDO = new ImUserDO();
            imUserDO.setPwdTime(new Date()).setType(0).setStatus(0)
                    .setPassword(
                            PBKDF2Util.PBKDF2ForPassStandard(
                                    imUserVO.getPassword() == null || imUserVO.getPassword().isBlank()
                                            ? OAuthLoginServiceImpl.DEFAULT_PASSWORD2 : imUserVO.getPassword(),
                                    PBKDF2Util.generateSalt()))
                    .setGmtCreated(new Date()).setGmtUpdated(new Date());
            imUserDO.setId(idWorker.nextId());
            String idCard = imUserVO.getIdCard();
            imUserDO.setCode(idCard);
            imUserDO.setIdCard(idCard);
            imUserDO.setName(idCard);
            imUserDO.setMobile(idCard);
            imUserDO.setEmail(idCard);
            imUserDO.setIsdn(idCard);

            // 同步到ES
            if (encryptionService.encryptEnabled()) {
                imUserEsService.insert(Collections.singletonList(imUserDO));
            }
            imUserMapper.insert(imUserDO);

            // 默认超管
            roleService.setRole(imUserDO.getId(), 1L);
            // TODO 同步将用户和拥有的组织部门也插入新表
            try {
                insertUserAdmin(imUserDO);
                insertUserOrganId(imUserVO, imUserDO);
            } catch (Exception e) {
                log.error("同步将用户和拥有的组织部门也插入新表失败", e);
            }
        });
    }

    @Override
    public IPage<ImUserDO> queryAdmin(IPage<ImUserDO> page, ImUserQO qo) {
        // 判断是否需要使用ES查询（仅当包含name模糊查询时）
        boolean useES = encryptionService.encryptEnabled() && qo.getName() != null && !qo.getName().isBlank();
        if (useES) {
            // 使用ES查询
            return imUserEsService.queryAdmin(page, qo);
        } else {
            // 使用MySQL查询（默认）
            return queryAdminFromMySQL(page, qo);
        }
    }

    /**
     * 从MySQL查询Admin用户
     */
    private IPage<ImUserDO> queryAdminFromMySQL(IPage<ImUserDO> page, ImUserQO qo) {
        return imUserMapper.selectPage(page,
                Wrappers.lambdaQuery(ImUserDO.class)
                        .like(qo.getName() != null && !qo.getName().isBlank(), ImUserDO::getName, qo.getName())//
                        .eq(ImUserDO::getType, 0)
                        .ne(ImUserDO::getId, 1L)//
                        .orderByDesc(ImUserDO::getGmtCreated)
                        .orderByDesc(ImUserDO::getGmtUpdated));
    }

    @Override
    public ImUserVO queryAdminById(Long id) {
        ImUserDO imUserDO = imUserMapper.selectById(id);
        ImUserVO imUserVO = new ImUserVO();
        BeanUtils.copyProperties(imUserDO, imUserVO);
        imUserVO.setOrgList(organizationUserMapper.selectOrgPrivByUserId(id));
        return imUserVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAdminUser(ImUserVO imUserVO) {
        ImUserDO oldUser = imUserMapper.selectById(imUserVO.getId());
        List<Long> oldOrgIds = getAdminUserOrgIds(imUserVO.getId());
        OperationTypeEnum operationType = resolveAdminUserOperationType(imUserVO);
        String detail = buildAdminUserUpdateDetail(imUserVO, oldUser, oldOrgIds);
        executeAdminUserOperation(operationType, getAdminUserLogTarget(imUserVO, oldUser), detail, () -> {
            Integer status = imUserVO.getStatus();
            // 表示禁用启用
            if(status != null){
                imUserMapper.update(null, Wrappers.lambdaUpdate(ImUserDO.class).eq(ImUserDO::getId, imUserVO.getId())
                        .set(ImUserDO::getStatus, status).set(ImUserDO::getGmtUpdated, new Date()));
                if (status == 1) {
                    oAuthLoginService.kickOutUserForRPC(imUserVO.getId() + "");
                }
                try {
                    userAdminMapper.updateStatusById(imUserVO.getId(), status);
                } catch (Exception e) {
                    log.error("更新用户失败", e);
                }
            }
            List<Long> orgIds = imUserVO.getOrgIds();
            if(CollectionUtils.isNotEmpty(orgIds)){
                 try {
                     organizationUserMapper.deleteByUserIds(List.of(imUserVO.getId()));
                     List<OrganizationUser> list = orgIds.stream().map(orgId -> {
                         OrganizationUser organizationUser = new OrganizationUser();
                         organizationUser.setImOrgId(orgId);
                         organizationUser.setUserId(imUserVO.getId());
                         organizationUser.setGrantUserId(1L);
                         organizationUser.setGmtModified(LocalDateTime.now());
                         organizationUser.setGmtCreated(LocalDateTime.now());
                         organizationUser.setGrantTime(LocalDateTime.now());
                         organizationUser.setApplicationId(1L);
                         return organizationUser;
                     }).collect(Collectors.toList());
                     organizationUserMapper.insertBatch(list);
                 } catch (Exception e) {
                     log.error("更新用户失败", e);
                 }
            }
            
            // 同步到ES
            if (encryptionService.encryptEnabled()) {
                ImUserDO user = imUserMapper.selectById(imUserVO.getId());
                if (user != null) {
                    imUserEsService.update(user);
                }
            }
        });
    }

    @Override
    public void bindAdminImUser(UserImUserRelDTO userImUserRelDTO) {
        if (Objects.isNull(userImUserRelDTO)) {
            throw new BusinessException("绑定关系不能为空");
        }
        Long userId = getUserId(userImUserRelDTO);
        Long imUserId = userImUserRelDTO.getImUserId();
        if (userId == null || userId <= 0 || imUserId == null || imUserId <= 0) {
            throw new BusinessException("用户ID和警信用户ID不能为空且必须大于0");
        }
        UserAdmin userAdmin = userAdminMapper.selectById(userId);
        if (userAdmin == null) {
            throw new BusinessException("管理员用户不存在");
        }
        List<ImUser> imUsers = Optional.ofNullable(imHttpClient.userPageById(String.valueOf(imUserId)))
                .map(UserGetVo::getResults)
                .orElseGet(Collections::emptyList);
        ImUser imUser = imUsers.stream()
                .findFirst()
                .orElse(null);
        if (imUser == null) {
            throw new BusinessException("警信用户不存在");
        }
        // 绑定关系是一对一：允许管理员改绑，但目标警信用户不能被其他管理员占用。
        Long boundImUserId = normalizeBindId(userAdmin.getImUserId());
        if (Objects.equals(boundImUserId, imUserId)) {
            throw new BusinessException("当前绑定关系已存在");
        }
        UserAdmin boundAdmin = userAdminMapper.selectOne(Wrappers.lambdaQuery(UserAdmin.class)
                .eq(UserAdmin::getImUserId, imUserId)
                .ne(UserAdmin::getId, userId)
                .last("LIMIT 1"));
        if (boundAdmin != null) {
            throw new BusinessException("警信用户已绑定其他管理员用户");
        }

        userAdmin.setImUserId(imUserId);
        userAdmin.setImUserDeptId(Optional.ofNullable(imUser.getPrimaryDepartment().getId()).orElse(0L));
        userAdminMapper.updateImUserRelation(userAdmin);

    }

    @Override
    public void deleteAdminImUserRelation(UserImUserRelDTO userImUserRelDTO) {
        Long userId = getUserId(userImUserRelDTO);
        if (Objects.isNull(userId)) {
            throw new BusinessException("用户ID不能为空");
        }
        UserAdmin userAdmin = userAdminMapper.selectById(userId);
        if (userAdmin == null) {
            throw new BusinessException("管理员用户不存在");
        }
        userAdminMapper.deleteImUserRelation(userId);
    }

    @Override
    public UserAdmin getBindAdminImUser(Long userId) {
        userId = Optional.ofNullable(userId)
                .orElse(Optional.ofNullable(SecurityUtils.getUser()).map(UserInfo::getUserId).orElse(null));
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空且必须大于0");
        }
        UserAdmin userAdmin = userAdminMapper.selectById(userId);
        fillBindImUserName(userAdmin);
        return userAdmin;
    }

    private void fillBindImUserName(UserAdmin userAdmin) {
        if (userAdmin == null || userAdmin.getImUserId() == null || userAdmin.getImUserId() <= 0) {
            return;
        }
        ImUserDO localUser = imUserMapper.selectById(userAdmin.getImUserId());
        if (localUser != null && StringUtils.isNotBlank(localUser.getName())) {
            userAdmin.setImUserName(localUser.getName());
            return;
        }
        var remoteUsers = Optional.ofNullable(imHttpClient.userPageById(String.valueOf(userAdmin.getImUserId())))
            .map(userGetVo -> userGetVo.getResults())
            .orElseGet(Collections::emptyList);
        var remoteUser = remoteUsers.stream()
            .findFirst()
            .orElse(null);
        if (remoteUser != null) {
            userAdmin.setImUserName(remoteUser.getName());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdminByid(Long id) {
        ImUserDO oldUser = imUserMapper.selectById(id);
        executeAdminUserOperation(OperationTypeEnum.USER_DELETE, getAdminUserLogTarget(null, oldUser), () -> {
            imUserMapper.deleteById(id);
            oAuthLoginService.kickOutUserForRPC(id + "");
            try {
                organizationUserMapper.deleteByUserIds(List.of(id));
                userAdminMapper.deleteById(id);
            } catch (Exception e) {
                log.error("删除用户失败", e);
            }

            // 同步删除ES数据
            if (encryptionService.encryptEnabled()) {
                imUserEsService.deleteByIds(Collections.singletonList(id));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <E extends Exception> void executeAdminUserOperation(OperationTypeEnum operationType, String target,
            AdminUserOperation<E> operation) throws E {
        executeAdminUserOperation(operationType, target, null, operation);
    }

    @SuppressWarnings("unchecked")
    private <E extends Exception> void executeAdminUserOperation(OperationTypeEnum operationType, String target,
            String detail, AdminUserOperation<E> operation) throws E {
        OperationLog operationLog = buildAdminUserOperationLog(operationType, target, detail);
        try {
            operation.execute();
        } catch (RuntimeException e) {
            operationLog.setStatus(MSIPConstant.OPERATION_FAILURE);
            throw e;
        } catch (Exception e) {
            operationLog.setStatus(MSIPConstant.OPERATION_FAILURE);
            throw (E)e;
        } finally {
            reportUtil.saveOperationLog(operationLog);
        }
    }

    private OperationTypeEnum resolveAdminUserOperationType(ImUserVO imUserVO) {
        Integer status = imUserVO.getStatus();
        if (status == null) {
            return OperationTypeEnum.USER_UPDATE;
        }
        return status == 1 ? OperationTypeEnum.USER_DISABLE : OperationTypeEnum.USER_ENABLE;
    }

    private String getAdminUserLogTarget(ImUserVO imUserVO, ImUserDO imUserDO) {
        if (imUserVO != null && imUserVO.getIdCard() != null && !imUserVO.getIdCard().isBlank()) {
            return imUserVO.getIdCard();
        }
        if (imUserDO != null && imUserDO.getIdCard() != null && !imUserDO.getIdCard().isBlank()) {
            return imUserDO.getIdCard();
        }
        if (imUserVO != null && imUserVO.getId() != null) {
            return String.valueOf(imUserVO.getId());
        }
        if (imUserDO != null && imUserDO.getId() != null) {
            return String.valueOf(imUserDO.getId());
        }
        return "";
    }

    private String buildAdminUserUpdateDetail(ImUserVO newUser, ImUserDO oldUser, List<Long> oldOrgIds) {
        List<String> detailList = new ArrayList<>();
        addChangeDetail(detailList, "状态", formatAdminUserStatus(oldUser == null ? null : oldUser.getStatus()),
                formatAdminUserStatus(newUser.getStatus()), newUser.getStatus() != null);
        if (CollectionUtils.isNotEmpty(newUser.getOrgIds())) {
            List<Long> newOrgIds = normalizeOrgIds(newUser.getOrgIds());
            if (!Objects.equals(normalizeOrgIds(oldOrgIds), newOrgIds)) {
                Map<Long, String> orgNameMap = getOrgNameMap(oldOrgIds, newOrgIds);
                detailList.add(String.format("组织权限：%s -> %s", formatOrgIds(oldOrgIds, orgNameMap), formatOrgIds(newOrgIds, orgNameMap)));
            }
        }
        return detailList.isEmpty() ? "无变更" : String.join("；", detailList);
    }

    private String buildAdminUserInsertDetail(ImUserVO newUser) {
        if (CollectionUtils.isEmpty(newUser.getOrgIds())) {
            return null;
        }
        List<Long> newOrgIds = normalizeOrgIds(newUser.getOrgIds());
        Map<Long, String> orgNameMap = getOrgNameMap(List.of(), newOrgIds);
        return String.format("组织权限：%s", formatOrgIds(newOrgIds, orgNameMap));
    }

    private void addChangeDetail(List<String> detailList, String name, String oldValue, String newValue,
            boolean shouldCompare) {
        if (shouldCompare && !Objects.equals(oldValue, newValue)) {
            detailList.add(String.format("%s：%s -> %s", name, oldValue, newValue));
        }
    }

    private List<Long> getAdminUserOrgIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<OrganizationUser> organizationUsers = organizationUserMapper.selectByUserId(userId);
        if (CollectionUtils.isEmpty(organizationUsers)) {
            return List.of();
        }
        return organizationUsers.stream().map(OrganizationUser::getImOrgId).collect(Collectors.toList());
    }

    private List<Long> normalizeOrgIds(List<Long> orgIds) {
        if (CollectionUtils.isEmpty(orgIds)) {
            return List.of();
        }
        return orgIds.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    private Map<Long, String> getOrgNameMap(List<Long> oldOrgIds, List<Long> newOrgIds) {
        List<Long> orgIds = new ArrayList<>();
        orgIds.addAll(normalizeOrgIds(oldOrgIds));
        orgIds.addAll(normalizeOrgIds(newOrgIds));
        List<Long> normalizedOrgIds = normalizeOrgIds(orgIds);
        if (normalizedOrgIds.isEmpty()) {
            return Map.of();
        }
        List<OrganizationVO> organizations = departmentRpcApi.findList(normalizedOrgIds);
        if (CollectionUtils.isEmpty(organizations)) {
            return Map.of();
        }
        return organizations.stream().filter(Objects::nonNull).filter(organization -> organization.getId() != null)
                .collect(Collectors.toMap(OrganizationVO::getId, OrganizationVO::getName, (first, second) -> first));
    }

    private String formatOrgIds(List<Long> orgIds, Map<Long, String> orgNameMap) {
        List<Long> normalizedOrgIds = normalizeOrgIds(orgIds);
        return normalizedOrgIds.isEmpty() ? "空" : normalizedOrgIds.stream()
                .map(orgId -> formatOrgIdAndName(orgId, orgNameMap))
                .collect(Collectors.joining(","));
    }

    private String formatOrgIdAndName(Long orgId, Map<Long, String> orgNameMap) {
        String orgName = orgNameMap.get(orgId);
        if (orgName == null || orgName.isBlank()) {
            return String.valueOf(orgId);
        }
        return orgName + "(" + orgId + ")";
    }

    private String formatAdminUserStatus(Integer status) {
        if (status == null) {
            return "空";
        }
        return status == 1 ? "禁用" : "启用";
    }

    private OperationLog buildAdminUserOperationLog(OperationTypeEnum operationTypeEnum, String target, String detail) {
        OperationLog operationLog = new OperationLog(operationTypeEnum);
        if (operationLog.getOperation() != null && operationLog.getOperation().contains("%s")) {
            String logTarget = target == null ? "" : target;
            if (detail == null) {
                operationLog.setOperation(String.format(operationLog.getOperation(), logTarget));
            } else {
                operationLog.setOperation(String.format(operationLog.getOperation(), logTarget, detail));
            }
        }
        if (operationLog.getOperation() != null && detail != null && !detail.isBlank()
                && !operationLog.getOperation().contains(detail)) {
            operationLog.setOperation(operationLog.getOperation() + "，" + detail);
        }
        var user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }
        return operationLog;
    }

    private void insertUserOrganId(ImUserVO imUserVO, ImUserDO imUserDO) {
        List<Long> orgIds = imUserVO.getOrgIds();
        if(CollectionUtils.isNotEmpty(orgIds)){
            List<OrganizationUser> list = orgIds.stream().map(orgId -> {
                OrganizationUser organizationUser = new OrganizationUser();
                organizationUser.setImOrgId(orgId);
                organizationUser.setUserId(imUserDO.getId());
                organizationUser.setGrantUserId(1L);
                organizationUser.setGmtModified(LocalDateTime.now());
                organizationUser.setGmtCreated(LocalDateTime.now());
                organizationUser.setGrantTime(LocalDateTime.now());
                organizationUser.setApplicationId(1L);
                return organizationUser;
            }).collect(Collectors.toList());
            organizationUserMapper.insertBatch(list);
        }
    }

    private void insertUserAdmin(ImUserDO imUserDO) {
        UserAdmin userAdmin = new UserAdmin();
        userAdmin.setId(imUserDO.getId());
        userAdmin.setName(imUserDO.getIdCard());
        userAdmin.setAccount(imUserDO.getIdCard());
//        userAdmin.setImUserId(imUserDO.getId());
//        userAdmin.setImUserDeptId(imUserDO.getDepartmentId());
        userAdmin.setStatus(0);
        userAdmin.setApplicationId(1L);
        userAdmin.setPassword(imUserDO.getPassword());
        userAdmin.setFirstLogin(1);
        userAdmin.setGmtModified(LocalDateTime.now());
        userAdmin.setGmtCreated(LocalDateTime.now());
        userAdmin.setCreateUserId(1L);
        userAdmin.setErrorPwdCount(0);

        userAdminMapper.insertUser(userAdmin);
//        userAdminMapper.updateImUserRelation(userAdmin);
    }

    private Long normalizeBindId(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    @FunctionalInterface
    private interface AdminUserOperation<E extends Exception> {

        void execute() throws E;
    }

    @Nullable
    private static Long getUserId(UserImUserRelDTO userImUserRelDTO) {
        return Optional.of(userImUserRelDTO)
                .map(UserImUserRelDTO::getUserId)
                .orElse(Optional.ofNullable(SecurityUtils.getUser()).map(UserInfo::getUserId).orElse(null));
    }
}
