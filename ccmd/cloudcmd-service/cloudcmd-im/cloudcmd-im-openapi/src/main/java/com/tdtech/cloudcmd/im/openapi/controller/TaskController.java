package com.tdtech.cloudcmd.im.openapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.TaskRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.*;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.OpenTasksConfigVO;
import com.tdtech.cloudcmd.im.openapi.service.OpenTasksConfigService;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

@Slf4j
@Tag(name = "任务管理相关接口", description = "任务管理相关接口")
@RestController
@RequestMapping("/openapi/v1/tasks")
@RequiredArgsConstructor
@OpenApiOauth(BusinessScopeEnum.THIRD_PARTY_TASKS)
@RequestLimit(business = "任务管理")
@Licensed(module = LicenseEnum.TASK_COORDINATION)
public class TaskController {
    @DubboReference
    private TaskRpcApi taskRpcApi;

    private final OpenTasksConfigService OpenTasksConfigService;

    /**
     * 设置任务标准件配置
     *
     * @param vo 配置请求（module、showInPc）
     * @return 设置结果
     */
    @PostMapping("/config")
    @Operation(summary = "设置任务标准件配置", description = "为当前调用方设置任务标准件模块的PC展示配置")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<Boolean> setTasksConfig(@RequestBody OpenTasksConfigVO vo) {
        try {
            OpenTasksConfigService.setConfig(vo);
        } catch (Exception e) {
            return R.failure(e.getMessage());
        }
        return R.success(Boolean.TRUE);
    }

