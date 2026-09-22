package com.tdtech.cloudcmd.linkx.third.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableCreateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableUpdateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.KeyValue;
import com.tdtech.cloudcmd.linkx.third.api.dto.SearchPair;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.tdtech.cloudcmd.linkx.third.enums.AppTypeEnum;
import com.tdtech.cloudcmd.linkx.third.enums.Constants;
import com.tdtech.cloudcmd.linkx.third.service.IAppCallableService;
import com.tdtech.cloudcmd.linkx.third.utils.AssertUtils;
import com.tdtech.cloudcmd.linkx.third.utils.ConditionExecuteUtils;
import com.tdtech.cloudcmd.linkx.third.utils.MySqlTableNameValidator;
import com.tdtech.cloudcmd.linkx.third.utils.datasource.DbDialect;
import com.tdtech.cloudcmd.linkx.third.vo.*;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Objects;

/**
 * 南向应用配置controller
 *
 */
@Slf4j
@RestController
@RequestMapping("/third/v1")
@RequiredArgsConstructor
@CrossOrigin
@Validated
@Tag(name = "南向应用配置", description = "南向应用配置")
public class AppCallableController {

    @Autowired
    private IAppCallableService service;

    @Autowired
    private ReportUtil reportUtil;

    /**
     * 获取可调用南向应用
     *
     * @return 南向应用列表
     */
    @GetMapping("/app/callable")
    @Operation(summary = "南向应用查询", description = "南向应用查询-不分页")
    public R<Page<AppCallableVo>> pageAppCallable(@RequestParam(value = "name", required = false) String name,
                                                  @Parameter(description = "当前页码", example = "1") @Min(1) @RequestParam(defaultValue = "1") Long page,
                                                  @Parameter(description = "每页记录数,-1代表查询全部", example = "10") @Min(-1) @RequestParam(defaultValue = "10") Long pageSize) {
        return R.success(service.pageAppCallable(name, page, pageSize));
    }

