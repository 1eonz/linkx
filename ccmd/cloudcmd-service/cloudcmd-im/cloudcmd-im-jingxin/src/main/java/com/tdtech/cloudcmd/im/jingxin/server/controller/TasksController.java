package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.MyTasksCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksDateCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksStatusStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksTypeCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksTypeStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksConfigVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksAttachmentService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksFavoriteService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksProcessesService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;
import com.tdtech.cloudcmd.im.jingxin.server.service.TasksConfigService;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author ly
 * @date 2025/8/26
 **/
@Slf4j
@RestController
@Tag(name = "三方任务")
@RequestMapping("/collaboration/v1/tasks")
@RequiredArgsConstructor
@Licensed(module = LicenseEnum.TASK_COORDINATION)
public class TasksController {

    @Resource
    private ITasksService tasksService;

    @Resource
    private ITasksProcessesService tasksProcessesService;

    @Resource
    private ITasksFavoriteService tasksFavoriteService;

    @Resource
    private ITasksAttachmentService tasksAttachmentService;

    @Resource
    private TasksConfigService tasksConfigService;

    /**
     * 查询 PC 展示的任务标准件模块列表
     *
     * @return 模块列表
     */
    @GetMapping("/config/modules")
    @Operation(summary = "查询PC展示的任务标准件模块列表",
            description = "查询 tb_tasks_config 中 show_in_pc=1 的模块列表，用于 PC 端任务 tab 页展示")
    public R<List<TasksConfigVO>> listPcModules() {
        return R.success(tasksConfigService.listPcModules());
    }

    /**
     * 通过模块名称分页查询任务列表
     *
     * @param module   模块名称（必填）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param name     任务名称关键字
     * @param content  任务内容关键字
     * @param scope    范围：1全部(默认)、2创建人、3执行人、4创建人+执行人
     * @param idCard   身份证号；scope != 1 时未传则取当前登录人身份证号
     * @return 任务分页
     */
    @GetMapping("/config/tasks/page")
    @Operation(summary = "通过模块名称分页查询任务列表",
            description = "按 module 过滤，支持按任务名称/任务内容关键字搜索分页；"
                    + "scope 过滤范围：1全部(默认)、2创建人、3执行人、4创建人+执行人；"
                    + "idCard 在 scope!=1 时未传则取当前登录人身份证号")
    public R<Page<TasksVO>> pageTasksByModule(
            @Parameter(description = "任务标准件模块名称", required = true)
            @RequestParam String module,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String content,
            @Parameter(description = "范围：1全部(默认)、2创建人、3执行人、4创建人+执行人")
            @RequestParam(required = false, defaultValue = "1") Integer scope,
            @Parameter(description = "身份证号；scope != 1 时未传则取当前登录人")
            @RequestParam(required = false) String idCard) {
        int scopeValue = (scope == null) ? 1 : scope;
        String idCardValue = idCard;
        if (scopeValue != 1 && StringUtils.isBlank(idCardValue)) {
            UserInfo user = SecurityUtils.getUser();
            if (Objects.isNull(user)) {
                throw new SecurityUtils.UnAuthException("access token invalid");
            }
            idCardValue = user.getIdCardNum();
        }
        return R.success(tasksConfigService.pageTasksByModule(module, pageNum, pageSize, name, content, scopeValue, idCardValue));
    }

