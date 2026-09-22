package com.tdtech.cloudcmd.auth.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.auth.PwdUtil;
import com.tdtech.cloudcmd.auth.Utils.HistoryPwdUtils;
import com.tdtech.cloudcmd.auth.constant.AuthBaseConstant;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginDto;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginInfo;
import com.tdtech.cloudcmd.auth.dto.OAuthPwdDto;
import com.tdtech.cloudcmd.auth.dto.OAuthRefreshDto;
import com.tdtech.cloudcmd.auth.dto.OAuthRefreshInfo;
import com.tdtech.cloudcmd.auth.dto.OAuthUserCacheDto;
import com.tdtech.cloudcmd.auth.dto.TokenRefreshMsg;
import com.tdtech.cloudcmd.auth.dto.ValidatorContext;
import com.tdtech.cloudcmd.auth.entity.*;
import com.tdtech.cloudcmd.auth.enums.CommonErrorEnum;
import com.tdtech.cloudcmd.auth.enums.UserLogEnums;
import com.tdtech.cloudcmd.auth.exception.OAuthException;
import com.tdtech.cloudcmd.auth.exception.UnAuthException;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.mapper.UserAdminMapper;
import com.tdtech.cloudcmd.auth.service.*;
import com.tdtech.cloudcmd.auth.service.mq.AuthPublisher;
import com.tdtech.cloudcmd.base.api.service.ExtendInfoPropertiesRpcService;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.constant.AuthConstants;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.api.CollaborationRpcApi;
import cloudcmd.service.rpc.PerWarningRalationRpcService;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplate;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.msip.enums.MSIPErrorEnum;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.PBKDF2Util;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.ACCESS_TMP_TOKEN_USER_KEY;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.ACCESS_TOKEN_TIMEOUT_KEY;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.ACCESS_TOKEN_USER_KEY;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.ACCESS_TOKEN_WILL_TIMEOUT_KEY;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.AUTH_AUTO_UNLOCK_TIME;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.AUTH_LOGIN_ERROR_NUM_LIMIT;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.AUTH_MULTI_END_LOGIN_ALLOWED;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.AUTH_PWD_VALIDITY_PERIOD;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.AUTH_REFRESHTOKEN_EXPIRE_IN;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.AUTH_TOKEN_EXPIRE_IN;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.CLIENT_USER_KEY;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.NOTIFY_EIGHTH_TIMEOUT;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.NOTIFY_TWELFTH_TIMEOUT;
import static com.tdtech.cloudcmd.auth.constant.OAuthConstant.REFRESH_TOKEN_USER_KEY;
import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.*;

/**
 * @author mWX556161
 * @date 2020/11/18 16:03
 */
@Slf4j
@Service
@DubboService
public class OAuthLoginServiceImpl implements IOAuthLoginService {
    public final static String RESET_PASSWORD_USERS = "RESET_PASSWORD_USERS";
    public static final String DEFAULT_PASSWORD = "Aa@123456";
    public static final String DEFAULT_PASSWORD2 = "eLTE@com1234";
    private static final SecureRandom numberGenerator = new SecureRandom();
    private final String ALLOW_SIMPLE_PASSWORD = "ALLOW_SIMPLE_PASSWORD";
    private final String AUTH_PWD_REPETITION_COUNT = "AUTH_PWD_REPETITION_COUNT";
    private final String PASSWORD_ERROR_COUNTER = "cloudcmd:auth:pwd_err_cnt";
    // 定时检查用户是否解锁，不该原来的key的缓存时间和逻辑，用新key处理
    private final String PASSWORD_ERROR_COUNTER_DELAY = "cloudcmd:auth:pwd_err_cnt:delay";

    private final Integer MAX_ERROR_COUNT = 5;

    @Resource
    private IApplicationService applicationService;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private IRoleService roleService;
    @Resource
    private AuthPublisher authPublisher;
    @Resource
    private IUserLogService userLogService;
    @Resource
    private PermissionServiceImpl permissionService;
    @Resource
    private ImHttpClient imHttpClient;
    @Resource
    private ImUserMapper imUserMapper;
    @Resource
    private RedisLockFactory redisLockFactory;

    @Resource
    private LicenseUtil licenseUtil;

    @DubboReference
    private ExtendInfoPropertiesRpcService extendInfoPropertiesRpcService;
    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @DubboReference
    private CollaborationRpcApi collaborationRpcApi;

    @DubboReference
    private PerWarningRalationRpcService perWarningRalationRpcService;

    @Resource
    private Executor bizExecutor;

    @Resource
    private ReportUtil reportUtil;

    @Resource
    private UserAdminMapper userAdminMapper;

    @Autowired
    private ImUserEsService imUserEsService;

    @Autowired
    private EncryptionService encryptionService;

    @Resource
    private HistoryPwdUtils historyPwdUtils;

    @Override
    public OAuthLoginDto oAuthLogin(OAuthLoginInfo loginInfo,
        Application application) {
        OperationLog log = new OperationLog(OperationTypeEnum.LOGIN);
        log.setOperation(String.format(log.getOperation(), loginInfo.getUsername()));
        // 当前用户
        ImUserDO userDo = null;
        try {
            // log.info("oAuthLogin loginInfo:{}", loginInfo);
            // 1、校验参数
            this.validateParam(loginInfo);
            // 2、验证用户信息
            var context = authenticate(loginInfo, application);
            userDo = context.getUser();
            // 3、判断是否允许多端登录
            if (application.getIsMultiLogin() == 0) {
                this.multiLoginValidate(loginInfo, userDo);
            }
            // 5.判断是否被重置密码，若重置则提示修改密码
            if (redisUtil.sIsMember(RESET_PASSWORD_USERS, userDo.getId())) { // 当前登录用户有被重置过密码
                // 给出提示, 在修改密码后将当前用户移出被重置用户列表
                var authLoginDto = createTmpToken(userDo, loginInfo, application);
                throw new OAuthException(COMMON_ERROR_137.getCode(), I18nUtil.get(COMMON_ERROR_137.getMsg()), authLoginDto);
            }
            // 6、生成token并返回对象
            OAuthLoginDto authLoginDto = this.saveUserToken(userDo, loginInfo, application);
            if (context.getWarningMsg() != null && !context.getWarningMsg().isBlank()) {
                // warning
                throw new OAuthException(COMMON_ERROR_121.getCode(), context.getWarningMsg(), authLoginDto);
            } else {
                return authLoginDto;
            }
        } catch (Exception e) {
            log.setStatus(MSIPConstant.OPERATION_FAILURE);
            // 抛出去，不影响现有的业务
            throw e;
        } finally {
            if (Objects.nonNull(userDo)) {
                log.setOperator(userDo.getName());
            } else {
                // 如果校验用户信息啥的报错了，导致userDo取不到，操作日志就显示不了操作人信息，这里再查一次吧，不改动原来校验逻辑
                ImUserDO userInfo;
                if (encryptionService.encryptEnabled()) {
                    userInfo = imUserEsService.findByIdCard(loginInfo.getUsername());
                } else {
                    userInfo = findUserByIdCard(loginInfo.getUsername());
                }
                if (Objects.nonNull(userInfo)) {
                    log.setOperator(userInfo.getName());
                }
            }
            reportUtil.saveOperationLog(log);
        }
    }