    /**
     * 创建南向应用
     *
     * @param dto 入参
     */
    @PostMapping("/app/callable")
    @Operation(summary = "创建南向应用", description = "创建南向应用")
    public R<Void> createAppCallable(@Valid @RequestBody AppCallableCreateDto dto) {
        try {
            // 参数校验
            checkParam(dto);
            service.createAppCallable(dto);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_INSERT,
                    String.format(OperationTypeEnum.GROUP_THIRD_APP_INSERT.getDesc(), buildCreateLog(dto)),
                    MSIPConstant.OPERATION_SUCCESS);
            return R.success();
        } catch (Exception e) {
            log.error("创建南向应用失败", e);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_INSERT,
                    "新增三方对接南向应用\"" + Objects.toString(dto.getName(), ""));
            return R.failure(e.getMessage());
        }
    }

    /**
     * 更新南向应用
     *
     * @param dto 入参
     */
    @PutMapping("/app/callable/{callableId}")
    @Operation(summary = "更新南向应用", description = "更新南向应用")
    public R<Void> updateAppCallable(@PathVariable Long callableId,
                                     @Valid @RequestBody AppCallableUpdateDto dto) {
        AppCallableDetailVo app = null;
        try {
            dto.setId(callableId);
            // 参数校验
            checkParam(dto);
            app = service.getAppCallable(callableId);
            AssertUtils.check(Objects.nonNull(app), "该南向应用不存在");
            service.updateAppCallable(dto);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_UPDATE,
                    String.format(OperationTypeEnum.GROUP_THIRD_APP_UPDATE.getDesc(), app.getSystemName(), callableId,
                            buildUpdateLog(dto, app)),
                    MSIPConstant.OPERATION_SUCCESS);
            return R.success();
        } catch (Exception e) {
            log.error("更新南向应用失败", e);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_UPDATE,
                    "修改三方对接南向应用" + resolveAppIdentifier(Objects.nonNull(app) ? app.getSystemName() : null, callableId));
            return R.failure(e.getMessage());
        }
    }

    /**
     * 删除南向应用-逻辑删除
     *
     * @param callableId 主键id
     */
    @DeleteMapping("/app/callable/{callableId}")
    @Operation(summary = "删除南向应用", description = "删除南向应用")
    public R<Void> deleteAppCallable(@PathVariable Long callableId) {
        AppCallable app = null;
        try {
            app = service.getById(callableId);
            if (Objects.isNull(app)) {
                reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_DELETE,
                        "删除三方对接南向应用[id=" + callableId + "]不存在");
                return R.failure("该南向应用不存在");
            }
            if (Objects.equals(app.getIsDeleted(), Constants.DELETED)) {
                reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_DELETE,
                        "删除三方对接南向应用" + resolveAppIdentifier(app.getSystemName(), callableId) + "已删除");
                return R.failure("该南向应用已被删除");
            }
            service.deleteAppCallable(callableId);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_DELETE,
                    String.format(OperationTypeEnum.GROUP_THIRD_APP_DELETE.getDesc(), app.getSystemName(), callableId),
                    MSIPConstant.OPERATION_SUCCESS);
            return R.success();
        } catch (Exception e) {
            log.error("删除南向应用失败", e);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_DELETE,
                    "删除三方对接南向应用" + resolveAppIdentifier(Objects.nonNull(app) ? app.getSystemName() : null, callableId));
            return R.failure(e.getMessage());
        }
    }

    private String resolveAppIdentifier(String systemName, Long callableId) {
        if (StringUtils.isNotBlank(systemName)) {
            return "\"" + systemName + "【" + callableId + "】\"";
        }
        return "id=" + callableId;
    }


    /**
     * 更新南向应用mapper
     *
     * @param mapperVo 更新南向应用mapper
     */
    @PutMapping("/app/callable/{callableId}/mapper")
    @Operation(summary = "更新南向应用mapper", description = "更新南向应用mapper")
    public R<Void> updateAppCallableMapper(@PathVariable("callableId") Long callableId,
                                           @Valid @RequestBody AppCallableMapperVo mapperVo) {
        AppCallable app = null;
        try {
            app = service.getById(callableId);
            if (Objects.isNull(app)) {
                reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_MAPPER_UPDATE,
                        "修改三方对接南向应用映射配置[id=" + callableId + "]不存在");
                return R.failure("该南向应用不存在");
            }
            service.updateAppCallableMapper(callableId, mapperVo);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_MAPPER_UPDATE,
                    String.format(OperationTypeEnum.GROUP_THIRD_APP_MAPPER_UPDATE.getDesc(), app.getSystemName(),
                            callableId, buildUpdateMapperLog(mapperVo)),
                    MSIPConstant.OPERATION_SUCCESS);
            return R.success();
        } catch (Exception e) {
            log.error("更新南向应用mapper失败", e);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_MAPPER_UPDATE,
                    "修改三方对接南向应用" + resolveAppIdentifier(Objects.nonNull(app) ? app.getSystemName() : null, callableId) + "的映射配置");
            return R.failure(e.getMessage());
        }
    }

    /**
     * 查询南向应用详情
     *
     * @param callableId 主键id
     * @return 南向应用详情
     */
    @GetMapping("/app/callable/{callableId}")
    @Operation(summary = "查询南向应用详情", description = "查询南向应用详情")
    public R<AppCallableDetailVo> getAppCallable(@PathVariable("callableId") Long callableId) {
        return R.success(service.getAppCallable(callableId));
    }

    /**
     * 设置事务数据任务标准件派发配置
     *
     * @param callableId 南向应用id
     * @param configVo  任务标准件派发配置（enableTask、taskAutoFillConfig 均可为空，为空时使用默认值）
     */
    @PostMapping("/app/callable/{callableId}/task-config")
    @Operation(summary = "设置事务数据任务标准件派发配置", description = "设置事务数据任务标准件派发配置")
    public R<Void> updateTaskConfig(@PathVariable("callableId") Long callableId,
                                    @RequestBody AppCallableTaskConfigVo configVo) {
        AppCallable app = null;
        try {
            app = service.getById(callableId);
            if (Objects.isNull(app)) {
                reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_TASK_CONFIG_UPDATE,
                        "修改三方对接南向应用任务标准件派发配置[id=" + callableId + "]不存在");
                return R.failure("该南向应用不存在");
            }
            service.updateTaskConfig(callableId, configVo);
            Integer enableTask = configVo == null || configVo.getEnableTask() == null ? 0 : configVo.getEnableTask();
            String autoFillConfig = configVo == null || StringUtils.isBlank(configVo.getTaskAutoFillConfig())
                    ? "[]" : configVo.getTaskAutoFillConfig();
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_TASK_CONFIG_UPDATE,
                    String.format(OperationTypeEnum.GROUP_THIRD_APP_TASK_CONFIG_UPDATE.getDesc(),
                            app.getSystemName(), callableId, getEnableTaskStr(enableTask), autoFillConfig),
                    MSIPConstant.OPERATION_SUCCESS);
            return R.success();
        } catch (Exception e) {
            log.error("设置任务标准件派发配置失败", e);
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_THIRD_APP_TASK_CONFIG_UPDATE,
                    "修改三方对接南向应用" + resolveAppIdentifier(Objects.nonNull(app) ? app.getSystemName() : null, callableId)
                            + "的任务标准件派发配置");
            return R.failure(e.getMessage());
        }
    }

    /**
     * 获取事务数据任务标准件派发配置
     *
     * @param callableId 南向应用id
     * @return 任务标准件派发配置
     */
    @GetMapping("/app/callable/{callableId}/task-config")
    @Operation(summary = "获取事务数据任务标准件派发配置", description = "获取事务数据任务标准件派发配置")
    public R<AppCallableTaskConfigVo> getTaskConfig(@PathVariable("callableId") Long callableId) {
        return R.success(service.getTaskConfig(callableId));
    }

    /**
     * 获取南向应用对应数据表列表
     *
     * @return 南向应用列表
     */
    @GetMapping("/app/callable/{callableId}/tables")
    @Operation(summary = "获取南向应用对应数据表列表", description = "获取南向应用对应数据表列表-不分页")
    public R<List<String>> listAppCallableTables(@PathVariable("callableId") Long callableId) {
        return R.success(service.listAppCallableTables(callableId));
    }

    /**
     * 获取事务转成的任务状态
     *
     * @param callableId  主键id
     * @param tableDataId 数据表ID，对应的是业务数据的linkx_id
     * @param tableName   表名
     * @return 任务状态
     */
    @GetMapping("/app/callable/{callableId}/data/tasks/status")
    @Operation(summary = "获取事务转成的任务状态", description = "获取事务转成的任务状态")
    public R<TasksVO> getTaskStatus(@PathVariable("callableId") Long callableId,
                                    @RequestParam(value = "tableDataId") Long tableDataId,
                                    @RequestParam(value = "tableName") String tableName) {
        return R.success(service.getTaskStatus(callableId, tableDataId, tableName));
    }

    /**
     * 分页查询可调用南向应用的数据
     *
     * @param callableId 应用id
     * @param table      表名
     * @param page       当前页码
     * @param pageSize   每页记录数
     * @return 分页结果
     */
    @Operation(summary = "分页查询可调用南向应用的数据", description = "根据条件分页查询可调用南向应用的数据")
    @GetMapping("/app/callable/{callableId}/data")
    public R<Page<JSONObject>> pageAppData(@PathVariable("callableId") Long callableId,
                                           @Parameter(description = "表名", example = "tb_user") @RequestParam("table") String table,
                                           @Parameter(description = "当前页码", example = "1") @Min(1) @RequestParam(defaultValue = "1") Long page,
                                           @Parameter(description = "每页记录数", example = "10") @Min(1) @RequestParam(defaultValue = "10") Long pageSize,
                                           @Parameter(description = "搜索字段名", example = "linkx_id") @RequestParam(required = false) String searchKey,
                                           @Parameter(description = "搜索字段值", example = "123456") @RequestParam(required = false) String searchValue) {
        SearchPair<String, String> searchPair = (StringUtils.isNotBlank(searchKey) && StringUtils.isNotBlank(searchValue))
                ? new SearchPair<>(searchKey, searchValue) : null;
        return R.success(service.pageAppData(callableId, table, page, pageSize, searchPair));
    }

    /**
     * 转换事务为任务
     *
     * @param
     * @return 南向应用列表
     */
    @PostMapping("/app/callable/{callableId}/data/tasks")
    @Operation(summary = "转换事务为任务", description = "转换事务为任务")
    public R<String> appDataToTask(@PathVariable("callableId") Long callableId,
                                   @Valid @RequestBody AppCallableToTaskVo toTaskVo) {
        return R.success(service.appDataToTask(callableId, toTaskVo));
    }

    /**
     * 获取事务数据的字段信息
     *
     * @param callableId 南向应用id
     * @param tableName  表名
     * @return 南向应用字段信息
     */
    @GetMapping("/app/callable/{callableId}/data/columnInfo")
    @Operation(summary = "事务数据的字段信息", description = "事务数据的字段信息")
    public R<List<AppCallableDataTableColumnInfoVo>> getAppDataColumnInfo(@PathVariable("callableId") Long callableId,
                                                                          @RequestParam("tableName") String tableName) {
        return R.success(service.getAppDataColumnInfo(callableId, tableName));
    }

    /**
     * 任务标准件获取事务数据的字段信息
     * @param taskNo 任务编号
     * @param taskId 任务ID
     * @return 南向应用字段信息
     */
    @GetMapping("/app/callable/data/columnInfo")
    @Operation(summary = "事务数据的字段信息", description = "事务数据的字段信息")
    public R<List<AppCallableDataTableColumnInfoVo>> getAppDataColumnInfo4App(@RequestParam(value = "taskId", required = false) Long taskId,
                                                                              @RequestParam(value = "taskNo", required = false) String taskNo) {
        return R.success(service.getAppDataColumnInfo4App(taskId, taskNo));
    }

    private void checkParam(AppCallableCreateDto dto) {
        if (AppTypeEnum.API.getCode().equals(dto.getType())) {
            check4Api(dto);
        } else {
            check4Db(dto);
        }
    }

    private void check4Db(AppCallableCreateDto dto) {
        AssertUtils.check(Objects.nonNull(dto.getDbType()), "数据库类型不能为空");
        AssertUtils.check(StringUtils.isNotBlank(dto.getAccount()), "数据库账号不能为空");
        AssertUtils.check(StringUtils.isNotBlank(dto.getPassword()), "数据库密码不能为空");
        AssertUtils.check(StringUtils.isNotBlank(dto.getDatabaseName()), "数据库库名不能为空");
        AssertUtils.check(StringUtils.isNotBlank(dto.getDataName()), "数据库表名/视图不能为空");
        // 动态表名存在sql注入风险，需要校验
        AssertUtils.check(MySqlTableNameValidator.isValidTableName(dto.getDatabaseName()), "非法的数据库表名");
    }

    private void check4Api(AppCallableCreateDto dto) {
        AssertUtils.check(StringUtils.isNotBlank(dto.getProtocol()), "访问协议不能为空");
        AssertUtils.check(StringUtils.isNotBlank(dto.getMethod()), "请求方法不能为空");
        AssertUtils.check(StringUtils.isNotBlank(dto.getUri()), "访问URI地址不能为空");
        AssertUtils.check(dto.getDataStartTime() != null, "三方数据库的第一条数据开始时间不能为空");
        AssertUtils.check(StringUtils.isNotBlank(dto.getDateTimeSign()), "三方数据库的时间的字段不能为空");
        if (dto.getPagenation() != null && dto.getPagenation() == 1) {
            AssertUtils.check(Objects.nonNull(dto.getPagenationType()), "分页时，分页模式不能为空");
            AssertUtils.check(Objects.nonNull(dto.getPageParamLocation()), "分页时，分页位置不能为空");
            AssertUtils.check(StringUtils.isNotBlank(dto.getPageFieldName()), "分页时，页码字段不能为空");
            AssertUtils.check(StringUtils.isNotBlank(dto.getPageSizeFieldName()), "分页时，页面条目数字段不能为空");
        }
    }

    private String buildCreateLog(AppCallableCreateDto dto) {
        // 记录操作日志
        StringBuilder logInfo = new StringBuilder();
        logInfo.append("名称：").append(dto.getName()).append(Constants.SEMICOLON);
        logInfo.append("所属系统：").append(dto.getSystemName()).append(Constants.SEMICOLON);
        logInfo.append("系统编码：").append(dto.getSystemCode()).append(Constants.SEMICOLON);
        logInfo.append("唯一标识字段：").append(dto.getUniqueId()).append(Constants.SEMICOLON);
        logInfo.append("应用类型：").append(getAppTypeStr(dto.getType())).append(Constants.SEMICOLON);
        logInfo.append("展示范围：").append(getScopeStr(dto.getScope())).append(Constants.SEMICOLON);
        logInfo.append("访问IP：").append(dto.getIp()).append(Constants.SEMICOLON);
        logInfo.append("端口：").append(dto.getPort()).append(Constants.SEMICOLON);
        boolean isApi = AppTypeEnum.API.getCode().equals(dto.getType());
        buildCreateDetailLog(isApi, logInfo, dto);
        logInfo.append("数据刷新周期：").append(dto.getPeriod());
        return logInfo.toString();
    }

    private String buildUpdateMapperLog(AppCallableMapperVo mapperVo) {
        // 组装更新mapper的操作日志
        if (StringUtils.isBlank(mapperVo.getMapper())) {
            return StringUtils.EMPTY;
        }
        List<KeyValue> keyValues = JSON.parseArray(mapperVo.getMapper(), KeyValue.class);
        StringBuilder logInfo = new StringBuilder();
        keyValues.forEach(kv -> {
            logInfo.append("字段名：").append(kv.getKey())
                    .append(Constants.COMMA)
                    .append("字段含义：")
                    .append(kv.getValue()).append(Constants.SEMICOLON);
        });
        return logInfo.toString();
    }

    private String buildUpdateLog(AppCallableUpdateDto  updateDto, AppCallableDetailVo dto) {
        // 更新的操作日志
        String baseFormat = "%s由[%s]修改为[%s];";
        StringBuilder logInfo = new StringBuilder();
        ConditionExecuteUtils.execute(!StringUtils.equals(updateDto.getName(), dto.getName()),
                () -> logInfo.append(String.format(baseFormat, "名称", dto.getName(), updateDto.getName())));
        ConditionExecuteUtils.execute(!StringUtils.equals(updateDto.getSystemName(), dto.getSystemName()),
                () -> logInfo.append(String.format(baseFormat, "所属系统", dto.getSystemName(), updateDto.getSystemName())));
        ConditionExecuteUtils.execute(!StringUtils.equals(updateDto.getUniqueId(), dto.getUniqueId()),
                () -> logInfo.append(String.format(baseFormat, "唯一标识字段", dto.getUniqueId(), updateDto.getUniqueId())));
        // 判断应用类型是否修改
        boolean appTypeChanged = !Objects.equals(dto.getType(), updateDto.getType());
        ConditionExecuteUtils.execute(appTypeChanged,
                () -> logInfo.append(String.format(baseFormat, "应用类型", getAppTypeStr(dto.getType()), getAppTypeStr(updateDto.getType()))));
        ConditionExecuteUtils.execute(!Objects.equals(updateDto.getScope(), dto.getScope()),
                () -> logInfo.append(String.format(baseFormat, "展示范围", getScopeStr(dto.getScope()), getScopeStr(updateDto.getScope()))));
        ConditionExecuteUtils.execute(!StringUtils.equals(updateDto.getIp(), dto.getIp()),
                () -> logInfo.append(String.format(baseFormat, "访问IP", dto.getIp(), updateDto.getIp())));
        ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPort(), dto.getPort()),
                () -> logInfo.append(String.format(baseFormat, "端口", dto.getPort(), updateDto.getPort())));
        boolean isApi = AppTypeEnum.API.getCode().equals(updateDto.getType());
        if (appTypeChanged) {
            buildCreateDetailLog(isApi, logInfo, updateDto);
        } else {
            buildUpdateDetailLog(isApi, logInfo, baseFormat, dto, updateDto);
        }
        ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPeriod(), dto.getPeriod()),
                () -> logInfo.append(String.format(baseFormat, "数据刷新周期", dto.getPeriod(), updateDto.getPeriod())));
        return logInfo.toString();
    }

    private void buildCreateDetailLog(boolean isApiType, StringBuilder logInfo, AppCallableCreateDto dto) {
        if (isApiType) {
            logInfo.append("访问协议：").append(dto.getProtocol()).append(Constants.SEMICOLON);
            logInfo.append("接口URI：").append(dto.getUri()).append(Constants.SEMICOLON);
            logInfo.append("请求方式：").append(dto.getMethod()).append(Constants.SEMICOLON);
            logInfo.append("请求Header：").append(dto.getReqHeader()).append(Constants.SEMICOLON);
            logInfo.append("请求Body：").append(dto.getReqBody()).append(Constants.SEMICOLON);
            logInfo.append("请求Params：").append(dto.getReqParam()).append(Constants.SEMICOLON);
            logInfo.append("是否分页：").append(getPagenationStr(dto.getPagenation())).append(Constants.SEMICOLON);
            if (dto.getPagenation() != null && dto.getPagenation() == 1) {
                logInfo.append("分页类型：").append(getPagenationTypeStr(dto.getPagenationType())).append(Constants.SEMICOLON);
                logInfo.append("分页参数类型：").append(getPageParamLocationStr(dto.getPageParamLocation())).append(Constants.SEMICOLON);
                logInfo.append("页码字段：").append(dto.getPageFieldName()).append(Constants.SEMICOLON);
                logInfo.append("每页条数字段：").append(dto.getPageSizeFieldName()).append(Constants.SEMICOLON);
                logInfo.append("响应数据路径：").append(dto.getResponseDataPath()).append(Constants.SEMICOLON);
            }
        } else {
            logInfo.append("表名/视图名：").append(dto.getDataName()).append(Constants.SEMICOLON);
            logInfo.append("数据库名：").append(dto.getDatabaseName()).append(Constants.SEMICOLON);
            logInfo.append("数据库类型：").append(getDbTypeStr(dto.getDbType())).append(Constants.SEMICOLON);
            logInfo.append("数据库账号：").append(dto.getAccount()).append(Constants.SEMICOLON);
            logInfo.append("数据库密码：").append(dto.getPassword()).append(Constants.SEMICOLON);
        }
    }

    private String getAppTypeStr(Integer type) {
        return AppTypeEnum.API.getCode().equals(type) ? AppTypeEnum.API.getMsg() : AppTypeEnum.DB.getMsg();
    }

    private String getDbTypeStr(Integer dbType) {
        DbDialect dialect = DbDialect.getByCode(dbType);
        return dialect == null ? StringUtils.EMPTY : dialect.name();
    }

    private String getPagenationStr(Integer pagenation) {
        return pagenation != null && pagenation == 1 ? "是" : "否";
    }

    private String getPagenationTypeStr(Integer pagenationType) {
        return pagenationType != null && pagenationType == 1 ? "页码模式" : "偏移模式";
    }

    private String getPageParamLocationStr(Integer location) {
        return location != null && location == 0 ? "Query参数" : "Body参数";
    }

    private String getScopeStr(Integer scope) {
        return (scope == null || scope == 0) ? "全部" : (scope == 1 ? "PC端" : "移动端");
    }

    private String getEnableTaskStr(Integer enableTask) {
        return enableTask != null && enableTask == 1 ? "是" : "否";
    }

    private void buildUpdateDetailLog(boolean isApiType, StringBuilder logInfo,
                                      String baseFormat, AppCallableDetailVo dto, AppCallableUpdateDto updateDto) {
        if (isApiType) {
            ConditionExecuteUtils.execute(!StringUtils.equals(updateDto.getProtocol(), dto.getProtocol()),
                    () -> logInfo.append(String.format(baseFormat, "访问协议", dto.getProtocol(), updateDto.getProtocol())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getUri(), dto.getUri()),
                    () -> logInfo.append(String.format(baseFormat, "接口URI", dto.getUri(), updateDto.getUri())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getMethod(), dto.getMethod()),
                    () -> logInfo.append(String.format(baseFormat, "请求方式", dto.getMethod(), updateDto.getMethod())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getReqHeader(), dto.getReqHeader()),
                    () -> logInfo.append(String.format(baseFormat, "请求Header", dto.getReqHeader(), updateDto.getReqHeader())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getReqBody(), dto.getReqBody()),
                    () -> logInfo.append(String.format(baseFormat, "请求Body", dto.getReqBody(), updateDto.getReqBody())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getReqParam(), dto.getReqParam()),
                    () -> logInfo.append(String.format(baseFormat, "请求Params", dto.getReqParam(), updateDto.getReqParam())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPagenation(), dto.getPagenation()),
                    () -> logInfo.append(String.format(baseFormat, "是否分页", getPagenationStr(dto.getPagenation()),
                            getPagenationStr(updateDto.getPagenation()))));
            if (dto.getPagenation() != null && dto.getPagenation() == 1) {
                ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPagenationType(), dto.getPagenationType()),
                        () -> logInfo.append(String.format(baseFormat, "分页类型",
                                getPagenationTypeStr(dto.getPagenationType()), getPagenationTypeStr(updateDto.getPagenationType()))));
                ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPageParamLocation(), dto.getPageParamLocation()),
                        () -> logInfo.append(String.format(baseFormat, "分页参数类型",
                                getPageParamLocationStr(dto.getPageParamLocation()), getPageParamLocationStr(updateDto.getPageParamLocation()))));
                ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPageFieldName(), dto.getPageFieldName()),
                        () -> logInfo.append(String.format(baseFormat, "页码字段", dto.getPageFieldName(), updateDto.getPageFieldName())));
                ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPageSizeFieldName(), dto.getPageSizeFieldName()),
                        () -> logInfo.append(String.format(baseFormat, "每页条数字段", dto.getPageSizeFieldName(), updateDto.getPageSizeFieldName())));
                ConditionExecuteUtils.execute(!Objects.equals(updateDto.getResponseDataPath(), dto.getResponseDataPath()),
                        () -> logInfo.append(String.format(baseFormat, "响应数据路径", dto.getResponseDataPath(), updateDto.getResponseDataPath())));
            }
        } else {
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getDataName(), dto.getDataName()),
                    () -> logInfo.append(String.format(baseFormat, "表名/视图名", dto.getDataName(), updateDto.getDataName())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getDatabaseName(), dto.getDatabaseName()),
                    () -> logInfo.append(String.format(baseFormat, "数据库名", dto.getDatabaseName(), updateDto.getDatabaseName())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getDbType(), dto.getDbType()),
                    () -> logInfo.append(String.format(baseFormat, "数据库类型",
                            getDbTypeStr(dto.getDbType()), getDbTypeStr(updateDto.getDbType()))));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getAccount(), dto.getAccount()),
                    () -> logInfo.append(String.format(baseFormat, "数据库账号", dto.getAccount(), updateDto.getAccount())));
            ConditionExecuteUtils.execute(!Objects.equals(updateDto.getPassword(), dto.getPassword()),
                    () -> logInfo.append(String.format(baseFormat, "数据库密码", dto.getPassword(), updateDto.getPassword())));
        }
    }
}