    /**
     * 分页查询任务
     *
     * @param pageNum
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
    @Operation(summary = "分页查询任务", description = "根据条件分页查询任务列表")
    public R<Page<TasksVO>> tasksPage(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String keywords,
        @RequestParam(required = false) String executor,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime,
        @RequestParam(required = false) String status, @RequestParam(required = false) String businessType,
        @RequestParam(required = false) String level, @RequestParam(required = false) Integer urgent) {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        TasksQueryReq queryReq =
            TasksQueryReq.builder().keywords(keywords).executor(executor).startTime(startTime).endTime(endTime)
                .status(status).businessType(businessType).level(level).urgent(urgent).idCard(user.getIdCardNum())
                .build();
        return R.success(tasksService.tasksPage(pageNum, pageSize, queryReq));
    }

    /**
     * 删除任务
     *
     * @param taskNumber
     * @param tasksDelReq
     * @return
     */
    @DeleteMapping("/{taskNumber}")
    @Operation(summary = "删除任务", description = "根据任务编号删除指定任务")
    public R<String> delete(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksDelReq tasksDelReq) {
        tasksService.delete(taskNumber, tasksDelReq);
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
    @Operation(summary = "修改任务", description = "根据任务编号修改指定任务信息")
    public R<String> update(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksUpdateReq tasksUpdateReq) {
        tasksService.update(taskNumber, tasksUpdateReq);
        return R.success(taskNumber);
    }

    /**
     * 查询任务详情
     *
     * @param taskNumber
     * @return
     */
    @GetMapping("/{taskNumber}")
    @Operation(summary = "查询任务详情", description = "根据任务编号查询任务详细信息")
    public R<TasksVO> detail(@PathVariable("taskNumber") String taskNumber) {
        return R.success(tasksService.detail(taskNumber));
    }

    /**
     * 新增任务
     *
     * @param tasksCreateReq
     * @return
     */
    @PostMapping
    @Operation(summary = "新增任务", description = "创建一个新的任务")
    public R<TasksVO> saveTasks(@RequestBody @Valid TasksCreateReq tasksCreateReq) {
        return R.success(tasksService.save(tasksCreateReq));
    }

    /**
     * 任务处置
     *
     * @param taskNumber
     * @param tasksProcessesCreateReq
     * @return
     */
    @PutMapping("/{taskNumber}/processes")
    @Operation(summary = "任务处置", description = "对指定任务进行处置操作")
    public R<String> saveTasksProcesses(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksProcessesCreateReq tasksProcessesCreateReq) {
        tasksProcessesService.save(taskNumber, tasksProcessesCreateReq);
        return R.success(taskNumber);
    }

    /**
     * 分页查询任务处理记录
     *
     * @param taskNumber
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/{taskNumber}/processes")
    @Operation(summary = "分页查询任务处理记录", description = "根据任务编号分页查询任务的处理记录")
    public R<Page<TasksProcesses>> tasksProcessesPage(@PathVariable("taskNumber") String taskNumber,
        @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(tasksProcessesService.page(pageNum, pageSize, taskNumber));
    }

    /**
     * 查询备注不为空的任务处置列表
     *
     * @param taskNumber
     * @return
     */
    @GetMapping("/{taskNumber}/processes/list")
    @Operation(summary = "查询备注不为空的任务处置列表", description = "根据任务编号查询备注不为空的任务处置列表")
    public R<List<TasksProcesses>> tasksProcessesList(@PathVariable("taskNumber") String taskNumber) {
        return R.success(tasksProcessesService.list(taskNumber));
    }

    /**
     * 按任务状态统计数量
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/status/count")
    @Operation(summary = "按任务状态统计数量", description = "按任务状态统计指定时间范围内的任务数量")
    public R<List<TasksCountVO>> statusCount(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.statusCount(startTime, endTime));
    }

    /**
     * 按任务类型统计数量
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/type/count")
    @Operation(summary = "按任务类型统计数量", description = "按任务类型统计指定时间范围内的任务数量")
    public R<List<TasksTypeCountVO>> typeCount(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.typeCount(startTime, endTime));
    }

    /**
     * 按任务类型统计数量
     *
     */
    @GetMapping("/statistics/type/list")
    @Operation(summary = "当前存在的任务类型", description = "当前存在的任务类型")
    public R<List<String>> typeCount() {
        return R.success(tasksService.typeList());
    }

    /**
     * 按任务类型统计
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/type")
    @Operation(summary = "按任务类型统计", description = "按任务类型统计指定时间范围内的任务数据")
    public R<List<TasksTypeStatisticsVO>> statisticsByBusinessType(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.statisticsByBusinessType(startTime, endTime));
    }

    /**
     * 按任务完成度统计-按任务状态维度统计
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/status/completeness")
    @Operation(summary = "按任务完成度统计-按任务状态维度统计",
        description = "按任务完成度统计指定时间范围内的任务数据（按任务状态维度）")
    public R<List<TasksStatusStatisticsVO>> statisticsStatusByCompleteness(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.statisticsStatusByCompleteness(startTime, endTime));
    }

    /**
     * 任务发起趋势统计
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/date")
    @Operation(summary = "任务发起趋势统计", description = "统计指定时间范围内任务发起的趋势数据")
    public R<List<TasksDateCountVO>> statisticsByDate(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.statisticsByDate(startTime, endTime));
    }

    /**
     * 我的待办任务数量
     *
     * @return
     */
    @GetMapping("/statistics/mine/todo/count")
    @Operation(summary = "查询我的待办任务数量", description = "查询我的待办任务数量")
    public R<Integer> mineToDoCount() {
        return R.success(tasksService.mineToDoCount());
    }

    /**
     * 任务量分析
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/mine/count")
    @Operation(summary = "任务量分析", description = "分析指定用户在指定时间范围内的任务量")
    public R<List<MyTasksCountVO>> mineCount(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.mineCount(startTime, endTime));
    }

    /**
     * 任务完成率分析
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/mine/completeness")
    @Operation(summary = "任务完成率分析", description = "分析指定用户在指定时间范围内的任务完成率")
    public R<List<TasksTypeStatisticsVO>> mineCompleteness(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.statisticsByCompleteness(startTime, endTime));
    }

    /**
     * 任务平均完成时间分析
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/statistics/mine/avg/duration")
    @Operation(summary = "任务平均完成时间分析", description = "分析指定用户在指定时间范围内的任务平均完成时间")
    public R<List<TasksTypeStatisticsVO>> avgDuration(
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime) {
        return R.success(tasksService.avgDuration(startTime, endTime));
    }

    /**
     * 分页查询我的收藏任务
     *
     * @param pageNum
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
    @GetMapping("/favorite/page")
    @Operation(summary = "分页查询我的收藏任务", description = "根据条件分页查询当前用户收藏的任务列表")
    public R<Page<TasksVO>> tasksFavoritePage(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String keywords,
        @RequestParam(required = false) String executor,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime,
        @RequestParam(required = false) String status, @RequestParam(required = false) String businessType,
        @RequestParam(required = false) String level, @RequestParam(required = false) Integer urgent) {
        List<String> businessTypeList = new ArrayList<>();
        if (StringUtils.isNotBlank(businessType)) {
            businessTypeList = Arrays.asList(businessType.split(","));
        }
        List<String> levelList = new ArrayList<>();
        if (StringUtils.isNotBlank(level)) {
            levelList = Arrays.asList(level.split(","));
        }
        UserInfo user = SecurityUtils.getUser();
        TasksQueryReq queryReq =
            TasksQueryReq.builder().keywords(keywords).executor(executor).startTime(startTime).endTime(endTime)
                .status(status).businessTypeList(businessTypeList)
                .idCard(Objects.nonNull(user) ? user.getIdCardNum() : null).levelList(levelList).urgent(urgent).build();
        return R.success(tasksFavoriteService.page(pageNum, pageSize, queryReq));
    }

    /**
     * 收藏任务
     *
     * @param taskNumber
     * @param tasksFavoriteCreateReq
     * @return
     */
    @PostMapping("/{taskNumber}/favorite")
    @Operation(summary = "收藏任务", description = "将指定任务添加到当前用户的收藏列表中")
    public R<String> saveFavorite(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksFavoriteCreateReq tasksFavoriteCreateReq) {
        tasksFavoriteService.save(taskNumber, tasksFavoriteCreateReq);
        return R.success(taskNumber);
    }

    /**
     * 取消收藏
     *
     * @param taskNumber
     * @param tasksFavoriteDelReq
     * @return
     */
    @DeleteMapping("/{taskNumber}/favorite")
    @Operation(summary = "取消收藏", description = "从当前用户的收藏列表中移除指定任务")
    public R<String> deleteFavorite(@PathVariable("taskNumber") String taskNumber,
        @RequestBody @Valid TasksFavoriteDelReq tasksFavoriteDelReq) {
        tasksFavoriteService.delete(taskNumber, tasksFavoriteDelReq);
        return R.success(taskNumber);
    }

    /**
     * 上传任务附件
     *
     * @param file       附件文件
     * @return 附件信息
     */
    @PostMapping("/upload/attachment")
    @Operation(summary = "上传任务附件", description = "上传任务附件")
    public R<TasksAttachmentVO> uploadAttachment(
            @Parameter(description = "附件文件") @RequestParam("file") MultipartFile file) {
        return R.success(tasksAttachmentService.uploadAttachment(file));
    }

    @PostMapping("/{taskNumber}/attachment")
    @Operation(summary = "保存任务附件信息并修改任务状态", description = "保存任务附件信息并修改任务状态")
    public R<Void> saveAttachment(@PathVariable("taskNumber") String taskNumber,
                                               @Valid @RequestBody TasksAttachmentCreateReq createReq) {
        tasksAttachmentService.save(taskNumber, createReq);
        return R.success();
    }

    /**
     * 查询任务附件列表
     *
     * @param taskNumber 任务编号
     * @return 附件列表
     */
    @GetMapping("/{taskNumber}/attachment")
    @Operation(summary = "查询任务附件列表", description = "查询指定任务的所有附件")
    public R<List<TasksAttachmentVO>> listAttachments(@PathVariable("taskNumber") String taskNumber) {
        return R.success(tasksAttachmentService.listByTaskNumber(taskNumber));
    }

    /**
     * 删除任务附件
     *
     * @param taskNumber 任务编号
     * @param id         附件ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{taskNumber}/attachment/{id}")
    @Operation(summary = "删除任务附件", description = "删除指定的任务附件")
    public R<Boolean> deleteAttachmentById(
            @PathVariable("taskNumber") String taskNumber,
            @PathVariable(value = "id") Long id) {
        return R.success(tasksAttachmentService.deleteById(id));
    }

    /**
     * 删除任务附件
     *
     * @param taskNumber 任务编号
     * @return 是否删除成功
     */
    @DeleteMapping("/{taskNumber}/v2/attachment")
    @Operation(summary = "删除任务附件", description = "删除指定的任务附件")
    public R<Boolean> deleteAttachment(
            @PathVariable("taskNumber") String taskNumber,
            @RequestBody @Valid TasksAttachmentDelReq tasksAttachmentDelReq) {
        return R.success(tasksAttachmentService.deleteFile(tasksAttachmentDelReq));
    }

    /**
     * 删除任务所有附件
     *
     * @param taskNumber 任务编号
     * @return 是否删除成功
     */
    @DeleteMapping("/{taskNumber}/attachment")
    @Operation(summary = "删除任务所有附件", description = "删除指定任务的所有附件")
    public R<Boolean> deleteAllAttachments(@PathVariable("taskNumber") String taskNumber) {
        return R.success(tasksAttachmentService.deleteByTaskNumber(taskNumber));
    }
}