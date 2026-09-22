package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.service.IIMUserRPCService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.LinkxAuthUserDO;
import com.tdtech.cloudcmd.im.jingxin.server.enums.Constant;
import com.tdtech.cloudcmd.im.jingxin.server.enums.LogTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.excel.DutyScheduleExcel;
import com.tdtech.cloudcmd.im.jingxin.server.excel.IMUserExcel;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.DutyTypeService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IDutyScheduleService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ImCommonService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.DutyScheduleMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.LinkxAuthUserMapper;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.PinyinUtils;
import com.tdtech.cloudcmd.util.PBKDF2Util;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ly
 * @date 2025/8/25
 **/
@Slf4j
@Service
public class DutyScheduleServiceImpl extends ServiceImpl<DutyScheduleMapper, DutySchedule>
        implements IDutyScheduleService {

    /** 正则：yyyy-MM-dd */
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    /** 正则：HH:mm */
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private static final String TEMPLATE_FILE_NAME = "xx年xx月值班信息模板";

    /** 新建用户默认密码（明文），与 auth 侧 OAuthLoginServiceImpl.DEFAULT_PASSWORD 保持一致 */
    private static final String DEFAULT_PASSWORD = "Aa@123456";

    private static final String ALL_USER_KEY = "cloudcmd:duty:schedule:user:all";

    private static final String ALL_USER_V2_KEY = "cloudcmd:im:user:all";

    /** 用户拼音索引：userId -> [fullPinyin(小写), headChar(小写)]，随 ALL_USER_V2_KEY 一起刷新 */
    private static final String ALL_USER_PINYIN_INDEX_KEY = "cloudcmd:im:user:pinyin:index";

    private static final int USER_INFO_QUERY_BATCH_SIZE = 50;

    private static final int DUTY_SCHEDULE_INSERT_BATCH_SIZE = 1000;

    private static final int USER_IM_UPSERT_BATCH_SIZE = 200;

    private static final int DUTY_TYPE_DROPDOWN_COLUMN_INDEX = 6;

    private static final int DUTY_TYPE_DROPDOWN_FIRST_ROW_INDEX = 2;

    private static final int DUTY_TYPE_DROPDOWN_LAST_ROW_INDEX = 1000;

    @Resource
    private DutyScheduleMapper dutyScheduleMapper;

    @Resource
    private IdWorker idWorker;

    @Resource
    private ImService imService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private LinkxAuthUserMapper linkxAuthUserMapper;

    @Autowired
    private EncryptionService encryptionService;

    @Resource
    private CollaborationPostService collaborationPostService;

    @Resource
    private FileUtil fileUtil;

    @Resource
    private ImHttpClient imHttpClient;

    @Resource
    private ImCommonService imCommonService;

    @Resource
    private ReportUtil reportUtil;

    @Resource
    private DutyTypeService dutyTypeService;

    @DubboReference
    private IIMUserRPCService imUserRPCService;

    @Override
    public void template(HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(Charsets.UTF_8.name());
        String fileName = URLEncoder.encode(TEMPLATE_FILE_NAME, Charsets.UTF_8.name());
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        InputStream tpl = getClass().getResourceAsStream("/template/duty_template.xlsx");
        if (tpl == null)
            throw new BusinessException("值班模板不存在！");


        List<IMUserExcel> imUserExcels = getAllUserFromCache();
        UserInfo user = SecurityUtils.getUser();
        if (user != null && user.isAdmin()) {
            // 查询部门id进行过滤
            List<Long> orgByUserId = imUserRPCService.findOrgByUserId(user.getUserId());
            if(CollectionUtils.isNotEmpty(orgByUserId)){
                imUserExcels = imUserExcels.stream().filter(imUserExcel ->
                        orgByUserId.contains(Long.valueOf(imUserExcel.getDeptId()))).collect(Collectors.toList());
            }
        }

        List<DutyType> dutyTypes = dutyTypeService.getAllList();
        InputStream templateWithDropdown = buildTemplateWithDutyTypeDropdown(tpl, dutyTypes);

        // 模板只保留主 sheet 和警信用户 sheet，排班类型在主 sheet 中通过下拉框选择。
        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).withTemplate(templateWithDropdown)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).build()) {
            WriteSheet userSheet = EasyExcel.writerSheet(1).head(IMUserExcel.class).build();
            excelWriter.write(imUserExcels, userSheet);
        }
    }

    private InputStream buildTemplateWithDutyTypeDropdown(InputStream tpl, List<DutyType> dutyTypes) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(tpl);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int dutyTypeSheetIndex = workbook.getSheetIndex("排班类型");
            if (dutyTypeSheetIndex >= 0) {
                workbook.removeSheetAt(dutyTypeSheetIndex);
            }

            Sheet mainSheet = workbook.getSheetAt(0);
            // 去除默认类型，默认只是为了存量数据的适配，新创建的排班数据不会使用默认类型
            String[] dutyTypeNames = dutyTypes.stream()
                    .filter(dutyType -> Objects.nonNull(dutyType)
                            && !Objects.equals(dutyType.getType(), Constant.DEFAULT_DUTY_TYPE_ID)
                            && !Constant.DEFAULT_DUTY_TYPE_NAME.equals(dutyType.getName()))
                    .map(DutyType::getName)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .toArray(String[]::new);
            if (dutyTypeNames.length > 0) {
                DataValidationHelper helper = mainSheet.getDataValidationHelper();
                DataValidationConstraint constraint = helper.createExplicitListConstraint(dutyTypeNames);
                CellRangeAddressList range = new CellRangeAddressList(DUTY_TYPE_DROPDOWN_FIRST_ROW_INDEX,
                        DUTY_TYPE_DROPDOWN_LAST_ROW_INDEX, DUTY_TYPE_DROPDOWN_COLUMN_INDEX, DUTY_TYPE_DROPDOWN_COLUMN_INDEX);
                DataValidation validation = helper.createValidation(constraint, range);
                validation.setShowErrorBox(true);
                mainSheet.addValidationData(validation);
            }

            workbook.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    private List<IMUserExcel> getAllUserFromCache() {
        List<IMUserExcel> imUserExcels = redisUtil.get(ALL_USER_KEY, new TypeReference<>() {
        });
        if (CollectionUtils.isEmpty(imUserExcels)) {
            log.info("getAllUserFromCache is empty");
            List<IMUserExcel> resultList = getAllUser();
            if (CollectionUtils.isNotEmpty(resultList)) {
                redisUtil.set(ALL_USER_KEY, resultList);
            }
        }
        return redisUtil.get(ALL_USER_KEY, new TypeReference<>() {
        });
    }

    private List<IMUserExcel> getAllUser() {
        List<ImUser> imUsers = imService.queryUser(null, 1, null, null, null);
        if(CollectionUtils.isNotEmpty(imUsers)){
            redisUtil.set(ALL_USER_V2_KEY, imUsers);
            // 构建拼音索引（全拼 + 首字母），随全量用户缓存一起刷新，供用户检索时 O(1) 命中
            Map<String, String[]> pinyinIndex = new HashMap<>();
            for (ImUser user : imUsers) {
                String userName = user.getName();
                if (StringUtils.isBlank(userName)) {
                    continue;
                }
                try {
                    String[] pinyinArr = PinyinUtils.getPinyinAndHead(userName);
                    String fullPinyin = PinyinUtils.normalize(pinyinArr[0]).toLowerCase();
                    String headChar = PinyinUtils.normalize(pinyinArr[1]).toLowerCase();
                    pinyinIndex.put(String.valueOf(user.getId()), new String[]{fullPinyin, headChar});
                } catch (Exception e) {
                    log.warn("构建拼音索引失败, userId={}, name={}, error={}", user.getId(), userName, e.getMessage());
                }
            }
            redisUtil.set(ALL_USER_PINYIN_INDEX_KEY, pinyinIndex);
//            persistUserImBatch(imUsers); 需求下车 屏蔽同步im用户信息代码
        }

        List<IMUserExcel> excelList = imUsers.stream().map(user -> {
            IMUserExcel excel = new IMUserExcel();
            List<ImUser.UserDepartment> userDepartments = user.getUserDepartments();
            if (userDepartments != null && !userDepartments.isEmpty()) {
                userDepartments.stream().filter(ImUser.UserDepartment::getIsPrimary).findAny()
                        .ifPresent(dep -> {
                            excel.setDeptName(dep.getDepartmentName());
                            excel.setDeptId(String.valueOf(dep.getId()));
                        });
            }
            excel.setId(String.valueOf(user.getId()));
            excel.setName(user.getName());

            return excel;
        }).collect(Collectors.toList());
        // 批量设置所属协同岗名称
        Lists.partition(excelList, 1000).forEach(list -> {
            List<String> userIdList = list.stream().map(IMUserExcel::getId).collect(Collectors.toList());
            Map<Long, List<CollaborationPost>> longListMap = collaborationPostService.buildUserIdListMap(userIdList);
            if(MapUtils.isNotEmpty(longListMap)){
                list.forEach(user -> {
                    List<CollaborationPost> posts = longListMap.get(Long.valueOf(user.getId()));
                    if(CollectionUtils.isNotEmpty(posts)){
                        user.setPostName(posts.stream().map(CollaborationPost::getPostName).collect(Collectors.joining(",")));
                    }
                });
            }
        });

        return excelList;
    }

    private void persistUserImBatch(List<ImUser> imUsers) {
        if (CollectionUtils.isEmpty(imUsers)) {
            return;
        }
        // 1) linkx_auth.tb_user_im：im-jingxin 直连副本表，自定义 upsert（手动加密敏感字段）
        List<LinkxAuthUserDO> userImList = imUsers.stream().map(this::convertToUserImDO).collect(Collectors.toList());
        executeBatches(userImList, linkxAuthUserMapper::upsertBatch, "linkx_auth.tb_user_im");
        // 2) icp_res.tb_im_user：走 auth 侧 RPC，由 auth ImUserMapper.upsertBatch 落库（手动加密），不覆盖登录态字段
        List<ImUserDto> rpcList = imUsers.stream().map(this::convertToImUserDto).collect(Collectors.toList());
        executeBatches(rpcList, imUserRPCService::upsertImUserBatch, "icp_res.tb_im_user");
    }

    private <T> void executeBatches(List<T> list, java.util.function.Consumer<List<T>> action, String target) {
        Lists.partition(list, USER_IM_UPSERT_BATCH_SIZE).forEach(batch -> {
            try {
                action.accept(batch);
            } catch (Exception e) {
                log.error("批量落库 {} 失败, batchSize={}, error={}", target, batch.size(), e.getMessage(), e);
            }
        });
    }

    private LinkxAuthUserDO convertToUserImDO(ImUser user) {
        // copyBean 按 同名字段 拷贝公共属性；主部门信息需手动补充
        LinkxAuthUserDO dto = BeanCopyUtils.copyBean(user, LinkxAuthUserDO::new);
        // upsertBatch 为自定义 XML 方法，不触发 EncryptionInnerInterceptor，敏感字段需手动加密
        dto.setName(encryptionService.encrypt(user.getName()));
        dto.setMobile(encryptionService.encrypt(user.getMobile()));
        dto.setIdCard(encryptionService.encrypt(user.getIdCard()));
        // 新建用户写入默认密码（PBKDF2 哈希）；已存在用户 upsert 不覆盖 password/pwd_time
        dto.setPassword(buildDefaultPassword());
        dto.setPwdTime(new Date());
        fillPrimaryDepartment(dto, user.getPrimaryDepartment());
        return dto;
    }

    private String buildDefaultPassword() {
        try {
            return PBKDF2Util.PBKDF2ForPassStandard(DEFAULT_PASSWORD, PBKDF2Util.generateSalt());
        } catch (Exception e) {
            log.error("生成默认密码哈希失败, error={}", e.getMessage(), e);
            throw new BusinessException("生成默认密码哈希失败");
        }
    }

    private ImUserDto convertToImUserDto(ImUser user) {
        ImUserDto dto = BeanCopyUtils.copyBean(user, ImUserDto::new);
        fillPrimaryDepartment(dto, user.getPrimaryDepartment());
        return dto;
    }

    private void fillPrimaryDepartment(LinkxAuthUserDO dto, ImUser.UserDepartment primaryDept) {
        if (primaryDept != null) {
            dto.setDepartmentCode(primaryDept.getDepartmentCode());
            dto.setDepartmentName(primaryDept.getDepartmentName());
            dto.setDepartmentId(primaryDept.getId());
        }
    }

    private void fillPrimaryDepartment(ImUserDto dto, ImUser.UserDepartment primaryDept) {
        if (primaryDept != null) {
            dto.setDepartmentCode(primaryDept.getDepartmentCode());
            dto.setDepartmentName(primaryDept.getDepartmentName());
            dto.setDepartmentId(primaryDept.getId());
        }
    }

    @Override
    public ImportDutyScheduleResult importExcel(MultipartFile file) throws IOException {
        OperationLog operationLog = buildImportOperationLog();
        ImportDutyScheduleResult result = null;
        try {
            result = doImportExcel(file);
            return result;
        } catch (IOException | RuntimeException e) {
            operationLog.setStatus(MSIPConstant.OPERATION_FAILURE);
            throw e;
        } finally {
            operationLog.setOperation(String.format(operationLog.getOperation(), getImportCount(result)));
            System.out.printf("【】】】】】】】】】】】】】】】"+operationLog.toString());
            reportUtil.saveOperationLog(operationLog);
        }
    }

    private ImportDutyScheduleResult doImportExcel(MultipartFile file) throws IOException {
        List<DutyScheduleExcel> excelList =
                EasyExcel.read(file.getInputStream()).head(DutyScheduleExcel.class).headRowNumber(2).sheet(0).doReadSync();

        log.info("importExcel excelList: {}", excelList);
        ImportDutyScheduleResult result = checkImportData(excelList);
        List<DutyScheduleExcel> successList = result.getSuccessList();

        if (CollectionUtils.isEmpty(successList)) {
            return result;
        }
        List<DutySchedule> dataList = successList.stream().map(DutySchedule::new).collect(Collectors.toList());
        List<Long> userIdList = dataList.stream().map(DutySchedule::getUserId).distinct().collect(Collectors.toList());
        Map<Long, ImUser> userId2ImUserMap = findUserInfoMap(userIdList);
        for (var ds : dataList) {
            ImUser find = userId2ImUserMap.get(ds.getUserId());
            if (find != null) {
                var primaryDepartment = find.getPrimaryDepartment();
                ds.setDepartmentId(primaryDepartment.getId());
                ds.setDepartmentName(primaryDepartment.getDepartmentName());
            }
        }
        
        // 查询现有数据，构建完整匹配Map（key: userId-dutyStartDate-dutyStartTime-dutyEndDate-dutyEndTime-dutyType）
        List<DutySchedule> oldList = findList(userIdList);
        Map<String, List<DutySchedule>> exactMatchMap = oldList.stream().collect(
                Collectors.toMap(this::buildExactMatchKey,
                        duty -> new ArrayList<>(List.of(duty)),
                        (existing, replacement) -> {
                            existing.addAll(replacement);
                            return existing;
                        }
                ));

        // 需要删除的完全重叠记录
        List<DutySchedule> need2RemoveList = dataList.stream()
                .map(data -> exactMatchMap.get(buildExactMatchKey(data)))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Date now = new Date();

        UserInfo user = SecurityUtils.getUser();
        Long userId;
        String userName;
        if (user != null) {
            userId = user.getUserId();
            userName = user.getUserName();
        } else {
            userName = null;
            userId = null;
        }

        dataList.forEach(data -> {
            var oldDutyList = exactMatchMap.get(buildExactMatchKey(data));
            DutySchedule exactDuty = null;
            if (CollectionUtils.isNotEmpty(oldDutyList)) {
                exactDuty = oldDutyList.stream().findFirst().orElse(null);
            }
            Date gmtCreated = Objects.isNull(exactDuty) ? now : exactDuty.getGmtCreated();
            // 新增处理
            data.setId(idWorker.nextId());
            data.setImportBatch(1L);
            data.setGmtCreated(gmtCreated);
            data.setImportUserId(userId);
            data.setImportUserName(userName);
            data.setGmtModified(now);
        });

        // 删除完全重叠的旧记录
        if (CollectionUtils.isNotEmpty(need2RemoveList)) {
            removeBatchByIds(need2RemoveList);
        }
        Lists.partition(dataList, DUTY_SCHEDULE_INSERT_BATCH_SIZE)
                .forEach(dutyScheduleMapper::insertBatch);

        return result;
    }

    private Map<Long, ImUser> findUserInfoMap(List<Long> userIdList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyMap();
        }

        // 用户查询接口单次最多支持 50 人，按 50 分批并发查询
        List<CompletableFuture<List<ImUser>>> futures = Lists.partition(userIdList, USER_INFO_QUERY_BATCH_SIZE).stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> {
                    List<ImUser> users = imService.findUserInfo(batch);
                    return users == null ? Collections.<ImUser>emptyList() : users;
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(ImUser::getId, user -> user, (oldUser, newUser) -> oldUser));
    }

    private int getImportCount(ImportDutyScheduleResult result) {
        if (Objects.isNull(result) || CollectionUtils.isEmpty(result.getSuccessList())) {
            return 0;
        }
        return result.getSuccessList().size();
    }

    /**
     * 构建值班信息导入操作日志。
     */
    private OperationLog buildImportOperationLog() {
        // 导入值班信息按新建类型记录操作日志
        LogTypeEnum logType = LogTypeEnum.INSERT;
        OperationLog operationLog = new OperationLog(OperationTypeEnum.DUTY_SCHEDULE_IMPORT);
        operationLog.setAction(logType.getMsg());
        UserInfo user = SecurityUtils.getUser();
        if (Objects.nonNull(user) && StringUtils.isNotBlank(user.getUserName())) {
            operationLog.setOperator(user.getUserName());
        }
        return operationLog;
    }

    private Long buildNewImportBatch(List<DutySchedule> dutyList) {
        if (CollectionUtils.isEmpty(dutyList)) {
            return 1L;
        }
        return dutyList.stream().mapToLong(DutySchedule::getImportBatch).max().getAsLong() + 1;
    }

    /**
     * 构建完全匹配的key（用于快速查找完全重叠的记录）
     *
     * @param duty 值班记录
     * @return key格式：userId-dutyStartDate-dutyStartTime-dutyEndDate-dutyEndTime-dutyType
     */
    private String buildExactMatchKey(DutySchedule duty) {
        return String.format("%s-%s-%s-%s-%s-%s",
            duty.getUserId(),
            duty.getDutyStartDate(),
            duty.getDutyStartTime(),
            duty.getDutyEndDate(),
            duty.getDutyEndTime(),
            duty.getDutyType());
    }

    private String buildExactMatchKeyFromExcel(DutyScheduleExcel excel) {
        return String.format("%s-%s-%s-%s-%s-%s",
                excel.getUserId(),
                excel.getDutyStartDate(),
                excel.getDutyStartTime(),
                excel.getDutyEndDate(),
                excel.getDutyEndTime(),
                excel.getDutyType());
    }

    @Override
    public List<DutySchedule> findList(List<Long> userIdList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DutySchedule> queryWrapper =
                new LambdaQueryWrapper<DutySchedule>().in(DutySchedule::getUserId, userIdList);
        return list(queryWrapper);
    }

    @Override
    public List<DutyScheduleVO> listDutyScheduleUsers(List<Long> userIdList, LocalDateTime dutyStartDate,
                                                      LocalDateTime dutyEndDate, Long dutyType) {
        // 参数校验：用户ID列表为空则直接返回
        if (CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyList();
        }
        // 构建查询条件：按用户ID列表、排班类型过滤，按日期、开始时间、用户ID排序
        // 时间范围条件在下方通过时间级精确过滤实现（支持跨天值班）
        LambdaQueryWrapper<DutySchedule> queryWrapper = new LambdaQueryWrapper<DutySchedule>()
                .in(DutySchedule::getUserId, userIdList)
                .eq(Objects.nonNull(dutyType), DutySchedule::getDutyType, dutyType)
                .orderByAsc(DutySchedule::getDutyStartDate, DutySchedule::getDutyStartTime, DutySchedule::getUserId);
        // 时间范围条件（查询在指定时间范围内正在值班的人员，支持跨天值班）
        // 区间重叠条件：排班结束时间 >= 查询开始时间 AND 排班开始时间 <= 查询结束时间
        if (dutyStartDate != null) {
            LocalDate startDate = dutyStartDate.toLocalDate();
            LocalTime startTime = dutyStartDate.toLocalTime();
            // 条件：dutyEndDate > startDate OR (dutyEndDate = startDate AND dutyEndTime >= startTime)
            queryWrapper.and(wrapper -> wrapper
                    .gt(DutySchedule::getDutyEndDate, startDate)
                    .or(w -> w.eq(DutySchedule::getDutyEndDate, startDate)
                            .ge(DutySchedule::getDutyEndTime, startTime))
            );
        }
        if (dutyEndDate != null) {
            LocalDate endDate = dutyEndDate.toLocalDate();
            LocalTime endTime = dutyEndDate.toLocalTime();
            // 条件：dutyStartDate < endDate OR (dutyStartDate = endDate AND dutyStartTime <= endTime)
            queryWrapper.and(wrapper -> wrapper
                    .lt(DutySchedule::getDutyStartDate, endDate)
                    .or(w -> w.eq(DutySchedule::getDutyStartDate, endDate)
                            .le(DutySchedule::getDutyStartTime, endTime))
            );
        }

        // 执行查询
        List<DutySchedule> dataList = dutyScheduleMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        // 获取排班类型映射表（用于设置排班类型名称）
        Map<Long, DutyType> dutyTypeMap = dutyTypeService.getTypeMap();

        // 【头像处理】批量获取用户头像 fileId 映射，避免 N+1 查询问题
        Map<Long, String> userAvatarMap = getUserAvatarMap(userIdList);

        // 转换为 VO 并设置头像
        return dataList.stream().map(duty -> {
            DutyScheduleVO vo = new DutyScheduleVO();
            BeanCopyUtils.copyBean(duty, vo);
            setDutyTypeName(vo, dutyTypeMap);
            // 【头像处理】根据用户ID获取头像 fileId，然后通过 FileUtil 处理头像下载和路径转换
            String avatarFileId = userAvatarMap.get(duty.getUserId());
            if (avatarFileId != null) {
                // 调用 FileUtil.getAvatarPathOrDownload()：
                // 1. 检查本地是否已存在该头像文件（格式：{userId}_{fileId}）
                // 2. 不存在则从 IM 服务器下载到本地
                // 3. 返回本地 URL 路径（格式：/collaboration/static/{userId}_{fileId}）
                vo.setAvatar(fileUtil.getAvatarPathOrDownload(imHttpClient, avatarFileId));
            } else {
                // 用户无头像信息，使用默认头像路径
                vo.setAvatar(fileUtil.getDefaultPath());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量获取用户头像 fileId 映射
     * <p>
     * 目的：避免在循环中逐个查询用户信息导致 N+1 查询问题
     *
     * @param userIdList 用户ID列表
     * @return Map<用户ID, 头像fileId>
     */
    private Map<Long, String> getUserAvatarMap(List<Long> userIdList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyMap();
        }
        try {
            // 批量查询用户信息（分批查询，每批50个）
            List<ImUser> users = batchQueryUsersByIds(userIdList);
            if (CollectionUtils.isEmpty(users)) {
                return Collections.emptyMap();
            }
            // 构建 Map：用户ID -> 头像fileId，过滤掉无头像的用户
            return users.stream()
                    .filter(user -> user.getId() != null && user.getAvatar() != null)
                    .collect(Collectors.toMap(ImUser::getId, ImUser::getAvatar, (a, b) -> a));
        } catch (Exception e) {
            log.warn("获取用户头像信息失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量查询用户信息（分批查询，避免单次查询数据量过大）
     * <p>
     * IM 接口单次查询有数量限制，因此需要分批调用
     *
     * @param userIdList 用户ID列表
     * @return 用户信息列表
     */
    private List<ImUser> batchQueryUsersByIds(List<Long> userIdList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyList();
        }
        List<ImUser> allUsers = new ArrayList<>();
        // 分批查询，每批50个（IM 接口限制）
        int batchSize = 50;
        for (int i = 0; i < userIdList.size(); i += batchSize) {
            // 计算当前批次的结束索引
            int endIndex = Math.min(i + batchSize, userIdList.size());
            // 获取当前批次的用户ID列表
            List<Long> batchIds = userIdList.subList(i, endIndex);
            try {
                // 将用户ID列表转为逗号分隔的字符串
                String userIdsStr = batchIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                // 调用 IM 接口批量查询用户信息
                var userGetVo = imHttpClient.userPageById(userIdsStr);
                if (userGetVo != null && CollectionUtils.isNotEmpty(userGetVo.getResults())) {
                    allUsers.addAll(userGetVo.getResults());
                }
            } catch (Exception e) {
                // 单批查询失败不影响其他批次，记录日志后继续
                log.warn("批量查询用户信息失败, batch: {}", batchIds, e);
            }
        }
        return allUsers;
    }

    @Override
    public Page<DutyScheduleVO> findPage(int pageNum, int pageSize, String userId, String userName, String startDate,
                                         String endDate, Long departmentId, Long dutyType) {
        Page<DutySchedule> page = new Page<>(pageNum, pageSize);
        List<Long> orgIds = new ArrayList<>();
        if(departmentId != null){
            orgIds.add(departmentId);
        }else{
            UserInfo user = SecurityUtils.getUser();
            if (user != null && user.isAdmin()) {
                // 查询部门id进行过滤
                List<Long> orgByUserId = imUserRPCService.findOrgByUserId(user.getUserId());
                if(CollectionUtils.isNotEmpty(orgByUserId)){
                    orgIds.addAll(orgByUserId);
                }
            }
        }
        String queryUserName = userName;
        List<Long> matchedDepartmentIds = findDepartmentAndChildrenIdsByName(userName);
        if (CollectionUtils.isNotEmpty(matchedDepartmentIds)) {
            orgIds.clear();
            orgIds.addAll(matchedDepartmentIds);
            userId = null;
            queryUserName = null;
            startDate = null;
            endDate = null;
        }
        LambdaQueryWrapper<DutySchedule> lambdaQueryWrapper =
                getDutyScheduleLambdaQueryWrapper(userId, queryUserName, startDate, endDate, orgIds, dutyType);
        Page<DutySchedule> dutySchedulePage = dutyScheduleMapper.selectPage(page, lambdaQueryWrapper);
        List<DutySchedule> records = dutySchedulePage.getRecords();
        // 构建返回的 VO 分页对象
        Page<DutyScheduleVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(dutySchedulePage.getTotal());
        voPage.setPages(dutySchedulePage.getPages());

        if (CollectionUtils.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 查询协同岗信息
        List<String> userIdList = records.stream()
                .map(e -> String.valueOf(e.getUserId()))
                .distinct()
                .collect(Collectors.toList());
        Map<Long, List<CollaborationPost>> longListMap = collaborationPostService.buildUserIdListMap(userIdList);
        Map<Long, DutyType> dutyTypeMap = dutyTypeService.getTypeMap();

        // 转换为 VO 并设置额外属性
        List<DutyScheduleVO> voList = records.stream().map(duty -> {
            DutyScheduleVO vo = new DutyScheduleVO();
            BeanCopyUtils.copyBean(duty, vo);

            // 设置协同岗名称
            List<CollaborationPost> posts = longListMap.get(duty.getUserId());
            if (CollectionUtils.isNotEmpty(posts)) {
                vo.setPostName(posts.stream()
                        .map(CollaborationPost::getPostName)
                        .collect(Collectors.joining(",")));
            }
            setDutyTypeName(vo, dutyTypeMap);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    private static LambdaQueryWrapper<DutySchedule> getDutyScheduleLambdaQueryWrapper(String userId, String userName,
                                                                                      String startDate, String endDate, List<Long> orgIds, Long dutyType) {
        LambdaQueryWrapper<DutySchedule> lambdaQueryWrapper =
            new LambdaQueryWrapper<DutySchedule>().like(StringUtils.isNotBlank(userId), DutySchedule::getUserId, userId)
                .like(StringUtils.isNotBlank(userName), DutySchedule::getUserName, userName);
        // 对于跨天值班，查询范围：值班开始日期 <= endDate 且 值班结束日期 >= startDate
        if (StringUtils.isNotBlank(startDate)) {
            lambdaQueryWrapper.ge(DutySchedule::getDutyEndDate, LocalDate.parse(startDate));
        }
        if (StringUtils.isNotBlank(endDate)) {
            lambdaQueryWrapper.le(DutySchedule::getDutyStartDate, LocalDate.parse(endDate));
        }
        if(CollectionUtils.isNotEmpty(orgIds)){
            lambdaQueryWrapper.in(DutySchedule::getDepartmentId, orgIds);
        }
        if (Objects.nonNull(dutyType)) {
            lambdaQueryWrapper.eq(DutySchedule::getDutyType, dutyType);
        }
        lambdaQueryWrapper.orderByAsc(DutySchedule::getDutyStartDate, DutySchedule::getDutyStartTime,DutySchedule::getUserId);
        return lambdaQueryWrapper;
    }

    private List<Long> findDepartmentAndChildrenIdsByName(String name) {
        if (StringUtils.isBlank(name)) {
            return Collections.emptyList();
        }
        List<ImDepartment> departments = organizationDiversionService.findDepartmentAndChildrenByName(name);
        if (CollectionUtils.isEmpty(departments)) {
            return Collections.emptyList();
        }
        return departments.stream()
                .map(ImDepartment::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<DutyScheduleVO>> calendar(String userId, String userName, String startDate, String endDate,
                                                      String month, Long departmentId, Long dutyType) {
        List<Long> orgIds = new ArrayList<>();
        if(departmentId != null){
            orgIds.add(departmentId);
        }else{
            UserInfo user = SecurityUtils.getUser();
            if (user != null && user.isAdmin()) {
                // 查询部门id进行过滤
                List<Long> orgByUserId = imUserRPCService.findOrgByUserId(user.getUserId());
                if(CollectionUtils.isNotEmpty(orgByUserId)){
                    orgIds.addAll(orgByUserId);
                }
            }
        }
        String queryUserName = userName;
        List<Long> matchedDepartmentIds = findDepartmentAndChildrenIdsByName(userName);
        if (CollectionUtils.isNotEmpty(matchedDepartmentIds)) {
            orgIds.clear();
            orgIds.addAll(matchedDepartmentIds);
            queryUserName = null;
            userId = null;
            startDate = null;
            endDate = null;
            month = null;
        }
        LambdaQueryWrapper<DutySchedule> lambdaQueryWrapper =
                getDutyScheduleLambdaQueryWrapper(userId, queryUserName, startDate, endDate, orgIds, dutyType);
        if (StringUtils.isNotBlank(month)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
            YearMonth ym = YearMonth.parse(month, fmt);
            LocalDate firstDay = ym.atDay(1);
            LocalDate lastDay = ym.atEndOfMonth();
            // 对于跨天值班，查询与该月有交集的记录：值班开始日期 <= 月末 且 值班结束日期 >= 月初
            lambdaQueryWrapper.le(DutySchedule::getDutyStartDate, lastDay).ge(DutySchedule::getDutyEndDate, firstDay);
        }
        List<DutySchedule> dataList = dutyScheduleMapper.selectList(lambdaQueryWrapper);
        // 查询该人员支持的协同岗
        if(CollectionUtils.isNotEmpty(dataList)){
            List<String> userIdList = dataList.stream().map(e-> String.valueOf(e.getUserId())).distinct().collect(Collectors.toList());
            Map<Long, List<CollaborationPost>> longListMap = collaborationPostService.buildUserIdListMap(userIdList);
            Map<Long, DutyType> dutyTypeMap = dutyTypeService.getTypeMap();
            List<DutyScheduleVO> dutyScheduleVoList = dataList.stream().map(duty -> {
                DutyScheduleVO dutyScheduleVO = new DutyScheduleVO();
                BeanCopyUtils.copyBean(duty, dutyScheduleVO);
                List<CollaborationPost> postList = longListMap.get(duty.getUserId());
                if (CollectionUtils.isNotEmpty(postList)) {
                    String postName = postList.stream().map(CollaborationPost::getPostName).collect(Collectors.joining(","));
                    dutyScheduleVO.setPostName(postName);
                }
                setDutyTypeName(dutyScheduleVO, dutyTypeMap);
                return dutyScheduleVO;
            }).collect(Collectors.toList());

            // 对于跨天值班，需要展开到每一天的日历中
            // 例如：2026-06-01 20:00 至 2026-06-02 08:00 的夜班，需要出现在6月1日和6月2日两天的日历中
            return expandCrossDayDutySchedules(dutyScheduleVoList);
        }else{
            return Collections.emptyMap();
        }

    }

    /**
     * 展开跨天值班到每一天的日历中
     * <p>
     * 对于跨天值班，需要在日历的每一天都显示该排班记录，并且需要将时间拆分为多段：
     * - 第一天：开始时间 -> 23:59:59
     * - 中间天：00:00:00 -> 23:59:59
     * - 最后一天：00:00:00 -> 结束时间
     * <p>
     * 例如：夜班 2026-06-01 20:00 至 2026-06-03 08:00
     * - 在6月1日的日历中：20:00 -> 23:59:59
     * - 在6月2日的日历中：00:00:00 -> 23:59:59
     * - 在6月3日的日历中：00:00:00 -> 08:00
     *
     * @param dutyScheduleVoList 排班记录列表
     * @return 按日期分组的排班记录Map
     */
    private Map<String, List<DutyScheduleVO>> expandCrossDayDutySchedules(List<DutyScheduleVO> dutyScheduleVoList) {
        // 用于存储展开后的记录，key为显示日期
        Map<String, List<DutyScheduleVO>> resultMap = new LinkedHashMap<>();

        // 一天的结束时间：23:59:59
        final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);
        // 一天的开始时间：00:00:00
        final LocalTime START_OF_DAY = LocalTime.of(0, 0, 0);

        for (DutyScheduleVO vo : dutyScheduleVoList) {
            LocalDate startDate = vo.getDutyStartDate();
            LocalDate endDate = vo.getDutyEndDate();
            LocalTime startTime = vo.getDutyStartTime();
            LocalTime endTime = vo.getDutyEndTime();

            // 遍历从开始日期到结束日期的每一天
            int dayIndex = 0;
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateKey = date.toString();

                // 创建该日期的VO副本
                DutyScheduleVO dateVo = new DutyScheduleVO();
                BeanCopyUtils.copyBean(vo, dateVo);

                // 设置显示日期
                dateVo.setDutyStartDate(date);
                dateVo.setDutyEndDate(date);

                // 计算该段的开始时间和结束时间
                boolean isFirstDay = date.equals(startDate);
                boolean isLastDay = date.equals(endDate);

                if (isFirstDay && isLastDay) {
                    // 同一天：使用原始时间
                    dateVo.setDutyStartTime(startTime);
                    dateVo.setDutyEndTime(endTime);
                } else if (isFirstDay) {
                    // 第一天：开始时间 -> 23:59:59
                    dateVo.setDutyStartTime(startTime);
                    dateVo.setDutyEndTime(END_OF_DAY);
                } else if (isLastDay) {
                    // 最后一天：00:00:00 -> 结束时间
                    dateVo.setDutyStartTime(START_OF_DAY);
                    dateVo.setDutyEndTime(endTime);
                } else {
                    // 中间天：00:00:00 -> 23:59:59
                    dateVo.setDutyStartTime(START_OF_DAY);
                    dateVo.setDutyEndTime(END_OF_DAY);
                }

                // 添加到对应日期的列表中
                resultMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(dateVo);
                dayIndex++;
            }
        }

        // 对每个日期内的排班按显示开始时间排序
        resultMap.forEach((date, list) -> {
            list.sort(Comparator.comparing(DutyScheduleVO::getDutyStartTime));
        });

        return resultMap;
    }

    @Override
    public List<DutySchedule> findList(LocalDate dutyStartDate) {
        LambdaQueryWrapper<DutySchedule> lambdaQueryWrapper =
            new LambdaQueryWrapper<DutySchedule>().eq(DutySchedule::getDutyStartDate, dutyStartDate);
        return list(lambdaQueryWrapper);
    }

    @Override
    public List<DutySchedule> findListByDateRange(LocalDate startDate, LocalDate endDate) {
        // 查询与日期范围有交集的排班：dutyStartDate <= endDate AND dutyEndDate >= startDate
        LambdaQueryWrapper<DutySchedule> lambdaQueryWrapper =
            new LambdaQueryWrapper<DutySchedule>()
                .le(DutySchedule::getDutyStartDate, endDate)
                .ge(DutySchedule::getDutyEndDate, startDate);
        return list(lambdaQueryWrapper);
    }

    @Override
    public boolean resetUserCache() {
        try {
            List<IMUserExcel> allUser = getAllUser();
            log.info("duty scheduled all user size: {}", allUser.size());
            if (CollectionUtils.isNotEmpty(allUser)) {
                redisUtil.set(ALL_USER_KEY, allUser);
            }
            return true;
        } catch (Exception e) {
            log.error("resetUserCache failed, will keep old cache", e);
            return false;
        }
    }

    @Override
    public List<DutyScheduleVO> getOnDutyPersonalList(Long departmentId) {
        // 获取用户可见组织部门
//        List<Long> orgIds =
//                organizationDiversionService.queryDepartmentForListById(departmentId).stream().map(ImDepartment::getId)
//                        .collect(Collectors.toList());
        // 获取当前登录人
        UserInfo userInfo = SecurityUtils.getUser();
        if (userInfo == null) {
            log.error("获取当前登录人信息失败");
            return Lists.newArrayList();
        }
        List<String> imOrgPrivCodes = userInfo.getImOrgPrivCodes();
        if(CollectionUtils.isNotEmpty(imOrgPrivCodes)){
            Set<String> userDepartments = imCommonService.getUserDepartments();
            if(!userDepartments.contains(String.valueOf(departmentId))){
                log.warn("该用户没权限查看该部门的值班人员");
                return Lists.newArrayList();
            }
        }else{
            Long organizationId = userInfo.getOrganizationId();
            if(departmentId == null || !departmentId.equals(organizationId)){
                log.warn("该用户没权限查看该部门的值班人员");
                return Lists.newArrayList();
            }
        }

        List<Long> orgIds = new ArrayList<>();
        orgIds.add(departmentId);
        log.info("查询当前正在值班的人员列表，部门ID：{}", departmentId);

        // 获取当前日期和时间
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        log.info("当前日期：{}，当前时间：{}", currentDate, currentTime);

        Long userId = userInfo.getUserId();
        // 查询正在值班的人员
        List<DutyScheduleVO> onDutyList = dutyScheduleMapper.selectOnDutyPersonnel(currentDate, currentTime, orgIds);
        log.info("查询到正在值班的人员数量：{}", onDutyList.size());
        if(CollectionUtils.isNotEmpty(onDutyList)){
            // 过滤掉自己
            return onDutyList.stream()
                    .filter(duty -> !duty.getUserId().equals(userId))
                    .peek(duty-> {
                        duty.setAvatar(fileUtil.getAvatarPathOrDownload(imHttpClient, duty.getAvatar()));
                    }).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    /**
     * 数据交叉校验，支持跨天值班
     *
     * @param list 现有排班列表
     * @param newDuty 新排班
     * @return 重叠的排班列表
     */
    public List<DutySchedule> overlapList(List<DutySchedule> list, DutySchedule newDuty) {
        List<DutySchedule> repeatList = new ArrayList<>();
        LocalDateTime newStart = LocalDateTime.of(newDuty.getDutyStartDate(), newDuty.getDutyStartTime());
        LocalDateTime newEnd = LocalDateTime.of(newDuty.getDutyEndDate(), newDuty.getDutyEndTime());
        for (DutySchedule d : list) {
            LocalDateTime existStart = LocalDateTime.of(d.getDutyStartDate(), d.getDutyStartTime());
            LocalDateTime existEnd = LocalDateTime.of(d.getDutyEndDate(), d.getDutyEndTime());
            // 半开区间相交条件：新开始 < 旧结束 且 新结束 > 旧开始
            if (newStart.isBefore(existEnd) && newEnd.isAfter(existStart)) {
                repeatList.add(d);
            }
        }
        return repeatList;
    }

    /**
     * 检查同一用户同一类型是否有时间重叠（不同类型可以时间重叠）
     * 支持跨天值班的重叠检查
     *
     * @param list 同一用户的排班列表
     * @param newDuty 新排班
     * @return true: 存在时间重叠, false: 不存在
     */
    public boolean isOverlap(List<DutyScheduleExcel> list, DutyScheduleExcel newDuty) {
        String newDutyType = newDuty.getDutyType();
        LocalDateTime newStart = LocalDateTime.of(
            LocalDate.parse(newDuty.getDutyStartDate()),
            LocalTime.parse(newDuty.getDutyStartTime()));
        LocalDateTime newEnd = LocalDateTime.of(
            LocalDate.parse(newDuty.getDutyEndDate()),
            LocalTime.parse(newDuty.getDutyEndTime()));

        for (DutyScheduleExcel d : list) {
            // 只检查同一类型的排班
            if (newDutyType != null && newDutyType.equals(d.getDutyType())) {
                LocalDateTime existStart = LocalDateTime.of(
                    LocalDate.parse(d.getDutyStartDate()),
                    LocalTime.parse(d.getDutyStartTime()));
                LocalDateTime existEnd = LocalDateTime.of(
                    LocalDate.parse(d.getDutyEndDate()),
                    LocalTime.parse(d.getDutyEndTime()));

                // 完全相同（开始和结束都相同）允许导入（会被替换）
                if (newStart.equals(existStart) && newEnd.equals(existEnd)) {
                    continue;
                }

                // 半开区间相交条件：新开始 < 旧结束 且 新结束 > 旧开始
                if (newStart.isBefore(existEnd) && newEnd.isAfter(existStart)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<DutySchedule> overlapList(Map<String, List<DutySchedule>> userDataMap, DutySchedule dutySchedule) {
        String key = String.valueOf(dutySchedule.getUserId());
        List<DutySchedule> dutyList = userDataMap.get(key);
        if (CollectionUtils.isNotEmpty(dutyList)) {
            return overlapList(dutyList, dutySchedule);
        }
        return Collections.emptyList();
    }

    private boolean isOverlap(Map<String, List<DutyScheduleExcel>> userDataMap, DutyScheduleExcel excel) {
        String key = String.valueOf(excel.getUserId());
        List<DutyScheduleExcel> dutyScheduleExcels = userDataMap.get(key);
        if (CollectionUtils.isNotEmpty(dutyScheduleExcels)) {
            return isOverlap(dutyScheduleExcels, excel);
        }
        return false;
    }

    private ImportDutyScheduleResult checkImportData(List<DutyScheduleExcel> excelList) {
        if (CollectionUtils.isEmpty(excelList)) {
            throw new BusinessException("导入的排班信息不能为空");
        }
        ImportDutyScheduleResult result = new ImportDutyScheduleResult();
        Map<Integer, List<String>> errorMap = new LinkedHashMap<>();
        List<DutyScheduleExcel> successList = new ArrayList<>();

        int startRow = 3;

        for (int i = 0; i < excelList.size(); i++) {
            DutyScheduleExcel excel = excelList.get(i);
            excel.setRowNum(startRow + i);
        }

        Map<String, DutyScheduleExcel> excelKeyMap = new HashMap<>();
        for (var excel : excelList) {
            String key = buildExactMatchKeyFromExcel(excel);
            if (excelKeyMap.containsKey(key)) {
                // 发现重复行，添加到错误列表
                errorMap.put(excel.getRowNum(), List.of("存在重复的排班记录"));
                continue;
            }
            excelKeyMap.put(key, excel);
        }

        excelList = new ArrayList<>(excelKeyMap.values());
        excelList.sort(Comparator.comparing(DutyScheduleExcel::getRowNum));

        Map<String, List<DutyScheduleExcel>> userDataMap = new HashMap<>();
        // 走缓存查询
        Map<String, IMUserExcel> userMap =
                getAllUserFromCache().stream().collect(Collectors.toMap(IMUserExcel::getId, Function.identity()));
        Map<Long, DutyType> dutyTypeMap = dutyTypeService.getTypeMap();
        Map<String, Long> dutyTypeNameMap = dutyTypeMap.values().stream()
                .filter(dutyType -> StringUtils.isNotBlank(dutyType.getName()))
                .collect(Collectors.toMap(DutyType::getName, DutyType::getType, (oldValue, newValue) -> oldValue));
        Set<String> unauthorizedUserIdSet = checkAdminImportUserPermission(excelList, userMap);

        // 查询所有涉及用户的现有排班数据
        Set<Long> userIdSet = excelList.stream()
                .filter(excel -> StringUtils.isNotBlank(excel.getUserId()))
                .map(excel -> Long.valueOf(excel.getUserId()))
                .collect(Collectors.toSet());

        Map<String, List<DutySchedule>> existingDataMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(userIdSet)) {
            List<DutySchedule> existingList = findList(new ArrayList<>(userIdSet));
            existingDataMap = existingList.stream().collect(
                Collectors.groupingBy(duty -> String.valueOf(duty.getUserId())));
        }
//
//        int startRow = 3;
        for (DutyScheduleExcel excel : excelList) {
            List<String> err = new ArrayList<>();
            String userId = excel.getUserId();
            String userName = excel.getUserName();
            String dutyStartDate = excel.getDutyStartDate();
            String dutyStartTime = excel.getDutyStartTime();
            String dutyEndDate = excel.getDutyEndDate();
            String dutyEndTime = excel.getDutyEndTime();
            String dutyTypeText = excel.getDutyType();
            Long dutyType = parseDutyType(dutyTypeText, dutyTypeMap, dutyTypeNameMap);

            // 1. 必填
            if (StringUtils.isBlank(userId))
                err.add("人员ID不能为空");
            if (StringUtils.isBlank(userName))
                err.add("姓名不能为空");
            if (StringUtils.isBlank(dutyStartDate))
                err.add("值班开始日期不能为空");
            if (StringUtils.isBlank(dutyStartTime))
                err.add("值班开始时间不能为空");
            if (StringUtils.isBlank(dutyEndDate))
                err.add("值班结束日期不能为空");
            if (StringUtils.isBlank(dutyEndTime))
                err.add("值班结束时间不能为空");
            if (StringUtils.isBlank(dutyTypeText))
                err.add("排班类型不能为空");

            // 2. 格式
            if (StringUtils.isNotBlank(dutyStartDate) && !DATE_PATTERN.matcher(dutyStartDate).matches()) {
                err.add("值班开始日期格式错误，应为 yyyy-MM-dd");
            }
            if (StringUtils.isNotBlank(dutyEndDate) && !DATE_PATTERN.matcher(dutyEndDate).matches()) {
                err.add("值班结束日期格式错误，应为 yyyy-MM-dd");
            }
            if (StringUtils.isNotBlank(dutyStartTime) && !TIME_PATTERN.matcher(dutyStartTime).matches()) {
                err.add("值班开始时间格式错误，应为 HH:mm");
            }
            if (StringUtils.isNotBlank(dutyEndTime) && !TIME_PATTERN.matcher(dutyEndTime).matches()) {
                err.add("值班结束时间格式错误，应为 HH:mm");
            }
            if (StringUtils.isNotBlank(dutyTypeText) && Objects.isNull(dutyType)) {
                err.add("排班类型不存在");
            }

            if (StringUtils.isNotBlank(userId)) {
                IMUserExcel matchImUserExcel = userMap.get(userId);
                if (Objects.isNull(matchImUserExcel)) {
                    err.add("人员ID不存在");
                } else if (StringUtils.isNotBlank(userName)) {
                    boolean nameEq = userName.equals(matchImUserExcel.getName());
                    if (!nameEq) {
                        err.add("人员ID和姓名不匹配");
                    }
                }
            }

            // 2. 权限范围检查
            if (StringUtils.isNotBlank(userId) && unauthorizedUserIdSet.contains(userId)) {
                err.add(userName + "人员不在权限范围内，不支持导入");
            }

            // 3. 业务：时间先后
            if (err.isEmpty()) {
                try {
                    LocalDateTime start = LocalDateTime.of(
                        LocalDate.parse(dutyStartDate), LocalTime.parse(dutyStartTime));
                    LocalDateTime end = LocalDateTime.of(
                        LocalDate.parse(dutyEndDate), LocalTime.parse(dutyEndTime));
                    if (start.isBefore(LocalDateTime.now())) {
                        err.add("值班开始时间必须晚于当前时间");
                    } else if (!end.isAfter(start)) {
                        err.add("值班结束时间必须晚于值班开始时间");
                    }
                } catch (Exception e) {
                    err.add("日期或时间转换异常");
                }
            }

            // 4. 业务：同一用户同一类型不能有时间重叠（不同类型可以时间重叠）
            // 注意：需要先设置dutyType，否则后续检查时类型不一致
            if (err.isEmpty()) {
                excel.setDutyType(String.valueOf(dutyType));
                if (isOverlap(userDataMap, excel)) {
                    err.add("该用户同一类型存在时间重叠的排班计划");
                } else {
                    remark2Map(userDataMap, excel);
                }
            }

            // 5. 业务：检查与数据库现有数据的冲突（同一类型不能时间重叠，不同类型可以）
            if (err.isEmpty()) {
                String conflictMsg = checkConflictWithExistingData(existingDataMap, excel, excel.getRowNum());
                if (StringUtils.isNotBlank(conflictMsg)) {
                    err.add(conflictMsg);
                }
            }

            if (!err.isEmpty()) {
                errorMap.put(excel.getRowNum(), err);
            } else {
                successList.add(excel);
            }
        }

        result.setErrorMap(errorMap);
        result.setSuccessList(successList);

        return result;
    }

    private Long parseDutyType(String dutyTypeText, Map<Long, DutyType> dutyTypeMap, Map<String, Long> dutyTypeNameMap) {
        if (StringUtils.isBlank(dutyTypeText)) {
            return null;
        }
        String trimText = dutyTypeText.trim();
        Long dutyType = dutyTypeNameMap.get(trimText);
        if (Objects.nonNull(dutyType)) {
            return dutyType;
        }
        if (StringUtils.isNumeric(trimText)) {
            dutyType = Long.valueOf(trimText);
            if (dutyTypeMap.containsKey(dutyType)) {
                return dutyType;
            }
        }
        return null;
    }

    private Set<String> checkAdminImportUserPermission(List<DutyScheduleExcel> excelList, Map<String, IMUserExcel> userMap) {
        UserInfo currentUser = SecurityUtils.getUser();
        if (Objects.isNull(currentUser) || !currentUser.isAdmin()) {
            return Collections.emptySet();
        }

        List<Long> currentUserOrgIdList = imUserRPCService.findOrgByUserId(currentUser.getUserId());
        if (CollectionUtils.isEmpty(currentUserOrgIdList)) {
            return Collections.emptySet();
        }

        Set<String> currentUserOrgIdSet = currentUserOrgIdList.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        // 找出权限范围外的用户ID集合
        return excelList.stream()
                .map(DutyScheduleExcel::getUserId)
                .filter(StringUtils::isNotBlank)
                .filter(userId -> {
                    IMUserExcel user = userMap.get(userId);
                    return user != null && !currentUserOrgIdSet.contains(user.getDeptId());
                })
                .collect(Collectors.toSet());
    }

    private void remark2Map(Map<String, List<DutyScheduleExcel>> userDataMap, DutyScheduleExcel excel) {
        String key = String.valueOf(excel.getUserId());
        List<DutyScheduleExcel> dutyScheduleExcels = userDataMap.get(key);
        if (Objects.isNull(dutyScheduleExcels)) {
            dutyScheduleExcels = new ArrayList<>();
        }
        dutyScheduleExcels.add(excel);
        userDataMap.put(key, dutyScheduleExcels);
    }

    /**
     * 检查导入数据与数据库现有数据的冲突
     * 新规则：同一类型不能时间重叠，不同类型可以时间重叠
     * 支持跨天值班
     *
     * @param existingDataMap 现有数据Map，key为userId
     * @param excel 导入的Excel数据
     * @param rowNumber 行号
     * @return 冲突信息，无冲突返回null
     */
    private String checkConflictWithExistingData(Map<String, List<DutySchedule>> existingDataMap,
                                                  DutyScheduleExcel excel, int rowNumber) {
        String key = String.valueOf(excel.getUserId());
        List<DutySchedule> existingList = existingDataMap.get(key);

        if (CollectionUtils.isEmpty(existingList)) {
            return null;
        }

        LocalDateTime newStart = LocalDateTime.of(
            LocalDate.parse(excel.getDutyStartDate()),
            LocalTime.parse(excel.getDutyStartTime()));
        LocalDateTime newEnd = LocalDateTime.of(
            LocalDate.parse(excel.getDutyEndDate()),
            LocalTime.parse(excel.getDutyEndTime()));
        String newDutyType = excel.getDutyType();

        for (DutySchedule existing : existingList) {
            // 只检查同一类型的排班
            if (newDutyType != null && newDutyType.equals(String.valueOf(existing.getDutyType()))) {
                LocalDateTime existStart = LocalDateTime.of(existing.getDutyStartDate(), existing.getDutyStartTime());
                LocalDateTime existEnd = LocalDateTime.of(existing.getDutyEndDate(), existing.getDutyEndTime());

                // 完全重叠（开始和结束都相同）允许导入（会被替换）
                if (newStart.equals(existStart) && newEnd.equals(existEnd)) {
                    continue;
                }

                // 判断是否有交集：新开始 < 旧结束 且 新结束 > 旧开始
                if (newStart.isBefore(existEnd) && newEnd.isAfter(existStart)) {
                    return String.format("%s的排班时间%s %s至%s %s与现有排班%s %s至%s %s存在冲突",
                        excel.getUserName(),
                        excel.getDutyStartDate(), excel.getDutyStartTime(),
                        excel.getDutyEndDate(), excel.getDutyEndTime(),
                        existing.getDutyStartDate(), existStart.toLocalTime(),
                        existing.getDutyEndDate(), existEnd.toLocalTime());
                }
            }
        }

        return null;
    }

    private void setDutyTypeName(DutyScheduleVO vo, Map<Long, DutyType> dutyTypeMap) {
        if (Objects.isNull(vo) || Objects.isNull(vo.getDutyType()) || MapUtils.isEmpty(dutyTypeMap)) {
            return;
        }
        DutyType dutyType = dutyTypeMap.get(vo.getDutyType());
        if (Objects.nonNull(dutyType)) {
            // 返回类型名称，减少前端二次查询排班类型列表的成本。
            vo.setDutyTypeName(dutyType.getName());
        }
    }

    /**
     * 这里先使用5分钟后台获取全量用户信息，避免数据过多造成接口超时，假设im如果有数据更新，需要等待5分钟才能获取最新人员信息
     */
    @Scheduled(initialDelay = 10000L, fixedDelay = 5L * 60L * 1000L)
    public void scheduled() {
        resetUserCache();
    }

}