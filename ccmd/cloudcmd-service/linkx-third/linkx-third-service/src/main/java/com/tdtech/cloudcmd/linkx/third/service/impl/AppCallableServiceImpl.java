package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.TaskRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksExecutors;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableCreateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableUpdateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.KeyValue;
import com.tdtech.cloudcmd.linkx.third.api.dto.SearchPair;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallableApi;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallableDb;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallableTransactionTask;
import com.tdtech.cloudcmd.linkx.third.enums.AppTypeEnum;
import com.tdtech.cloudcmd.linkx.third.enums.Constants;
import com.tdtech.cloudcmd.linkx.third.enums.TaskCreateTypeEnum;
import com.tdtech.cloudcmd.linkx.third.mapper.*;
import com.tdtech.cloudcmd.linkx.third.service.AppCallableDataSyncService;
import com.tdtech.cloudcmd.linkx.third.service.IAppCallableService;
import com.tdtech.cloudcmd.linkx.third.utils.AssertUtils;
import com.tdtech.cloudcmd.linkx.third.utils.ConditionExecuteUtils;
import com.tdtech.cloudcmd.linkx.third.utils.DateFormatFieldUtils;
import com.tdtech.cloudcmd.linkx.third.utils.KeyValueUtils;
import com.tdtech.cloudcmd.linkx.third.utils.MySqlTableNameValidator;
import com.tdtech.cloudcmd.linkx.third.vo.*;
import com.tdtech.cloudcmd.util.*;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.linkx.third.service.AbstractAppCallableDataSyncServiceImpl.LINKX_ID_COLUMN;