    @SneakyThrows
    @Override
    public OAuthLoginDto tokenLogin(OAuthLoginInfo loginInfo, Application application) {
        log.info("token login:{} {}", loginInfo, application);
        var token = loginInfo.getToken();
        var imUser = imHttpClient.userProfile(token);
        if (imUser == null) {
            throw new OAuthException(CommonErrorEnum.COMMON_ERROR_107.getCode(),
                I18nUtil.get(CommonErrorEnum.COMMON_ERROR_107.getMsg()));
        }
        var imUserDO = BeanCopyUtils.copyBean(imUser, ImUserDO::new);
        Optional.ofNullable(imUser.getUserDepartments()).stream().flatMap(Collection::stream)
            .filter(ImUser.UserDepartment::getIsPrimary).findAny().ifPresent(dep -> {
                imUserDO.setDepartmentCode(dep.getDepartmentCode());
                imUserDO.setDepartmentName(dep.getDepartmentName());
                imUserDO.setDepartmentId(dep.getId());
                // var imDepartments = imHttpClient.queryDepartment(dep.getDepartmentCode());
                // Optional.ofNullable(imDepartments).stream().flatMap(Collection::stream).findAny()
                // .ifPresent(dAny -> imUserDO.setDepartmentId(dAny.getId()));
            });
        var userGetVo = imHttpClient.userPage(imUserDO.getIdCard(), null);
        if (userGetVo != null && userGetVo.getResults() != null && !userGetVo.getResults().isEmpty()) {
            var imUserDetail = userGetVo.getResults().get(0);
            imUserDO.setDirectLeaderId(imUserDetail.getDirectLeaderId());
            if (imUserDetail.getDirectLeaderId() != null && imUserDetail.getDirectLeaderId() != 0) {
                var userIDNameInfos = imHttpClient.queryUserDetail(imUserDetail.getDirectLeaderId() + "");
                if (userIDNameInfos != null && !userIDNameInfos.isEmpty()) {
                    imUserDO.setDirectLeaderName(userIDNameInfos.get(0).getName());
                } else {
                    imUserDO.setDirectLeaderName(null);
                }
            }
        }
        ImUserDO upsert = upsert(imUserDO);

        return this.saveUserToken(upsert, loginInfo, application);
    }

    @SneakyThrows
    @Override
    public OAuthLoginDto h5Login(OAuthLoginInfo loginInfo, Application application) {
        // 1、校验参数
        var idCardNum = loginInfo.getUsername();
        // 2、验证用户信息
        var imUserDO = getImUserDO(idCardNum);
        if (imUserDO == null) {
            throw new OAuthException(COMMON_ERROR_113.getCode(), I18nUtil.get(COMMON_ERROR_113.getMsg()),
                I18nUtil.get(COMMON_ERROR_113.getMsg()));
        }
        ImUserDO upsert = upsert(imUserDO);
        // 6、生成token并返回对象
        return this.saveUserToken(upsert, loginInfo, application);
    }

    private ImUserDO upsert(ImUserDO imUserDO) throws NoSuchAlgorithmException {
        ImUserDO userDORaw = BeanCopyUtils.copyBean(imUserDO, ImUserDO::new);
        ImUserDO oldUser = imUserMapper.selectById(imUserDO.getId());
        var i = imUserMapper.updateById(imUserDO);
        if (i == 0) {
            var key = "cloudcmd:auth:tokenlogin:upsertlock:" + imUserDO.getId();
            log.info("lock key:{}", key);
            var redisLock = redisLockFactory.newRedisLock(key, Duration.ofSeconds(60L));
            try {
                redisLock.tryLockWithException(30L, TimeUnit.SECONDS);
                log.info("lock pass:{} ", imUserDO);
                i = imUserMapper.updateById(imUserDO);
                if (i == 0) {
                    // 检查接入人数，进行告警等操作
                    checkLimitUserCount(1);
                    String passWord = PBKDF2Util.PBKDF2ForPassStandard(DEFAULT_PASSWORD, PBKDF2Util.generateSalt());
                    imUserDO.setPwdTime(new Date()).setType(1).setStatus(0)
                        .setPassword(passWord);
                    roleService.setDefaultRole(imUserDO.getId());
                    if (encryptionService.encryptEnabled()) {
                        // 写入es
                        userDORaw.setPassword(passWord);
                        userDORaw.setPwdTime(new Date());
                        userDORaw.setType(1);
                        userDORaw.setStatus(0);
                        imUserEsService.insert(Collections.singletonList(userDORaw));
                    }
                    imUserMapper.insert(imUserDO);//锁标志 必须放最后
                } else {
                    if (encryptionService.encryptEnabled()) {
                        // 同步更新至es
                        imUserEsService.updateByIds(Collections.singletonList(userDORaw));
                    }
                }
            } finally {
                redisLock.unlock();
            }
        } else {
            if (encryptionService.encryptEnabled()) {
                // 同步更新至es
                imUserEsService.updateByIds(Collections.singletonList(userDORaw));
            }
        }
//        CompletableFuture.runAsync(() -> syncUserNameOnChange(oldUser, imUserDO), bizExecutor);
        return userDORaw;
    }

    private void syncUserNameOnChange(ImUserDO oldUser, ImUserDO newUser) {
        if (oldUser == null || oldUser.getId() == null) {
            return;
        }
        if (StringUtils.isBlank(oldUser.getName()) || StringUtils.isBlank(newUser.getName())) {
            return;
        }
        if (oldUser.getName().equals(newUser.getName())) {
            return;
        }
        log.info("user name changed, userId:{}, oldName:{}, newName:{}", oldUser.getId(), oldUser.getName(), newUser.getName());
        try {
            collaborationRpcApi.updateUserName(oldUser.getId(), newUser.getName());
        } catch (Exception e) {
            log.error("syncUserName to collaborationPost failed, userId:{}", oldUser.getId(), e);
        }
        try {
            perWarningRalationRpcService.updateTargetName(oldUser.getId(), newUser.getName());
        } catch (Exception e) {
            log.error("syncUserName to preWarningRalation failed, userId:{}", oldUser.getId(), e);
        }
    }