    /**
     * 分页查询任务
     * 
     * @param page
     * @param pageSize
     * @param keywords
     * @param executor
     * @param startTime
     * @param endTime
     * @param status
     * @param businessType
     * @param level
     * @param urgent
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询任务列表", description = "分页查询任务列表")
    @Parameter(name = "page", description = "当前页码")
    @Parameter(name = "pageSize", description = "每页条数")
    @Parameter(name = "keywords", description = "搜索关键词")
    @Parameter(name = "executor", description = "执行人身份证")
    @Parameter(name = "startTime", description = "时间范围查询起始时间")
    @Parameter(name = "endTime", description = "时间范围查询结束时间")
    @Parameter(name = "status", description = "任务状态。业务系统任务状态文字描述")
    @Parameter(name = "businessType", description = "业务类型")
    @Parameter(name = "level", description = "任务等级")
    @Parameter(name = "urgent", description = "是否为紧急任务。默认为0（不紧急）")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<Page<TasksVO>> tasksPage(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String keywords, @RequestParam(required = false) String executor,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime,
        @RequestParam(required = false) String status, @RequestParam(required = false) String businessType,
        @RequestParam(required = false) String level, @RequestParam(required = false) Integer urgent) {

        TasksQueryReq queryReq = TasksQueryReq.builder().keywords(keywords).executor(executor).startTime(startTime)
            .endTime(endTime).status(status).businessType(businessType).level(level).urgent(urgent).build();
        return R.success(taskRpcApi.tasksPage(page, pageSize, queryReq));
    }

    /**
     * 删除任务
     * 
     * @param taskNumber
     * @param opUserIdCard
     * @return
     */
    @DeleteMapping("/{taskNumber}")
    @Operation(summary = "删除任务", description = "删除任务")
    @Parameter(name = "taskNumber", description = "业务系统生成的任务编号", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<String> deleteTasks(@PathVariable("taskNumber") String taskNumber, @RequestParam(required = false) String opUserIdCard) {
        TasksDelReq tasksDelReq = new TasksDelReq();
        tasksDelReq.setType(1);
        taskRpcApi.deleteTasks(taskNumber, tasksDelReq);
        return R.success(taskNumber);
    }

    /**
     * 修改任务
     * 
     * @param taskNumber
     * @param tasksUpdateReq
     * @return
     */
    @PutMapping("/{taskNumber}")
    @Operation(summary = "修改任务", description = "修改任务")
    @Parameter(name = "taskNumber", description = "业务系统生成的任务编号", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<String> updateTasks(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksUpdateReq tasksUpdateReq) {
        taskRpcApi.updateTasks(taskNumber, tasksUpdateReq);
        return R.success(taskNumber);
    }

    /**
     * 查询任务详情
     * 
     * @param taskNumber
     * @return
     */
    @GetMapping("/{taskNumber}")
    @Operation(summary = "查询任务详情", description = "查询任务详情")
    @Parameter(name = "taskNumber", description = "业务系统生成的任务编号", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<TasksVO> tasksDetail(@PathVariable("taskNumber") String taskNumber) {
        return R.success(taskRpcApi.tasksDetail(taskNumber));
    }

    /**
     * 新增任务
     * 
     * @param tasksCreateReq
     * @return
     */
    @PostMapping("")
    @Operation(summary = "新增任务", description = "新增任务")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<TasksVO> saveTasks(@RequestBody @Valid TasksCreateReq tasksCreateReq) {
        try {
            // 确保当前调用方有该 module 的配置记录，未登记则自动插入 show_in_pc=0
            OpenTasksConfigService.ensureModule(tasksCreateReq.getModule());
            return R.success(taskRpcApi.saveTasks(tasksCreateReq));
        } catch (BusinessException e) {
            log.error("新增任务失败", e);
            return R.failure(e.getMessage());
        }
    }

    /**
     * 任务处置
     * 
     * @param taskNumber
     * @param tasksProcessesCreateReq
     * @return
     */
    @PutMapping("/{taskNumber}/processes")
    @Operation(summary = "任务处置", description = "任务处置")
    @Parameter(name = "taskNumber", description = "业务系统生成的任务编号", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<String> saveTasksProcesses(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksProcessesCreateReq tasksProcessesCreateReq) {
        taskRpcApi.saveTasksProcesses(taskNumber, tasksProcessesCreateReq);
        return R.success(taskNumber);
    }

    /**
     * 分页查询任务处理记录
     * 
     * @param taskNumber
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/{taskNumber}/processes")
    @Operation(summary = "分页查询任务处理记录", description = "分页查询任务处理记录")
    @Parameter(name = "taskNumber", description = "业务系统生成的任务编号", required = true)
    @Parameter(name = "page", description = "当前页码")
    @Parameter(name = "pageSize", description = "每页条数")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<Page<TasksProcesses>> tasksProcessesPage(@PathVariable("taskNumber") String taskNumber,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(taskRpcApi.tasksProcessesPage(taskNumber, page, pageSize));
    }

    /**
     * 收藏任务
     * 
     * @param taskNumber
     * @param tasksFavoriteCreateReq
     * @return
     */
    @PostMapping("/{taskNumber}/favorite")
    @Operation(summary = "收藏任务", description = "收藏任务")
    @Parameter(name = "taskNumber", description = "业务系统生成的任务编号", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<String> saveFavorite(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksFavoriteCreateReq tasksFavoriteCreateReq) {
        taskRpcApi.saveFavorite(taskNumber, tasksFavoriteCreateReq);
        return R.success(taskNumber);
    }

    /**
     * 取消收藏
     * 
     * @param taskNumber
     * @param opUserIdCard
     * @return
     */
    @DeleteMapping("/{taskNumber}/favorite")
    @Operation(summary = "取消收藏", description = "取消收藏")
    @Parameter(name = "taskNumber", description = "业务系统生成的任务编号", required = true)
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    public R<String> deleteFavorite(@PathVariable("taskNumber") String taskNumber, @RequestParam String opUserIdCard) {
        TasksFavoriteDelReq tasksFavoriteDelReq = new TasksFavoriteDelReq();
        tasksFavoriteDelReq.setOpUserId(opUserIdCard);
        taskRpcApi.deleteFavorite(taskNumber, tasksFavoriteDelReq);
        return R.success(taskNumber);
    }
}