/**
 * <p>
 * 南向应用信息表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
@Slf4j
@Service
public class AppCallableServiceImpl extends ServiceImpl<AppCallableMapper, AppCallable> implements IAppCallableService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private AppCallableTransactionTaskMapper appCallableTransactionTaskMapper;

    @Autowired
    private AppCallableApiMapper apiMapper;

    @Autowired
    private AppCallableDbMapper dbMapper;

    @DubboReference
    private TaskRpcApi taskRpcApi;

    @Autowired
    private Map<String, AppCallableDataSyncService> syncServiceMap;

    @Autowired
    private AppCallableSyncMapper syncMapper;

    public static final String TASK_STATUS_PENDING = "待处理";

    @Override
    public Page<AppCallableVo> pageAppCallable(String name, long page, long pageSize) {
        List<AppCallable> appCallables = list();
        var wrapper = new LambdaQueryWrapper<AppCallable>()
                .eq(AppCallable::getIsDeleted, 0)
                // 改为全模糊查询,并通过改为接口名称查询
                .like(StringUtils.isNotBlank(name), AppCallable::getName, name)
                .orderByDesc(AppCallable::getGmtCreated);
        Page<AppCallable> pageData = page(new Page<>(page, pageSize), wrapper);
        List<AppCallable> records = CollectionUtils.isEmpty(pageData.getRecords()) ? Collections.emptyList()
                : pageData.getRecords();
        List<AppCallableVo> appCallableVos = BeanCopyUtils.copyList(records, AppCallableVo::new);
        Page<AppCallableVo> resultData = new Page<>(page, pageSize);
        resultData.setRecords(appCallableVos);
        resultData.setTotal(pageData.getTotal());
        return resultData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAppCallable(AppCallableCreateDto dto) {
        // 应用编码重复性校验，若已存在（含已软删除）同编码记录，则报错
        boolean codeExists = lambdaQuery().eq(AppCallable::getSystemCode, dto.getSystemCode()).exists();
        AssertUtils.check(!codeExists, "对应编码的应用数据已存在");

        // 人员校验
        var user = SecurityUtils.getUser();
        AssertUtils.checkAuth(Objects.nonNull(user), "access token invalid");

        // 校验reqParam和reqBody中的日期动态格式字段
//        validateDateFormatFields(dto);

        // 根据类型区分，构建主从表入库
        long appId = idWorker.nextId();
        AppCallable appCallable = BeanCopyUtils.copyBean(dto, AppCallable::new);
        appCallable.setId(appId);
        appCallable.setCreateUserId(user.getUserId());
        appCallable.setIsDeleted(Constants.VALID);
        appCallable.setGmtCreated(DateUtils.of(new Date()));
        appCallable.setMapper(StringUtils.isBlank(appCallable.getMapper()) ? "[]" : appCallable.getMapper());
        appCallable.setEnableTask(Objects.isNull(appCallable.getEnableTask()) ? 0 : appCallable.getEnableTask());
        appCallable.setTaskAutoFillConfig(StringUtils.isBlank(appCallable.getTaskAutoFillConfig())
                ? "[]" : appCallable.getTaskAutoFillConfig());
        save(appCallable);
        // 构建从表数据
        if (AppTypeEnum.API.getCode().equals(dto.getType())) {
            AppCallableApi appCallableApi = BeanCopyUtils.copyBean(dto, AppCallableApi::new);
            appCallableApi.setId(idWorker.nextId());
            appCallableApi.setAppCallableId(String.valueOf(appId));
            appCallableApi.setPagenation(Objects.isNull(appCallableApi.getPagenation()) ? 0 : appCallableApi.getPagenation());
            apiMapper.insert(appCallableApi);
        } else {
            AppCallableDb appCallableDb = BeanCopyUtils.copyBean(dto, AppCallableDb::new);
            appCallableDb.setId(idWorker.nextId());
            appCallableDb.setAppCallableId(String.valueOf(appId));
            dbMapper.insert(appCallableDb);
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAppCallable(AppCallableUpdateDto dto) {
        // 检测类型变化，若类型变化需要删除另外一种类型的子表冗余数据
        AppCallable app = getById(dto.getId());
        AssertUtils.check(Objects.nonNull(app), "南向应用数据不存在");
        AssertUtils.check(!Objects.equals(app.getIsDeleted(), Constants.DELETED), "该南向应用已被删除");

        // 不允许变更系统编码
        AssertUtils.check(StringUtils.isBlank(dto.getSystemCode()) || Objects.equals(dto.getSystemCode(), app.getSystemCode()),
                "应用编码不支持变更");

        // 校验reqParam和reqBody中的日期动态格式字段
//        validateDateFormatFields(dto);
        Integer appRawType = app.getType();
        boolean typeChanged = !Objects.equals(app.getType(), dto.getType());
        BeanCopyUtils.copyBean(dto, app);
        baseMapper.updateById(app);
        // 更新或者新增从表
        if (AppTypeEnum.API.getCode().equals(dto.getType())) {
            update4Api(dto);
            // 清除另外表的冗余数据
            ConditionExecuteUtils.execute(typeChanged, () -> dbMapper.delete(new QueryWrapper<AppCallableDb>()
                    .lambda().eq(AppCallableDb::getAppCallableId, String.valueOf(dto.getId()))));
        } else {
            update4Db(dto);
            // 清除另外表的冗余数据
            ConditionExecuteUtils.execute(typeChanged, () -> apiMapper.delete(new QueryWrapper<AppCallableApi>()
                    .lambda().eq(AppCallableApi::getAppCallableId, String.valueOf(dto.getId()))));
        }
    }

    private void update4Db(AppCallableUpdateDto dto) {
        AppCallableDb db = dbMapper.selectOne(new QueryWrapper<AppCallableDb>().lambda()
                .eq(AppCallableDb::getAppCallableId, String.valueOf(dto.getId())));
        if (Objects.isNull(db)) {
            db = BeanCopyUtils.copyBean(dto, AppCallableDb::new);
            db.setId(idWorker.nextId());
            db.setAppCallableId(String.valueOf(dto.getId()));
            dbMapper.insert(db);
        } else {
            Long dbId = db.getId();
            BeanCopyUtils.copyBean(dto, db);
            db.setId(dbId);
            dbMapper.updateById(db);
        }
    }

    private void update4Api(AppCallableUpdateDto dto) {
        AppCallableApi api = apiMapper.selectOne(new QueryWrapper<AppCallableApi>().lambda()
                .eq(AppCallableApi::getAppCallableId, String.valueOf(dto.getId())));
        if (Objects.isNull(api)) {
            api = BeanCopyUtils.copyBean(dto, AppCallableApi::new);
            api.setId(idWorker.nextId());
            api.setAppCallableId(String.valueOf(dto.getId()));
            apiMapper.insert(api);
        } else {
            Long apiId = api.getId();
            BeanCopyUtils.copyBean(dto, api);
            api.setId(apiId);
            apiMapper.updateById(api);
        }
    }

    @Override
    public void deleteAppCallable(Long callableId) {
        // 软删除主表数据, 从表数据无需处理
        AppCallable appCallable = new AppCallable();
        appCallable.setIsDeleted(Constants.DELETED);
        appCallable.setId(callableId);
        baseMapper.updateById(appCallable);
    }

    @Override
    public Page<JSONObject> pageAppData(Long callableId, String table, Long current, Long size, SearchPair<String, String> searchPair) {
        AppCallableDetailVo app = getAppCallable(callableId);
        AssertUtils.check(Objects.nonNull(app), "应用数据不存在");

        // 动态表名存在sql注入风险，需要校验
        AssertUtils.check(MySqlTableNameValidator.isValidTableName(table), "非法的数据库表名");

        // 查询数据
        Page<JSONObject> page = new Page<>(current, size);
        try {
            Long totalCount = baseMapper.countAppData(table, searchPair);
            page.setTotal(totalCount);
            if (totalCount <= 0) {
                page.setRecords(Collections.emptyList());
                return page;
            }
            // 时间排序字段, 若未指定则默认按更新时间排序，逆序
            String timeColumn = StringUtils.isBlank(app.getDateTimeSign()) ? "gmt_updated" : app.getDateTimeSign();
            List<JSONObject> records = baseMapper.pageAppData(table, current, size, searchPair, timeColumn);
            // 插入状态信息
            List<Long> linkxIds = records.stream().map(it -> it.getLong(LINKX_ID_COLUMN)).filter(Objects::nonNull).collect(Collectors.toList());
            List<TransactionTaskStatusVo> taskVosByIds = this.getTaskVosByIds(linkxIds);
            Map<Long, TransactionTaskStatusVo> statusVoMap = taskVosByIds.stream().collect(Collectors.toMap(
                    TransactionTaskStatusVo::getLinkxId,
                    Function.identity(),
                    (a, b) -> a));
            for (JSONObject record : records) {
                Long linkxId = record.getLong(LINKX_ID_COLUMN);
                record.put(Constants.LINKX_STATUS_INFO, statusVoMap.getOrDefault(linkxId, new TransactionTaskStatusVo()));
            }
            page.setRecords(records == null ? Collections.emptyList() : records);
        } catch (Exception e) {
            log.error("查询南向应用数据出错", e);
            throw new BusinessException("查询南向应用数据出错");
        }
        return page;
    }

    private List<TransactionTaskStatusVo> getTaskVosByIds(List<Long> linkxIds) {
        if (CollectionUtils.isEmpty(linkxIds)) {
            return new ArrayList<>();
        }
        List<AppCallableTransactionTask> transactionTasks = appCallableTransactionTaskMapper.selectList(
                new LambdaQueryWrapper<>(AppCallableTransactionTask.class)
                        .in(AppCallableTransactionTask::getAppCallableTableId, linkxIds)
                        .select(AppCallableTransactionTask::getAppCallableTableId, AppCallableTransactionTask::getTaskNo)
        );
        if (CollectionUtils.isEmpty(transactionTasks)) {
            return new ArrayList<>();
        }
        List<String> taskNos = transactionTasks.stream().map(AppCallableTransactionTask::getTaskNo).collect(Collectors.toList());
        List<TasksVO> tasksVOS = taskRpcApi.tasksDetails(taskNos);
        if (CollectionUtils.isEmpty(tasksVOS)) {
            tasksVOS = Collections.emptyList();
        }
        Map<String, List<TasksVO>> tasksVOGroup = tasksVOS.stream().collect(Collectors.groupingBy(TasksVO::getNumber));
        List<TransactionTaskStatusVo> statusVos = new ArrayList<>();
        for (AppCallableTransactionTask transactionTask : transactionTasks) {
            TransactionTaskStatusVo statusVo = new TransactionTaskStatusVo();
            statusVo.setLinkxId(transactionTask.getAppCallableTableId());
            statusVo.setTaskNo(transactionTask.getTaskNo());
            List<TasksVO> vos = tasksVOGroup.getOrDefault(transactionTask.getTaskNo(), new ArrayList<>());
            statusVo.setTaskStatus(!vos.isEmpty() ? vos.get(0).getStatus() : "");
            statusVos.add(statusVo);
        }
        // 是否考虑更新
        return statusVos;
    }
    @Override
    public String appDataToTask(Long callableId, AppCallableToTaskVo toTaskVo) {
        // 校验是否传入转发人
        AssertUtils.check(CollectionUtils.isNotEmpty(toTaskVo.getExecutors()), "请传入执行人信息");
        // 动态表名存在sql注入风险，需要校验
        AssertUtils.check(MySqlTableNameValidator.isValidTableName(toTaskVo.getAppCallableTableName()), "非法的数据库表名");
        // 校验是否有此条事务数据
        JSONObject appDataById = baseMapper.getAppDataById(toTaskVo.getAppCallableTableName(), toTaskVo.getAppCallableTableId());
        AssertUtils.check(Objects.nonNull(appDataById), "未查询到此条事务数据");
        // 人员校验
        var user = SecurityUtils.getUser();
        AssertUtils.checkAuth(Objects.nonNull(user), "access token invalid");

        AppCallable appCallable = this.getAppCallableById(callableId);
        TasksCreateReq createReq = new TasksCreateReq();
        // 创建任务实体
        // 任务编号：用雪花算法生成
        createReq.setNumber(String.valueOf(idWorker.nextId()));
        createReq.setName(toTaskVo.getName());
        createReq.setContent(toTaskVo.getContent());
        createReq.setSystem(appCallable.getSystemName());
        createReq.setModule(StringUtils.isNotBlank(toTaskVo.getModule()) ? toTaskVo.getModule() : appCallable.getSystemName());
        createReq.setBusinessType(appCallable.getName());
        createReq.setStatus(TASK_STATUS_PENDING);
        createReq.setType(1);
        createReq.setLevel(toTaskVo.getLevel());
        createReq.setUrgent(toTaskVo.getUrgent());
        createReq.setUrl(toTaskVo.getUrl());
        TasksExecutors creator = toTaskVo.getCreator();
        if (Objects.isNull(creator)) {
            creator = new TasksExecutors();
            creator.setName(user.getUserName());
            creator.setIdCard(user.getIdCardNum());
            creator.setDepartmentId(String.valueOf(user.getOrganizationId()));
            creator.setDepartmentCode(user.getOrganizationCode());
        }
        createReq.setCreator(creator);
        List<TasksExecutors> executors = toTaskVo.getExecutors();
        createReq.setExecutors(executors);
        createReq.setStartTime(toTaskVo.getStartTime());
        createReq.setEndTime(toTaskVo.getEndTime());
        // 存入json数据,改为前端传
        createReq.setExtend(toTaskVo.getExtend());
        // 只保留第一层结构
        // createReq.setExtend(JSONObject.toJSONString(appDataById));

        TasksVO tasksVO = taskRpcApi.saveTasks(createReq);
        AppCallableTransactionTask transactionTask = new AppCallableTransactionTask();
        transactionTask.setId(idWorker.nextId());
        transactionTask.setAppCallableId(callableId);
        transactionTask.setAppCallableTableId(toTaskVo.getAppCallableTableId());
        transactionTask.setAppCallableTableName(toTaskVo.getAppCallableTableName());
        transactionTask.setTaskId(tasksVO.getId());
        transactionTask.setTaskNo(tasksVO.getNumber());
        transactionTask.setTaskStatus(tasksVO.getStatus());
        transactionTask.setCreateUserId(user.getUserId());
        transactionTask.setToUserId(toTaskVo.getToUserId());
        transactionTask.setTaskCreateType(TaskCreateTypeEnum.MANUAL.getCode());
        transactionTask.setGmtCreated(DateUtils.of(new Date()));
        appCallableTransactionTaskMapper.insert(transactionTask);
        return tasksVO.getNumber();
    }

    @Override
    public void updateAppCallableMapper(Long callableId, AppCallableMapperVo mapperVo) {
        // 更新主表
        String mapper = Objects.isNull(mapperVo) || StringUtils.isBlank(mapperVo.getMapper()) ? "[]" : mapperVo.getMapper();
        this.update(new LambdaUpdateWrapper<>(AppCallable.class)
                .set(AppCallable::getMapper, mapper)
                .eq(AppCallable::getId, callableId));
    }

    @Override
    public void updateTaskConfig(Long callableId, AppCallableTaskConfigVo configVo) {
        Integer enableTask = Objects.isNull(configVo) || Objects.isNull(configVo.getEnableTask())
                ? 0 : configVo.getEnableTask();
        String taskAutoFillConfig = Objects.isNull(configVo) || StringUtils.isBlank(configVo.getTaskAutoFillConfig())
                ? "[]" : configVo.getTaskAutoFillConfig();
        this.update(new LambdaUpdateWrapper<>(AppCallable.class)
                .set(AppCallable::getEnableTask, enableTask)
                .set(AppCallable::getTaskAutoFillConfig, taskAutoFillConfig)
                .eq(AppCallable::getId, callableId));
    }

    @Override
    public AppCallableTaskConfigVo getTaskConfig(Long callableId) {
        AppCallable app = getAppCallableById(callableId);
        AssertUtils.check(Objects.nonNull(app), "南向应用数据不存在");
        AppCallableTaskConfigVo configVo = new AppCallableTaskConfigVo();
        configVo.setEnableTask(Objects.isNull(app.getEnableTask()) ? 0 : app.getEnableTask());
        configVo.setTaskAutoFillConfig(StringUtils.isBlank(app.getTaskAutoFillConfig())
                ? "[]" : app.getTaskAutoFillConfig());
        return configVo;
    }

    @Override
    public AppCallableDetailVo getAppCallable(Long callableId) {
        AppCallable appCallable = this.getAppCallableById(callableId);
        AppCallableDetailVo detailVo = BeanCopyUtils.copyBean(appCallable, AppCallableDetailVo::new);
        // 目前仅考虑单表
        if (AppTypeEnum.API.getCode().equals(appCallable.getType())) {
            List<AppCallableApi> appCallableApis = apiMapper.selectList(new LambdaQueryWrapper<>(AppCallableApi.class)
                    .eq(AppCallableApi::getAppCallableId, String.valueOf(callableId)));
            AssertUtils.check(CollectionUtils.isNotEmpty(appCallableApis), "未查询到对应的南向应用API信息");
            AppCallableApi appCallableApi = appCallableApis.get(0);
            // 塞入api信息
            detailVo.setProtocol(appCallableApi.getProtocol());
            detailVo.setMethod(appCallableApi.getMethod());
            detailVo.setReqHeader(appCallableApi.getReqHeader());
            detailVo.setReqBody(appCallableApi.getReqBody());
            detailVo.setReqParam(appCallableApi.getReqParam());
            detailVo.setUri(appCallableApi.getUri());
            detailVo.setPagenation(appCallableApi.getPagenation());
            detailVo.setPageParamLocation(appCallableApi.getPageParamLocation());
            detailVo.setPageFieldName(appCallableApi.getPageFieldName());
            detailVo.setPageSizeFieldName(appCallableApi.getPageSizeFieldName());
            detailVo.setResponseDataPath(appCallableApi.getResponseDataPath());
            detailVo.setPagenationType(appCallableApi.getPagenationType());
            detailVo.setDataStartTime(appCallableApi.getDataStartTime());
            detailVo.setDateTimeSign(appCallableApi.getDateTimeSign());
        } else {
            List<AppCallableDb> appCallableDbs = dbMapper.selectList(new LambdaQueryWrapper<>(AppCallableDb.class)
                    .eq(AppCallableDb::getAppCallableId, String.valueOf(callableId)));
            AssertUtils.check(CollectionUtils.isNotEmpty(appCallableDbs), "未查询到对应的南向应用数据库/视图信息");
            AppCallableDb appCallableDb = appCallableDbs.get(0);
            // 塞入数据库信息
            detailVo.setDbType(appCallableDb.getDbType());
            detailVo.setAccount(appCallableDb.getAccount());
            // 是否需要展示密码或前端加密
            detailVo.setPassword(appCallableDb.getPassword());
            detailVo.setDatabaseName(appCallableDb.getDatabaseName());
            detailVo.setDataName(appCallableDb.getDataName());
        }
        return detailVo;
    }

    @Override
    public List<String> listAppCallableTables(Long callableId) {
        AppCallable appCallable = this.getAppCallableById(callableId);
        return listAppCallableTables(callableId, appCallable);
    }

    @NotNull
    private List<String> listAppCallableTables(Long callableId, AppCallable appCallable) {
        List<String> list = new ArrayList<>();
        String tableName = Constants.TABLE_NAME_PREFIX + appCallable.getSystemCode();
        if (AppTypeEnum.API.getCode().equals(appCallable.getType())) {
            list.add(tableName);
        } else {
            List<AppCallableDb> appCallableDbs = dbMapper.selectList(new LambdaQueryWrapper<>(AppCallableDb.class)
                    .eq(AppCallableDb::getAppCallableId, String.valueOf(callableId)));
            appCallableDbs.forEach(db -> list.add(String.join("_", tableName, db.getDataName())));
        }
        return list;
    }

    private AppCallable getAppCallableById(Long callableId) {
        List<AppCallable> list = this.list(new LambdaQueryWrapper<>(AppCallable.class)
                .eq(AppCallable::getId, callableId).eq(AppCallable::getIsDeleted, Constants.VALID));
        AssertUtils.check(CollectionUtils.isNotEmpty(list), "未查询到对应的南向应用信息");
        return list.get(0);
    }

    @Override
    public TasksVO getTaskStatus(Long callableId, Long tableDataId, String tableName) {
        List<AppCallableTransactionTask> transactionTasks = appCallableTransactionTaskMapper.selectList(new LambdaQueryWrapper<>(AppCallableTransactionTask.class)
                .eq(AppCallableTransactionTask::getAppCallableId, callableId)
                .eq(AppCallableTransactionTask::getAppCallableTableId, tableDataId)
                .eq(AppCallableTransactionTask::getAppCallableTableName, tableName));
        AssertUtils.check(CollectionUtils.isNotEmpty(transactionTasks), "未查询对应的任务信息");
        AppCallableTransactionTask transactionTask = transactionTasks.get(0);
        // 通过编号查询任务最新状态
        TasksVO tasksVO = taskRpcApi.tasksDetail(transactionTask.getTaskNo());
        AssertUtils.check(Objects.nonNull(tasksVO), "未查询对应的任务信息");
        String taskStatus = tasksVO.getStatus();
        // 更新库中的任务状态
        appCallableTransactionTaskMapper.update(new LambdaUpdateWrapper<>(AppCallableTransactionTask.class)
                .set(AppCallableTransactionTask::getTaskStatus, taskStatus)
                .eq(AppCallableTransactionTask::getAppCallableId, transactionTask.getAppCallableId())
                .eq(AppCallableTransactionTask::getAppCallableTableId, tableDataId));

        // 返回任务详情
        return tasksVO;
    }

    @Override
    public String getTableName(AppCallableDetailVo app) {
        // 目前仅考虑单表
        if (AppTypeEnum.API.getCode().equals(app.getType())) {
            return Constants.TABLE_NAME_PREFIX + app.getSystemCode();
        } else {
            return Constants.TABLE_NAME_PREFIX + app.getSystemCode() + "_" + app.getDataName();
        }
    }

    @Override
    public void runSync(Long callableId) {
        AppCallableDetailVo app = getAppCallable(callableId);
        AssertUtils.check(Objects.nonNull(app), "南向应用数据不存在");
        AppCallableDataSyncService syncService = syncServiceMap.get(Objects.equals(AppTypeEnum.API.getCode(), app.getType())
                ? AppTypeEnum.API.getSyncService() : AppTypeEnum.DB.getSyncService());
        syncService.doSync(app);
    }

    @Override
    public List<AppCallableDataTableColumnInfoVo> getAppDataColumnInfo(Long callableId, String tableName) {
        AssertUtils.check(MySqlTableNameValidator.isValidTableName(tableName), "数据库表名不合法");
        AppCallable app = getById(callableId);
        AssertUtils.check(Objects.nonNull(app), "对应南向应用数据不存在");
        List<String> callableTables = listAppCallableTables(callableId, app);
        AssertUtils.check(callableTables.contains(tableName), "表名与南向应用对应的数据表不匹配");
        // 获取字段信息
        List<AppCallableDataTableColumnInfoVo> columnInfoList = syncMapper.getColumnInfo(tableName);
        if (CollectionUtils.isEmpty(columnInfoList)) {
            return Collections.emptyList();
        }

        // 获取mapper字段
        List<KeyValue> keyValues = JSON.parseArray(app.getMapper(), KeyValue.class);
        Map<String, String> columnMapper = KeyValueUtils.keyvalueToMap(keyValues);
        columnInfoList.forEach(column ->
                {
                    String columnMapperName = columnMapper.get(column.getColumnName());
                    if (StringUtils.isNotBlank(columnMapperName)) {
                        column.setIsMapperColumn(1);
                        column.setMapperColumnValue(columnMapperName);
                    }
                }
        );
        return columnInfoList;
    }

    @Override
    public List<AppCallableDataTableColumnInfoVo> getAppDataColumnInfo4App(Long taskId, String taskNo) {
        if (StringUtils.isBlank(taskNo) && Objects.isNull(taskId)) {
            throw new BusinessException("请传入任务编号或者任务id");
        }
        AppCallableTransactionTask transactionTask = appCallableTransactionTaskMapper.selectOne(
                Wrappers.lambdaQuery(AppCallableTransactionTask.class)
                .eq(Objects.nonNull(taskId), AppCallableTransactionTask::getTaskId, taskId)
                .eq(StringUtils.isNotBlank(taskNo), AppCallableTransactionTask::getTaskNo, taskNo));
        if (Objects.isNull(transactionTask)) {
            return new ArrayList<>();
        }
        return getAppDataColumnInfo(transactionTask.getAppCallableId(), transactionTask.getAppCallableTableName());
    }

    private void validateDateFormatFields(AppCallableCreateDto dto) {
        if (!AppTypeEnum.API.getCode().equals(dto.getType())) {
            return;
        }
        List<DateFormatFieldInfo> paramFields = DateFormatFieldUtils.extractDateFormatFields(dto.getReqParam());
        List<DateFormatFieldInfo> bodyFields = DateFormatFieldUtils.extractDateFormatFields(dto.getReqBody());
        boolean paramHasDateFields = paramFields.size() >= 2;
        boolean bodyHasDateFields = bodyFields.size() >= 2;
        // query和body中有且只能有一个包含2个及以上的日期动态格式字段
        if (paramHasDateFields && bodyHasDateFields) {
            throw new BusinessException("请求Params和请求Body中不能同时配置2个及以上的日期动态格式字段，请检查是否配置或日期动态格式是否正确");
        }
        if (!paramHasDateFields && !bodyHasDateFields) {
            throw new BusinessException("请求Params和请求Body中必须有一个包含2个及以上的日期动态格式字段，请检查是否配置或日期动态格式是否正确");
        }
        List<DateFormatFieldInfo> dateFields = paramHasDateFields ? paramFields : bodyFields;
        String source = paramHasDateFields ? "Params" : "Body";
        // 前两个字段的日期格式必须一致（默认为开始时间和结束时间）
        DateFormatFieldInfo first = dateFields.get(0);
        DateFormatFieldInfo second = dateFields.get(1);
        if (!first.getDateFormat().equals(second.getDateFormat())) {
            throw new BusinessException(String.format(
                    "请求%s中前两个日期动态格式字段[%s=%s, %s=%s]格式不一致，开始时间和结束时间格式需保持一致",
                    source, first.getFieldName(), first.getDateFormat(),
                    second.getFieldName(), second.getDateFormat()));
        }
    }


}