    @Override
    public void checkLimitUserCount(Integer ready2SaveCount) {
        LambdaQueryWrapper<ImUserDO> queryWrapper = new LambdaQueryWrapper<ImUserDO>()
                // 除了超级管理员以外的
                .eq(ImUserDO::getType, 1);
        var dbUserCount = imUserMapper.selectCount(queryWrapper);
        String limitCount = licenseUtil.getValue(LicenseEnum.ACCESSABLE_NUMBER.getCode());

        log.info("checkLimitUserCount ready2SaveCount: {}, dbUserCount: {}, limitCount: {}", ready2SaveCount, dbUserCount, limitCount);

        var limitCountInt = Long.parseLong(limitCount);
        // 是否超过80%
        boolean over80 = isOver80(dbUserCount + ready2SaveCount, limitCountInt);
        // 是否超过100%
        boolean over100 = (dbUserCount + ready2SaveCount) > limitCountInt;

        if (!over100) {
            // 擦除告警
            reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.EXCEEDED_OVER100);
        }
        if (!over80) {
            // 擦除告警
            reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.EXCEEDED_OVER80);
        }
        // 优先看是否超过100%，超过100%则不发送80%告警
        if (over100) {
            // 告警
            reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.EXCEEDED_OVER100);

            String error = String.format("可接入人数达到限制：%s", limitCount);
            log.error(error);
            // 并抛出异常
            throw new BusinessException(MSIPErrorEnum.SYSTEM_FUNCTIONS_ARE_LIMITED.getCode(), MSIPErrorEnum.SYSTEM_FUNCTIONS_ARE_LIMITED.getMsg());
        } else if (over80) {
            // 告警
            reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.EXCEEDED_OVER80);
        }
    }

    private boolean isOver80(Long part, Long total) {
        return part != null && total != null && total != 0L
                && part * 10 > total * 8;
    }

    @Nullable
    private ImUserDO getImUserDO(String idCardNum) {
        var userGetVo = imHttpClient.userPage(idCardNum, null);
        if (userGetVo == null || userGetVo.getResults() == null || userGetVo.getResults().isEmpty()) {
            return null;
        }

        var imUser = userGetVo.getResults().get(0);
        var imUserDO = BeanCopyUtils.copyBean(imUser, ImUserDO::new);
        Optional.ofNullable(imUser.getUserDepartments()).stream().flatMap(Collection::stream)
            .filter(ImUser.UserDepartment::getIsPrimary).findAny().ifPresent(dep -> {
                imUserDO.setDepartmentCode(dep.getDepartmentCode());
                imUserDO.setDepartmentName(dep.getDepartmentName());
                imUserDO.setDepartmentId(dep.getId());
                // var imDepartments = imHttpClient.queryDepartment(dep.getDepartmentCode());
                // Optional.ofNullable(imDepartments).stream().flatMap(Collection::stream).findAny()
                // .ifPresent(dAny -> imUserDO.setDepartmentId(dAny.getId()));
            });
        if (!Objects.equals(imUser.getDirectLeaderId(), 0L)) {
            var userIDNameInfos = imHttpClient.queryUserDetail(imUser.getDirectLeaderId() + "");
            if (userIDNameInfos != null && !userIDNameInfos.isEmpty()) {
                imUserDO.setDirectLeaderName(userIDNameInfos.get(0).getName());
            } else {
                imUserDO.setDirectLeaderName(null);
            }
        }
        return imUserDO;
    }

    private ImUserDO findUserByIdCard(String idCard) {
        return Optional.ofNullable(
                        imUserMapper.selectList(Wrappers.lambdaQuery(ImUserDO.class).eq(ImUserDO::getIdCard, idCard))).stream()
                .flatMap(Collection::stream).findAny().orElse(null);
    }

    @SneakyThrows
    private ValidatorContext authenticate(OAuthLoginInfo loginInfo, Application application) {
        String username = loginInfo.getUsername();
        String password = loginInfo.getPassword();
        // 1.自动建账号
        // try {
        // var remote = getImUserDO(username);
        // if (remote != null) {
        // upsert(remote);
        // }
        // } catch (Exception e) {
        // log.warn("error:{}", e.getMessage());
        // }n
        var validatorContext = new ValidatorContext();
        validatorContext.setApplication(application);
        ImUserDO userDo;
        if (encryptionService.encryptEnabled()) {
            userDo = imUserEsService.findByIdCard(username);
        } else {
            userDo = findUserByIdCard(username);
        }

        if (userDo == null) {
            throw new OAuthException(COMMON_ERROR_110.getCode(), I18nUtil.get(COMMON_ERROR_110.getMsg()),
                I18nUtil.get(COMMON_ERROR_110.getMsg()));
        }
        validatorContext.setUser(userDo);

        //
        if (!Objects.equals(AuthBaseConstant.PWD_AUTH_TYPE, loginInfo.getGrantType())) {
            throw new OAuthException(COMMON_ERROR_106.getCode(), I18nUtil.get(COMMON_ERROR_106.getMsg()));
        }
        // 2.检查用户锁定状态
        if (Objects.equals(userDo.getStatus(), 1)) {
            throw new OAuthException(COMMON_ERROR_112.getCode(), I18nUtil.get(COMMON_ERROR_112.getMsg()),
                I18nUtil.get(COMMON_ERROR_112.getMsg()));
        }

        // 3.校验用户名和密码
        var passwordErrorCounter = redisUtil.get(PASSWORD_ERROR_COUNTER + userDo.getId(), PasswordErrorCounter.class);
        var unLockTime = redisUtil.get(AUTH_AUTO_UNLOCK_TIME, Integer.class);
        unLockTime = unLockTime == null ? 10 : unLockTime;
        Integer errorCount = redisUtil.get(AUTH_LOGIN_ERROR_NUM_LIMIT, Integer.class);
        errorCount = errorCount == null ? MAX_ERROR_COUNT : errorCount;
        var now = System.currentTimeMillis();
        if (passwordErrorCounter != null && passwordErrorCounter.shouldBlock(unLockTime, errorCount, now)) {// 锁了 不验密码
            // 上报锁定告警
            reportAlarm(userDo);
            long seconds = (passwordErrorCounter.getTime() + unLockTime * 60L * 1000L - now) / 1000;
            String data = String.format(I18nUtil.get("USERLOGINLOCKED"), seconds);
            // 上报用户被冻结的日志
            roleService.lockUser(userDo);
            throw new OAuthException(COMMON_ERROR_112.getCode(), I18nUtil.get(COMMON_ERROR_112.getMsg()), data);
        } else if (!PwdUtil.checkPwdMatches(userDo.getPassword(), password)) {
            if (passwordErrorCounter == null) {
                passwordErrorCounter = new PasswordErrorCounter().setCount(1).setTime(now);
                redisUtil.set(PASSWORD_ERROR_COUNTER + userDo.getId(), passwordErrorCounter, unLockTime,
                    TimeUnit.MINUTES);
                redisUtil.set(PASSWORD_ERROR_COUNTER_DELAY + userDo.getId(), passwordErrorCounter);
            } else {
                passwordErrorCounter.increaseCount().setTime(now);
                redisUtil.set(PASSWORD_ERROR_COUNTER + userDo.getId(), passwordErrorCounter, unLockTime,
                    TimeUnit.MINUTES);
                redisUtil.set(PASSWORD_ERROR_COUNTER_DELAY + userDo.getId(), passwordErrorCounter);
            }
            throw new OAuthException(COMMON_ERROR_113.getCode(), I18nUtil.get(COMMON_ERROR_113.getMsg()),
                I18nUtil.get(COMMON_ERROR_113.getMsg()));
        } else {
            redisUtil.del(PASSWORD_ERROR_COUNTER + userDo.getId());
            redisUtil.del(PASSWORD_ERROR_COUNTER_DELAY + userDo.getId());
            // 擦除锁定告警
            clearAlarm(userDo.getId());
        }
        // 9.判断密码是否过期,90天
        Date pwdLastModifyTime = userDo.getPwdTime();
        if (pwdLastModifyTime != null) {
            long pwdValidity = Optional.ofNullable(redisUtil.get(AUTH_PWD_VALIDITY_PERIOD, Long.class)).orElse(90L);
            var expireDateTime =
                ZonedDateTime.ofInstant(pwdLastModifyTime.toInstant(), ZoneId.systemDefault()).plusDays(pwdValidity);
            // expired
            var nowDateTime = ZonedDateTime.now();
            var wanningDateTime = expireDateTime.plusDays(-7);
            if (expireDateTime.isBefore(nowDateTime)) {
                var authLoginDto = createTmpToken(userDo, loginInfo, application);
                throw new OAuthException(COMMON_ERROR_115.getCode(), I18nUtil.get(COMMON_ERROR_115.getMsg()),
                    authLoginDto);
            } else if (wanningDateTime.isBefore(nowDateTime)) {
                var nowSecond = nowDateTime.toInstant().getEpochSecond();
                var expireSecond = expireDateTime.toInstant().getEpochSecond();
                var daysToExpire = (expireSecond - nowSecond) / 24L / 60L / 60L + 1;
                validatorContext.setWarningMsg("" + daysToExpire);
            }
        }
        return validatorContext;
    }

    private void reportAlarm(ImUserDO user) {
        AlarmTemplate alarmTemplate = getAlarmTemplateEnum(user.getId());
        if (Objects.isNull(alarmTemplate)) {
            return;
        }
        authPublisher.publishAlarmMessage(alarmTemplate, user.getIdCard());
    }

    private void clearAlarm(Long userId) {
        AlarmTemplate alarmTemplate = getAlarmTemplateEnum(userId);
        if (Objects.isNull(alarmTemplate)) {
            return;
        }
        authPublisher.publishAlarmClearMessage(alarmTemplate);
    }

    private OAuthLoginDto createTmpToken(ImUserDO user, OAuthLoginInfo loginInfo, Application application) {
        String refreshToken = this.getSecureRandom();// nope
        String accessToken = this.getSecureRandom();
        var loginCacheDto =
            UserInfo.builder().userId(user.getId()).appKey(application.getAppKey())
                .deviceId(loginInfo.getDeviceId()).userName(user.getName()).executorCode(user.getCode())
                .createTime(new Date()).loginIp(loginInfo.getLoginIp()).isAdmin(user.getType() == 0)
                .executorId(user.getId()).build();

        int tokenExpire = 30 * 60; // 30Min
        int refreshTokenExpire = 30 * 60;// 30Min

        redisUtil.set(ACCESS_TMP_TOKEN_USER_KEY.replace("{access_token}", accessToken), loginCacheDto, tokenExpire,
            TimeUnit.SECONDS);

        OAuthLoginDto authLoginDto = new OAuthLoginDto();
        authLoginDto.setUserName(loginCacheDto.getUserName());
        authLoginDto.setAccessToken(accessToken);
        authLoginDto.setUserId(String.valueOf(user.getId()));
        authLoginDto.setExpireIn(tokenExpire);
        authLoginDto.setRefreshToken(refreshToken);
        authLoginDto.setScope("all");
        authLoginDto.setTokenType("password");
        authLoginDto.setRefreshTokenExpireIn(refreshTokenExpire * 60 * 60 * 24);
        return authLoginDto;
    }

    /**
     * 校验client参数
     *
     * @param loginInfo
     */
    private void validateParam(OAuthLoginInfo loginInfo) {
        String clientId = loginInfo.getClientId();
        String clientSecret = loginInfo.getClientSecret();
        String username = loginInfo.getUsername();
        String password = loginInfo.getPassword();
        String grantType = loginInfo.getGrantType();
        String deviceId = loginInfo.getDeviceId();
        if (StringUtils.isNullBlank(clientId) || StringUtils.isNullBlank(clientSecret) || StringUtils.isNullBlank(
            username) || StringUtils.isNullBlank(password) || StringUtils.isNullBlank(
            grantType) || StringUtils.isNullBlank(deviceId)) {
            String data = "the mandatory parameter is empty.:" + clientId;
            throw new OAuthException(COMMON_ERROR_106.getCode(), I18nUtil.get(COMMON_ERROR_106.getMsg()), data);
        }
        this.validateClient(clientId, clientSecret);
    }

    /**
     * 校验应用
     *
     * @param clientId
     * @param clientSecret
     */
    private void validateClient(String clientId, String clientSecret) {
        Application application = applicationService.getApplicationById(clientId);
        // 校验 ClientDetails
        if (application == null) {
            String data = "client id is not exists:" + clientId;
            log.info(data);
            throw new OAuthException(COMMON_ERROR_106.getCode(), I18nUtil.get(COMMON_ERROR_106.getMsg()), data);
        }
        String salt = PBKDF2Util.getSaltFromStandardPass(application.getAppSecret());
        // 关键字扫描
        String encryptPassword = PBKDF2Util.PBKDF2ForPassStandard(clientSecret, salt);
        if (StringUtils.isNotBlank(encryptPassword) && !encryptPassword.equals(application.getAppSecret())) {
            String data = "clientSecret is not correct:" + clientSecret;
            log.info(data);
            throw new OAuthException(COMMON_ERROR_106.getCode(), I18nUtil.get(COMMON_ERROR_106.getMsg()), data);
        }
    }

    /**
     * 多端登录判断
     */
    public void multiLoginValidate(OAuthLoginInfo loginInfo, ImUserDO user) {
        Boolean multiEndLoginAllowed = redisUtil.get(AUTH_MULTI_END_LOGIN_ALLOWED, Boolean.class);
        if (multiEndLoginAllowed == null || multiEndLoginAllowed) {
            // 允许多测登陆
            String userCacheDtoStr = redisUtil.get(CLIENT_USER_KEY.replace("{userId}", String.valueOf(user.getId()))
                .replace("{clientId}", loginInfo.getClientId()), String.class);
            this.handleKickOut(userCacheDtoStr, loginInfo);
        } else {
            Set<String> keys = redisUtil.keys(
                CLIENT_USER_KEY.replace("{userId}", String.valueOf(user.getId())).replace("{clientId}", "*"));
            for (String key : keys) {
                String userCacheDtoStr = redisUtil.get(key, String.class);
                // log.info("user kick out key is:{}", userCacheDtoStr);
                this.handleKickOut(userCacheDtoStr, loginInfo);
            }
        }
    }

    /**
     * 处理踢出
     *
     * @param userCacheDtoStr
     */
    public void handleKickOut(String userCacheDtoStr, OAuthLoginInfo loginInfo) {
        if (!StringUtils.isNullBlank(userCacheDtoStr)) {
            OAuthUserCacheDto userCacheDto = JSONObject.parseObject(userCacheDtoStr, OAuthUserCacheDto.class);
            // 如果存在同一类的同一用户
            String accessToken = userCacheDto.getAccessToken();
            var key = ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken);
            UserInfo loginCacheDto =
                redisUtil.get(key, UserInfo.class);
            // 发布踢出消息会，web端会调用登出接口清除缓存
            log.info("handleKickOut--------------loginCacheDtoStr is:{}", loginCacheDto);
            if (loginCacheDto != null) {
                // 判断deviceId不一样 //发送踢出通知
                // 如果设备号相同，则表明是同一台设备的登录
                String deviceId = loginCacheDto.getDeviceId();
                String loginIp = loginCacheDto.getLoginIp();
                redisUtil.del(key);
                redisUtil.del(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", userCacheDto.getRefreshToken()));
                redisUtil.del(CLIENT_USER_KEY.replace("{userId}", String.valueOf(loginCacheDto.getUserId()))
                    .replace("{clientId}", loginCacheDto.getAppKey()));
                // log.info("handleKickOut kicOut device on the latest.");
                authPublisher.publishKickOutMessage(userCacheDto.getUserId(), userCacheDto);
            }
        }
    }

    @Override
    public void kickOutUserForRPC(String userId) {
        Set<String> keys = redisUtil.keys(CLIENT_USER_KEY.replace("{userId}", userId).replace("{clientId}", "*"));
        for (String key : keys) {
            String[] keyPaths = key.split(":");
            if (keyPaths.length == 6) {
                String userCacheDtoStr = redisUtil.get(key, String.class);
                // log.info("user kick out key is:{}", userCacheDtoStr);
                if (!StringUtils.isNullBlank(userCacheDtoStr)) {
                    OAuthUserCacheDto userCacheDto = JSONObject.parseObject(userCacheDtoStr, OAuthUserCacheDto.class);
                    // 如果存在同一类的同一用户
                    String accessToken = userCacheDto.getAccessToken();
                    var tokenKey = ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken);
                    var refreshTokenKey =
                        REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", userCacheDto.getRefreshToken());
                    UserInfo user = redisUtil.get(tokenKey, UserInfo.class);
                    // 发布踢出消息会，web端会调用登出接口清除缓存
                    log.info("handleKickOut--------------user is:{}", user);
                    redisUtil.del(tokenKey, refreshTokenKey, key);
                    if (user != null) {
                        // 判断deviceId不一样 //发送踢出通知
                        authPublisher.publishKickOutMessage(user.getUserId(), userCacheDto);
                    }
                }
            }
        }
    }

    /**
     * 清除缓存
     *
     * @param accessToken
     */
    private void cleanCacheByAccessToken(String accessToken) {
        UserInfo loginCacheDto =
            redisUtil.get(ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken),
                UserInfo.class);
        if (loginCacheDto != null) {
            String userCacheDtoStr = redisUtil.get(
                CLIENT_USER_KEY.replace("{userId}", String.valueOf(loginCacheDto.getUserId()))
                    .replace("{clientId}", loginCacheDto.getAppKey()), String.class);
            // send heartbeat message
            redisUtil.del(CLIENT_USER_KEY.replace("{userId}", String.valueOf(loginCacheDto.getUserId()))
                .replace("{clientId}", loginCacheDto.getAppKey()));
            if (!StringUtils.isNullBlank(userCacheDtoStr)) {
                OAuthUserCacheDto userCacheDto = JSONObject.parseObject(userCacheDtoStr, OAuthUserCacheDto.class);
                redisUtil.del(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", userCacheDto.getRefreshToken()));
            }
        }
        redisUtil.del(ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken));
        redisUtil.set(ACCESS_TOKEN_TIMEOUT_KEY.replace("{access_token}", accessToken), loginCacheDto, 60 * 2L,
            TimeUnit.SECONDS);
    }

    /**
     * 登录时 存储用户对象
     */
    private OAuthLoginDto saveUserToken(ImUserDO imUserDO, OAuthLoginInfo loginInfo, Application application) {

        Long userId = imUserDO.getId();
        String clientId = application.getAppKey();
        String deviceId = loginInfo.getDeviceId();
        String refreshToken = this.getSecureRandom();
        String accessToken = this.getSecureRandom();

        // 用户默认加上自己部门的数据权限
        OrgPrivDto privDto = new OrgPrivDto();
        privDto.setId(imUserDO.getDepartmentId());
        privDto.setCode(imUserDO.getDepartmentCode());
        privDto.setName(imUserDO.getDepartmentName());
        List<OrgPrivDto> orgPrivDtos = new ArrayList<>();
        orgPrivDtos.add(privDto);
        RoleDto firstRole = roleService.getFirstRole(userId);
        if (firstRole == null) {
            // 没有角色抛出异常提示
            log.warn("roles empty:{} {} {}", imUserDO, loginInfo, application);
            throw new OAuthException(COMMON_ERROR_133.getCode(), I18nUtil.get(COMMON_ERROR_133.getMsg()),
                    "未查询到该用户没有角色信息");
        }
        boolean isRoleNoAuth = CollectionUtils.isEmpty(firstRole.getOrgPrivList());
        List<RoleDto> roles = roleService.getRoleWithDataPriv(userId, orgPrivDtos, true);
        if (application.isAdmin() && roles.stream()
            .noneMatch(a -> a.getAdminPrivJson() != null && !a.getAdminPrivJson().isEmpty())) {
            log.warn("admin empty:{} {} {}", imUserDO, loginInfo, application);
            throw new OAuthException(COMMON_ERROR_133.getCode(), I18nUtil.get(COMMON_ERROR_133.getMsg()),
                "未查询到该用户没有权限信息");
        } else if (application.isCapp() && roles.stream()
            .noneMatch(a -> a.getCappPrivJson() != null && !a.getCappPrivJson().isEmpty())) {
            log.warn("capp empty:{} {} {}", imUserDO, loginInfo, application);
            throw new OAuthException(COMMON_ERROR_133.getCode(), I18nUtil.get(COMMON_ERROR_133.getMsg()),
                "未查询到该用户没有权限信息");
        } else if (application.isIcc() && roles.stream()
            .noneMatch(a -> a.getIccPrivJson() != null && !a.getIccPrivJson().isEmpty())) {
            log.warn("icc empty:{} {} {}", imUserDO, loginInfo, application);
            throw new OAuthException(COMMON_ERROR_133.getCode(), I18nUtil.get(COMMON_ERROR_133.getMsg()),
                "未查询到该用户没有权限信息");
        }
        // 用户缓存对象
        OAuthUserCacheDto userCacheDto = new OAuthUserCacheDto();
        userCacheDto.setUserId(userId);
        userCacheDto.setAccessToken(accessToken);
        userCacheDto.setRefreshToken(refreshToken);
        userCacheDto.setOrganizationId(imUserDO.getDepartmentId());
        userCacheDto.setExecutorId(imUserDO.getId());
        List<String> roleNames = roles.stream().map(RoleDto::getName).collect(Collectors.toList());
        String role = StringUtils.join(roleNames, ",");
        userCacheDto.setRole(role);
        userCacheDto.setImOrgPrivs(roles.get(0).getImOrgPrivJson());
        redisUtil.set(CLIENT_USER_KEY.replace("{userId}", String.valueOf(userId)).replace("{clientId}", clientId),
            userCacheDto);
        // 登录缓存对象
        List<OrgPrivDto> orgPrivList = roles.get(0).getOrgPrivList();
        List<Long> orgPrivIds = new ArrayList<>();
        List<String> orgPrivCodes = new ArrayList<>();
        for (OrgPrivDto orgPrivDto : orgPrivList) {
            if (StringUtils.isNotBlank(orgPrivDto.getCode())) {
                orgPrivCodes.add(orgPrivDto.getCode());
            }
            if (orgPrivDto.getId() != null) {
                orgPrivIds.add(orgPrivDto.getId());
            }
        }
        var loginCacheDto = UserInfo.builder().userId(userId).appKey(application.getAppKey())
            .deviceId(deviceId).userName(imUserDO.getName()).executorCode(imUserDO.getCode()).createTime(new Date())
            .userPeriod(null).loginIp(loginInfo.getLoginIp()).isAdmin(Objects.equals(imUserDO.getType(), 0))
            .executorId(imUserDO.getId()).organizationId(imUserDO.getDepartmentId())
            .organizationCode(imUserDO.getDepartmentCode()).hasChildOrgPriv(0)
            .imOrgPrivIds(orgPrivIds).imOrgPrivCodes(orgPrivCodes).imOrgPrivs(roles.get(0).getImOrgPrivJson())
            .isRoleNoAuth(isRoleNoAuth)
            .idCardNum(imUserDO.getIdCard()).build();

        int tokenExpire = this.getTokenExpireCache();
        int refreshTokenExpire = this.getRefreshTokenExpireCache();

        redisUtil.set(ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken), loginCacheDto,
            tokenExpire * 60 * 60L, TimeUnit.SECONDS);
        redisUtil.set(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", refreshToken), loginCacheDto,
            refreshTokenExpire * 60 * 60 * 24L, TimeUnit.SECONDS);

        // permissions
        permissionService.getPermissions(imUserDO, roles, application);

        // 记录用户登录审计日志 设备唯一标识
        userLogService.writeToUserLog(imUserDO.getId(), loginInfo.getLoginIp(), UserLogEnums.LOGIN);

        OAuthLoginDto authLoginDto = new OAuthLoginDto();
        authLoginDto.setUserName(loginCacheDto.getUserName());
        authLoginDto.setAccessToken(accessToken);
        authLoginDto.setUserId(String.valueOf(imUserDO.getId()));
        authLoginDto.setExpireIn(tokenExpire * 60 * 60);
        authLoginDto.setRefreshToken(refreshToken);
        authLoginDto.setScope(loginInfo.getClientId());
        authLoginDto.setTokenType(loginInfo.getGrantType());
        authLoginDto.setRefreshTokenExpireIn(refreshTokenExpire * 60 * 60 * 24);
        authLoginDto.setIdCardNum(imUserDO.getIdCard());
        authLoginDto.setImOrgPrivs(roles.get(0).getImOrgPrivJson());
        authLoginDto.setIsAdmin(loginCacheDto.isAdmin());
        authLoginDto.setRoles(roles);
        authLoginDto.setIsRoleNoAuth(isRoleNoAuth);
        return authLoginDto;
    }

    /**
     * 登出
     *
     * @param accessToken
     */
    @Override
    @LogReport(type = OperationTypeEnum.LOGOUT)
    public void oAuthLogout(@LogReportParam(field = "userName") UserInfo user, String accessToken, String ip) {
        this.cleanCacheByAccessToken(accessToken);
        log.info("01=========user:{}========", user);
        if (user != null) {
            // 清楚缓存权限
            Long userId = user.getUserId();
            redisUtil.del(AuthConstants.ACCESSS_PERMISSION + userId);
            // 记录登出日志
            userLogService.writeToUserLog(userId, ip, UserLogEnums.LOGOUT);
            redisUtil.del(AuthConstants.ON_LINE_USER + userId);
        }
    }

    /**
     * token刷新
     *
     * @param authRefreshInfo
     * @return
     */
    @Override
    public OAuthRefreshDto oAuthRefreshToken(OAuthRefreshInfo authRefreshInfo) {
        // log.info("oAuthRefreshToken authRefreshInfo:{}", authRefreshInfo);
        OAuthRefreshDto authRefreshDto = new OAuthRefreshDto();
        String newAccessToken = "";
        String refreshToken = authRefreshInfo.getRefreshToken();
        String clientId = authRefreshInfo.getClientId();
        String clientSecret = authRefreshInfo.getClientSecret();
        this.validateClient(clientId, clientSecret);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new OAuthException(COMMON_ERROR_116.getCode(), I18nUtil.get(COMMON_ERROR_116.getMsg()));
        }
        UserInfo loginCacheDto =
            redisUtil.get(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", refreshToken),
                UserInfo.class);
        if (loginCacheDto != null) {
            String userCacheDtoStr = redisUtil.get(
                CLIENT_USER_KEY.replace("{userId}", String.valueOf(loginCacheDto.getUserId()))
                    .replace("{clientId}", loginCacheDto.getAppKey()), String.class);
            long remainTime = redisUtil.ttl(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", refreshToken));
            if (remainTime <= 2L * this.getTokenExpireCache() * 60L * 60L) {
                // log.info("refreshToken refresh refreshToken is:{}", refreshToken);
                // 生成新的refresh_token
                refreshToken = this.getSecureRandom();
                redisUtil.set(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", refreshToken), loginCacheDto,
                    this.getRefreshTokenExpireCache() * 60L * 60L * 24L, TimeUnit.SECONDS);
            }
            if (!StringUtils.isNullBlank(userCacheDtoStr)) {
                OAuthUserCacheDto userCacheDto = JSONObject.parseObject(userCacheDtoStr, OAuthUserCacheDto.class);
                String accessToken = userCacheDto.getAccessToken();
                redisUtil.del(ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken));
                redisUtil.set(ACCESS_TOKEN_TIMEOUT_KEY.replace("{access_token}", accessToken), loginCacheDto, 60 * 2,
                    TimeUnit.SECONDS);
                newAccessToken = this.getSecureRandom();
                userCacheDto.setRefreshToken(refreshToken);
                userCacheDto.setAccessToken(newAccessToken);
                redisUtil.set(CLIENT_USER_KEY.replace("{userId}", String.valueOf(loginCacheDto.getUserId()))
                    .replace("{clientId}", loginCacheDto.getAppKey()), userCacheDto);
                redisUtil.set(ACCESS_TOKEN_USER_KEY.replace("{access_token}", newAccessToken), loginCacheDto,
                    this.getTokenExpireCache() * 60L * 60L, TimeUnit.SECONDS);
            }
            // 发布token刷新通知，向cagent发送
            TokenRefreshMsg msg = new TokenRefreshMsg();
            msg.setOldToken(authRefreshInfo.getAuthorization());
            msg.setNewToken(newAccessToken);
            authPublisher.publishRefreshTokenMessage(authRefreshInfo.getAuthorization(), msg);
        } else {
            throw new OAuthException(COMMON_ERROR_116.getCode(), I18nUtil.get(COMMON_ERROR_116.getMsg()));
        }

        authRefreshDto.setAccessToken(newAccessToken);
        log.info("old_token:{} ============> refresh_token is:{}", authRefreshInfo.getAuthorization(), newAccessToken);
        authRefreshDto.setExpireIn(this.getTokenExpireCache() * 60 * 60);
        authRefreshDto.setRefreshToken(refreshToken);
        long refreshTokenExpireIn = redisUtil.ttl(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", refreshToken));
        authRefreshDto.setRefreshTokenExpireIn((int)refreshTokenExpireIn);
        authRefreshDto.setTokenType(authRefreshInfo.getGrantType());
        return authRefreshDto;
    }

    @Override
    public void validTokens() {
        log.info("========================== validTokens ==========================");
        Set<String> tokenKeys = redisUtil.keys(ACCESS_TOKEN_USER_KEY.replace("{access_token}", "*"));
        for (String tokenKey : tokenKeys) {
            UserInfo loginCacheDto =
                redisUtil.get(tokenKey, UserInfo.class);
            if (loginCacheDto != null) {
                String userCacheStr = redisUtil.get(CLIENT_USER_KEY.replace("{userId}", loginCacheDto.getUserId() + "")
                    .replace("{clientId}", loginCacheDto.getAppKey()), String.class);
                if (!StringUtils.isNullBlank(userCacheStr)) {
                    OAuthUserCacheDto userCacheDto = JSONObject.parseObject(userCacheStr, OAuthUserCacheDto.class);
                    String accessToken = userCacheDto.getAccessToken();
                    String[] tokenPath = tokenKey.split(":");
                    if (!accessToken.equals(tokenPath[3])) {
                        redisUtil.del(tokenKey);
                        redisUtil.set(ACCESS_TOKEN_TIMEOUT_KEY.replace("{access_token}", tokenPath[3]), loginCacheDto,
                            60 * 2L, TimeUnit.SECONDS);
                        continue;
                    }
                    String refreshToken = userCacheDto.getRefreshToken();
                    // 判断是否发送过消息
                    String willTimeOut =
                        redisUtil.get(ACCESS_TOKEN_WILL_TIMEOUT_KEY.replace("{access_token}", accessToken),
                            String.class);
                    if (StringUtils.isNullBlank(willTimeOut)) {
                        // 返回为秒
                        Long timeout = redisUtil.ttl(ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken));
                        // log.info("token " + tokenKey + " expire time is " + timeout + " seconds");
                        Integer tokenExpireCache = this.getTokenExpireCache();
                        if (0L < timeout && timeout <= tokenExpireCache * 60L * 5L) {
                            redisUtil.set(ACCESS_TOKEN_WILL_TIMEOUT_KEY.replace("{access_token}", accessToken),
                                timeout + "", timeout * 1000L, TimeUnit.MILLISECONDS);
                            authPublisher.publishTokenWillTimeoutMessage(accessToken, NOTIFY_TWELFTH_TIMEOUT,
                                userCacheDto);
                        }
                        if (tokenExpireCache * 60L * 5L < timeout && timeout <= tokenExpireCache * 60L * 12L) {
                            redisUtil.set(ACCESS_TOKEN_WILL_TIMEOUT_KEY.replace("{access_token}", accessToken),
                                timeout + "", timeout * 1000L, TimeUnit.MILLISECONDS);
                            // 过期时间还有1/8
                            authPublisher.publishTokenWillTimeoutMessage(accessToken, NOTIFY_EIGHTH_TIMEOUT,
                                userCacheDto);
                        }
                    }
                    // 判断refreshToken是否过期
                    String refreshStr =
                        redisUtil.get(REFRESH_TOKEN_USER_KEY.replace("{refresh_token}", refreshToken), String.class);
                    if (StringUtils.isNullBlank(refreshStr)) {
                        // 发布下线通知
                        authPublisher.publishOfflineMessage(accessToken, userCacheDto);
                        // 清空用户登录缓存信息
                        redisUtil.del(ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken));
                        // refreshToken过期则 永久存储的user对象没作用了
                        redisUtil.del(CLIENT_USER_KEY.replace("{userId}", loginCacheDto.getUserId() + "")
                            .replace("{clientId}", loginCacheDto.getAppKey()));
                        // 去除用户对象
                        redisUtil.set(ACCESS_TOKEN_TIMEOUT_KEY.replace("{access_token}", accessToken), loginCacheDto,
                            60L * 2, TimeUnit.SECONDS);
                    }
                }
            }
        }
    }

    /**
     * 心跳检测接口
     *
     * @param accessToken
     */
    @Override
    public R oauthKeepalive(String accessToken) {
        UserInfo loginCacheDto =
            redisUtil.get(ACCESS_TOKEN_USER_KEY.replace("{access_token}", accessToken),
                UserInfo.class);
        if (loginCacheDto == null) {
            throw new UnAuthException("CommonErrorEnum_COMMON_ERROR_118");
        }
        JSONObject result = new JSONObject();

        result.put("time", System.currentTimeMillis());

        // 更新在线用户 过期时间
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            redisUtil.expire(AuthConstants.ON_LINE_USER + user.getUserId(),
                Duration.ofSeconds(AuthConstants.ON_LINE_USER_EXPIRATION_TIME));
        }
        return R.success(result);
    }

    private Integer getRefreshTokenExpireCache() {
        int refreshTokenExpire = 90;
        String refreshTokenExpireStr = redisUtil.get(AUTH_REFRESHTOKEN_EXPIRE_IN, String.class);
        if (!StringUtils.isNullBlank(refreshTokenExpireStr)) {
            refreshTokenExpire = Integer.parseInt(refreshTokenExpireStr);
        }
        return refreshTokenExpire;
    }

    private Integer getTokenExpireCache() {
        int tokenExpire = 24;
        String tokenExpireStr = redisUtil.get(AUTH_TOKEN_EXPIRE_IN, String.class);
        if (!StringUtils.isNullBlank(tokenExpireStr)) {
            tokenExpire = Integer.parseInt(tokenExpireStr);
        }
        return tokenExpire;
    }

    // SecureRandom生成24字节随机数
    public String getSecureRandom() {
        SecureRandom ng = numberGenerator;
        byte[] randomBytes = new byte[24];
        ng.nextBytes(randomBytes);
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < randomBytes.length; i++) {
            String hex = Long.toHexString(0xff & randomBytes[i]);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    @LogReport(type = OperationTypeEnum.UPDATE_PASSWORD)
    public void changePassword(@LogReportParam(field = "username") OAuthPwdDto pwdDto, String ip) {
        UserInfo user;
        var token = SecurityUtils.getToken();
        if ((user = SecurityUtils.getUser()) == null && (token == null || (user =
            redisUtil.get(ACCESS_TMP_TOKEN_USER_KEY.replace("{access_token}", token),
                UserInfo.class)) == null)) {
            throw new OAuthException(COMMON_ERROR_113.getCode(), I18nUtil.get(COMMON_ERROR_113.getMsg()));
        }
        if (!StringUtils.isNullBlank(redisUtil.get(user.getUserName()))) {// 有值说明被限制修改
            throw new OAuthException(CommonErrorEnum.COMMON_ERROR_136.getCode(),
                I18nUtil.get(CommonErrorEnum.COMMON_ERROR_136.getMsg()));
        }
        var userDetails = imUserMapper.selectById(user.getUserId());

        userLogService.writeToUserLog(user.getUserId(), ip, UserLogEnums.UPDATAPWD);
        // 旧密码输入错误 1分钟内重试5次 该账号锁定密码修改10分钟
        var passwordErrorCounter =
            redisUtil.get(PASSWORD_ERROR_COUNTER + userDetails.getId(), PasswordErrorCounter.class);
        var unLockTime = redisUtil.get(AUTH_AUTO_UNLOCK_TIME, Integer.class);
        unLockTime = unLockTime == null ? 10 : unLockTime;
        Integer errorCount = redisUtil.get(AUTH_LOGIN_ERROR_NUM_LIMIT, Integer.class);
        errorCount = errorCount == null ? MAX_ERROR_COUNT : errorCount;
        var now = System.currentTimeMillis();
        if (passwordErrorCounter != null && passwordErrorCounter.shouldBlock(unLockTime, errorCount, now)) {// 锁了 不验密码
            long seconds = (passwordErrorCounter.getTime() + unLockTime * 60L * 1000L - now) / 1000;
            String data = String.format(I18nUtil.get("USERLOGINLOCKED"), seconds);
            throw new OAuthException(COMMON_ERROR_112.getCode(), I18nUtil.get(COMMON_ERROR_112.getMsg()), data);
        } else if (!PwdUtil.checkPwdMatches(userDetails.getPassword(), pwdDto.getOldPassword())) {
            if (passwordErrorCounter == null) {
                passwordErrorCounter = new PasswordErrorCounter().setCount(1).setTime(now);
                redisUtil.set(PASSWORD_ERROR_COUNTER + userDetails.getId(), passwordErrorCounter);
                redisUtil.set(PASSWORD_ERROR_COUNTER_DELAY + userDetails.getId(), passwordErrorCounter);
            } else {
                passwordErrorCounter.increaseCount().setTime(now);
                redisUtil.set(PASSWORD_ERROR_COUNTER + userDetails.getId(), passwordErrorCounter);
                redisUtil.set(PASSWORD_ERROR_COUNTER_DELAY + userDetails.getId(), passwordErrorCounter);
            }

            int remainingAttempts = errorCount - passwordErrorCounter.getCount();
            String errorMsg;
            if (remainingAttempts > 0) {
                errorMsg = I18nUtil.get(COMMON_ERROR_134.getMsg()) + "，还剩 " + remainingAttempts + " 次尝试机会";
            } else {
                errorMsg = I18nUtil.get(COMMON_ERROR_134.getMsg());
            }
            throw new OAuthException(COMMON_ERROR_134.getCode(), errorMsg);
        } else {
            redisUtil.del(PASSWORD_ERROR_COUNTER + userDetails.getId());
            redisUtil.del(PASSWORD_ERROR_COUNTER_DELAY + userDetails.getId());
        }
      /*  log.info("{}:{}", userDetails.getPassword(), pwdDto.getOldPassword());
        if (Objects.equals(pwdDto.getOldPassword(), pwdDto.getNewPassword())) {
            throw new OAuthException(COMMON_ERROR_123.getCode(), I18nUtil.get(COMMON_ERROR_123.getMsg()));
        }*/
        // 校验密码非空
        PwdUtil.checkPwdNotEmpty(pwdDto.getNewPassword());
        // check new pwd
        String allow_simple_password = globalsRpcService.getGlobalsValueByName(ALLOW_SIMPLE_PASSWORD);
        if (Integer.parseInt(allow_simple_password) == 0) {
            PwdUtil.checkPwdFormat(pwdDto.getNewPassword(), userDetails.getIdCard());
        }
        String newPassword;
        try {
            newPassword = PBKDF2Util.PBKDF2ForPassStandard(pwdDto.getNewPassword(), PBKDF2Util.generateSalt());
        } catch (NoSuchAlgorithmException e) {
            log.info("get salt fail");
            throw new OAuthException(COMMON_ERROR_120.getCode(), I18nUtil.get(COMMON_ERROR_120.getMsg()));
        }

        String historyLimitConfig = globalsRpcService.getGlobalsValueByName(AUTH_PWD_REPETITION_COUNT);
        int historyLimit = StringUtils.isEmpty(historyLimitConfig) ? 3 : Integer.parseInt(historyLimitConfig);
        // 校验新密码是否与最近N次历史密码重复
        historyPwdUtils.checkHistoryPassword(userDetails.getId(), pwdDto.getNewPassword(), historyLimit);

        userDetails.setPassword(newPassword);
        userDetails.setPwdTime(new Date());
        // 数据同步到es
        if (encryptionService.encryptEnabled()) {
            imUserEsService.updateByIds(Collections.singletonList(userDetails));
        }
        imUserMapper.updateById(userDetails);
        updateAdminUser(userDetails.getId(), newPassword);
        redisUtil.del(ACCESS_TMP_TOKEN_USER_KEY.replace("{access_token}", token));
        redisUtil.sRem(RESET_PASSWORD_USERS, userDetails.getId());
        historyPwdUtils.saveHistoryPassword(userDetails.getId(), newPassword, historyLimit);
    }

    private void updateAdminUser(Long userId, String pwd) {
        try {
            UserAdmin userAdmin = new UserAdmin();
            userAdmin.setId(userId);
            userAdmin.setPassword(PBKDF2Util.PBKDF2ForPassStandard(pwd, PBKDF2Util.generateSalt()));
            userAdminMapper.updatePwdById(userAdmin);
        } catch (Exception e) {
            log.error("修改密码同步修改adminUser失败", e);
        }
    }

    @Override
    public String getLoginMessage() {
        String globalValue = globalsRpcService.getGlobalsValueByName("LOGIN_EXTENSION_PROPERTIES");
        return extendInfoPropertiesRpcService.getExtendInfoPropertiesLabelByName(globalValue);
    }

    /**
     * 定时擦除密码锁定告警
     */
    @Scheduled(fixedDelay = 10000L, initialDelay = 20000L)
    public void scheduledCheckUserLockStatus() {
        Set<String> keys = redisUtil.keys(PASSWORD_ERROR_COUNTER_DELAY + "*");
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        log.info("scheduledCheckUserLockStatus keys: {}", keys);
        for (String key : keys) {
            PasswordErrorCounter passwordErrorCounter = redisUtil.get(key, PasswordErrorCounter.class);
            log.info("passwordErrorCounter key:{}, passwordErrorCounter: {}", key, passwordErrorCounter);

            var unLockTime = redisUtil.get(AUTH_AUTO_UNLOCK_TIME, Integer.class);
            unLockTime = unLockTime == null ? 10 : unLockTime;
            Integer errorCount = redisUtil.get(AUTH_LOGIN_ERROR_NUM_LIMIT, Integer.class);
            errorCount = errorCount == null ? MAX_ERROR_COUNT : errorCount;
            var now = System.currentTimeMillis();
            Integer count = passwordErrorCounter.getCount();
            Long time = passwordErrorCounter.getTime();
            // 超过最大错误次数
            boolean isBigThanMaxCount = count != null && count >= errorCount;
            // 到了释放时间
            boolean isFreeTime = time != null && now > time + unLockTime * 60L * 1000L;
            if (isBigThanMaxCount && isFreeTime) {
                Long userId = Long.valueOf(key.replace(PASSWORD_ERROR_COUNTER_DELAY, ""));
                clearAlarm(userId);
                // 删除缓存，不再重复处理
                redisUtil.del(key);
            }
        }
    }

    private AlarmTemplate getAlarmTemplateEnum(Long userId) {
        var roles = roleService.getRoleInfoListByUserId(userId, false);
        if (CollectionUtils.isEmpty(roles)) {
            log.error("getAlarmTemplateEnum error roles is null");
            return null;
        }
        // 超级管理员
        Optional<RoleDto> superAdmin = roles.stream().filter(role -> 0 == role.getType()).findAny();
        if (superAdmin.isPresent()) {
            return AlarmTemplateZhEnum.SUPER_ADMINISTRATOR_ACCOUNT_LOCKOUT;
        }
        return null;

//        // 一般管理员暂时不告警了
//        // 管理员
//        Optional<Role> admin = roles.stream().filter(role -> 1 == role.getType()).findAny();
//        if (admin.isPresent()) {
//            return AlertDefineEnum.ADMINISTRATOR_ACCOUNT_LOCKOUT;
//        }
//        // 系统账号告警调整为警信虚拟账号密码错误告警
//        // 系统账号
//        return AlertDefineEnum.SYSTEM_ACCOUNT_LOCKOUT;
    }
}