package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.enums.NotifyTypeEnum;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksDeleteTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksStatusEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksUrlOpenTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksAttachmentService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExecutorsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksFavoriteService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TrTasksGroupMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.im.jingxin.server.service.impl.GroupExtendsServiceImpl.collectAllOrgCodes;

/**
 * @author ly
 * @date 2025/8/25
 **/
@Slf4j
@Service
public class TasksServiceImpl extends ServiceImpl<TasksMapper, Tasks> implements ITasksService {

    private final String ALL = "全部";

    // 按任务状态统计数量要返回的数据
    private List<String> STATUS_COUNT_LIST =
        Arrays.asList(ALL, TasksStatusEnum.TO_BE_PROCESSED.getMsg(), TasksStatusEnum.IN_PROGRESS.getMsg(),
            TasksStatusEnum.COMPLETED.getMsg());

    @Resource
    private TasksMapper tasksMapper;

    @Resource
    private IdWorker idWorker;

    @Resource
    private ITasksFavoriteService favoriteService;

    @Resource
    private ITasksExecutorsService tasksExecutorsService;

    @Resource
    private StreamBridge streamBridge;

    @Resource
    private TrTasksGroupMapper tasksGroupMapper;

    @Resource
    private ITasksAttachmentService tasksAttachmentService;

    @Override
    public Page<TasksVO> tasksPage(int pageNum, int pageSize, TasksQueryReq tasksQueryReq) {
        //        log.info("tasksPage tasksQueryReq: {}", tasksQueryReq);
        Page<Tasks> page = new Page<>(pageNum, pageSize);
        if (StringUtils.isNotBlank(tasksQueryReq.getBusinessType())) {
            tasksQueryReq.setBusinessTypeList(Arrays.asList(tasksQueryReq.getBusinessType().split(",")));
        }
        if (StringUtils.isNotBlank(tasksQueryReq.getLevel())) {
            tasksQueryReq.setLevelList(Arrays.asList(tasksQueryReq.getLevel().split(",")));
        }
        Page<Tasks> dataPage = tasksMapper.selectPageWithCondition(page, tasksQueryReq);
        List<Tasks> records = dataPage.getRecords();
        List<Long> favoriteTaskIdList =
            favoriteService.favoriteTaskIdList(records.stream().map(Tasks::getId).collect(Collectors.toList()),
                tasksQueryReq.getIdCard());
        List<TasksVO> dataList = records.stream().map(task -> {
            TasksVO vo = TasksVO.fromTasksView(task);
            vo.setFavorite(favoriteTaskIdList.contains(task.getId()));
            return vo;
        }).collect(Collectors.toList());
        Page<TasksVO> resultPage = new Page<>();
        resultPage.setTotal(dataPage.getTotal());
        resultPage.setPages(dataPage.getPages());
        resultPage.setCurrent(dataPage.getCurrent());
        resultPage.setSize(dataPage.getSize());
        resultPage.setRecords(dataList);

        return resultPage;
    }

    @Override
    public boolean delete(String taskNumber, TasksDelReq tasksDelReq) {
        log.info("delete tasks taskNumber: {}, param: {}", taskNumber, tasksDelReq);
        Tasks oldTasks = findOne(taskNumber);
        if (Objects.isNull(oldTasks)) {
            throw new BusinessException("任务不存在");
        }
        TasksDeleteTypeEnum tasksDeleteTypeEnum = TasksDeleteTypeEnum.matchCode(tasksDelReq.getType());
        if (Objects.isNull(tasksDeleteTypeEnum)) {
            throw new BusinessException("删除方式有误");
        }
        switch (tasksDeleteTypeEnum) {
            case REMOVE:
                tasksMapper.deleteById(oldTasks.getId());
            case DISCARDED:
                oldTasks.setUpdateTime(new Date());
                oldTasks.setStatus(tasksDeleteTypeEnum.getMsg());
                tasksMapper.updateById(oldTasks);
        }
        TasksVO tasksVO = TasksVO.fromTasksView(oldTasks);
        tasksVO.setExecutors(tasksExecutorsService.findList(oldTasks.getId()));
        // 删除任务时，同步删除任务执行人
        if(tasksDeleteTypeEnum == TasksDeleteTypeEnum.REMOVE){
            tasksExecutorsService.removeByTaskId(oldTasks.getId());
        }
        sendToCAgentByTasks(NotifyTypeEnum.DELETE.getCode(), tasksVO);
        return true;
    }

    @Override
    public boolean update(String taskNumber, TasksUpdateReq tasksUpdateReq) {
        Tasks oldTasks = findOne(taskNumber);
        if (Objects.isNull(oldTasks)) {
            throw new BusinessException("任务不存在");
        }
        if (StringUtils.isNotBlank(tasksUpdateReq.getName())) {
            oldTasks.setName(tasksUpdateReq.getName());
        }
        if (StringUtils.isNotBlank(tasksUpdateReq.getContent())) {
            oldTasks.setContent(tasksUpdateReq.getContent());
        }
        if (Objects.nonNull(tasksUpdateReq.getUrgent())) {
            oldTasks.setUrgent(tasksUpdateReq.getUrgent());
        }
        if (StringUtils.isNotBlank(tasksUpdateReq.getUrl())) {
            oldTasks.setUrl(tasksUpdateReq.getUrl());
        }
        if (StringUtils.isNotBlank(tasksUpdateReq.getApprovalUrl())) {
            oldTasks.setApprovalUrl(tasksUpdateReq.getApprovalUrl());
        }
        if (Objects.nonNull(tasksUpdateReq.getApprovalType())) {
            oldTasks.setApprovalType(tasksUpdateReq.getApprovalType());
        }
        if (CollectionUtils.isNotEmpty(tasksUpdateReq.getExecutors())) {
            tasksExecutorsService.resetExecutors(oldTasks.getId(), tasksUpdateReq.getExecutors());
        }
        if (Objects.nonNull(tasksUpdateReq.getStartTime())) {
            oldTasks.setStartTime(new Date(tasksUpdateReq.getStartTime()));
        }
        if (Objects.nonNull(tasksUpdateReq.getEndTime())) {
            oldTasks.setEndTime(new Date(tasksUpdateReq.getEndTime()));
        }
        if (StringUtils.isNotBlank(tasksUpdateReq.getExtend())) {
            oldTasks.setExtend(tasksUpdateReq.getExtend());
        }
        if (Objects.nonNull(tasksUpdateReq.getType())) {
            oldTasks.setType(tasksUpdateReq.getType());
        }
        oldTasks.setUpdateTime(new Date());
        updateById(oldTasks);
        sendToCAgent(NotifyTypeEnum.UPDATE.getCode(), taskNumber);

        return true;
    }

    @Override
    public TasksVO save(TasksCreateReq tasksCreateReq) {
        var count = count(tasksCreateReq.getNumber());
        if (count > 0) {
            String msg = String.format("number: %s，已经存在", tasksCreateReq.getNumber());
            throw new BusinessException(msg);
        }
        Date now = new Date();
        Tasks newTasks = new Tasks();
        BeanUtils.copyProperties(tasksCreateReq, newTasks);
        newTasks.setId(idWorker.nextId());
        newTasks.setStartTime(new Date(tasksCreateReq.getStartTime()));
        newTasks.setEndTime(new Date(tasksCreateReq.getEndTime()));
        newTasks.setOperateTime(now);
        newTasks.setUpdateTime(now);
        newTasks.setSystemName(tasksCreateReq.getSystem());

        TasksExecutors creator = tasksCreateReq.getCreator();
        if (Objects.nonNull(creator)) {
            newTasks.setCreatorName(creator.getName());
            newTasks.setCreatorIdCard(creator.getIdCard());
            newTasks.setCreatorDepartment(creator.getDepartment());
            newTasks.setCreatorDepartmentId(creator.getDepartmentId());
            newTasks.setCreatorDepartmentCode(creator.getDepartmentCode());
        }
        if (tasksCreateReq.getUrlOpenType() == null) {
            newTasks.setUrlOpenType(TasksUrlOpenTypeEnum.NORMAL_H5.getCode());
        }
        save(newTasks);
        List<TasksExecutors> executors = tasksCreateReq.getExecutors();
        if (CollectionUtils.isNotEmpty(executors)) {
            tasksExecutorsService.saveExecutors(newTasks.getId(), executors);
        }
        sendToCAgent(NotifyTypeEnum.CREATE.getCode(), newTasks.getNumber());
        return TasksVO.fromTasksView(newTasks);
    }

    @Override
    public void sendToCAgent(String notifyType, String taskNumber) {
        TasksVO detail = detail(taskNumber);

        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage("TASKS_UPDATE").broadcast()
            .body("TASKS_UPDATE", notifyType, detail).build();
        streamBridge.send("cloudcmd-cagent", cagentMqFrame);
    }


    private void sendToCAgentByTasks(String notifyType, TasksVO detail) {
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage("TASKS_UPDATE").broadcast()
                .body("TASKS_UPDATE", notifyType, detail).build();
        streamBridge.send("cloudcmd-cagent", cagentMqFrame);
    }

    @Override
    public Tasks findOne(String taskNumber) {
        // 数据库唯一约束，现在的版本是数据没有按三方应用去区分，全面放开，后续需求完善再确定数据范围，根据应用id和taskNumber做唯一约束
        LambdaQueryWrapper<Tasks> queryWrapper = new LambdaQueryWrapper<Tasks>().eq(Tasks::getNumber, taskNumber);
        return getOne(queryWrapper);
    }

    @Override
    public long count(String taskNumber) {
        LambdaQueryWrapper<Tasks> queryWrapper = new LambdaQueryWrapper<Tasks>().eq(Tasks::getNumber, taskNumber);
        return count(queryWrapper);
    }

    @Override
    public TasksVO detail(String taskNumber) {
        Tasks tasks = findOne(taskNumber);
        if (Objects.isNull(tasks)) {
            throw new BusinessException("任务不存在");
        }
        TasksVO tasksVO = TasksVO.fromTasksView(tasks);
        tasksVO.setExecutors(tasksExecutorsService.findList(tasks.getId()));

        return tasksVO;
    }

    @Override
    public List<TasksVO> details(List<String> taskNumbers) {
        if (CollectionUtils.isEmpty(taskNumbers)) {
            return new ArrayList<>();
        }
        // 数据库唯一约束，现在的版本是数据没有按三方应用去区分，全面放开，后续需求完善再确定数据范围，根据应用id和taskNumber做唯一约束
        LambdaQueryWrapper<Tasks> queryWrapper = new LambdaQueryWrapper<Tasks>().in(Tasks::getNumber, taskNumbers);
        List<Tasks> tasksList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(tasksList)) {
            return new ArrayList<>();
        }
        List<Long> taskIds = tasksList.stream().map(Tasks::getId).collect(Collectors.toList());
        List<TasksExecutors> executorsList = tasksExecutorsService.list(new LambdaQueryWrapper<TasksExecutors>().in(TasksExecutors::getTaskId, taskIds));
        Map<Long, List<TasksExecutors>> executorsGroup = executorsList.stream().collect(Collectors.groupingBy(TasksExecutors::getTaskId));
        List<TasksAttachmentVO> tasksAttachments = tasksAttachmentService.listByTaskNumbers(taskNumbers);
        Map<String, List<TasksAttachmentVO>> attachmentsGroup = tasksAttachments.stream().collect(Collectors.groupingBy(TasksAttachmentVO::getTaskNumber));
        List<TasksVO> tasksVOS = new ArrayList<>();
        for (Tasks tasks : tasksList) {
            TasksVO tasksVO = TasksVO.fromTasksView(tasks);
            tasksVO.setExecutors(executorsGroup.getOrDefault(tasks.getId(), new ArrayList<>()));
            tasksVO.setAttachments(attachmentsGroup.getOrDefault(tasks.getNumber(), new ArrayList<>()));
            tasksVOS.add(tasksVO);
        }
        return tasksVOS;
    }

    @Override
    public List<TasksCountVO> statusCount(Date startTime, Date endTime) {
        List<Tasks> dataList = findPersonalList(startTime, endTime);
        Long totalCount = Long.valueOf(dataList.size());
        Map<String, Long> tasksMap =
            dataList.stream().collect(Collectors.groupingBy(Tasks::getStatus, Collectors.counting()));
        tasksMap.put(ALL, totalCount);
        return STATUS_COUNT_LIST.stream().map(statusName -> {
            TasksCountVO vo = new TasksCountVO();
            vo.setStatus(statusName);
            Long count = tasksMap.get(statusName);
            if (Objects.nonNull(count) && totalCount.longValue() > 0) {
                vo.setCount(count);
                Double ratio = new BigDecimal(count).multiply(new BigDecimal(100))
                    .divide(new BigDecimal(totalCount), RoundingMode.HALF_EVEN).setScale(2, RoundingMode.HALF_EVEN)
                    .doubleValue();
                vo.setRatio(ratio);
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> typeList() {
        UserInfo user = SecurityUtils.getUser();
        String idCard = Objects.nonNull(user) ? user.getIdCardNum() : null;
        if (StringUtils.isBlank(idCard)) {
            log.info("user idCard is null: {}", user);
            return Collections.emptyList();
        }
        return tasksMapper.typeByIdCard(idCard);
    }

    @Override
    public List<TasksTypeCountVO> typeCount(Date startTime, Date endTime) {
        UserInfo user = SecurityUtils.getUser();
        String idCard = Objects.nonNull(user) ? user.getIdCardNum() : null;
        if (StringUtils.isBlank(idCard)) {
            log.info("user idCard is null: {}", user);
            return Collections.emptyList();
        }
        return tasksMapper.countByIdCard(startTime, endTime, idCard, 8);
    }

    @Override
    public List<TasksTypeStatisticsVO> statisticsByBusinessType(Date startTime, Date endTime) {
        List<Tasks> dataList = findPermissionList(startTime, endTime);
        Long totalCount = Long.valueOf(dataList.size());
        Map<String, Long> tasksMap =
            dataList.stream().collect(Collectors.groupingBy(Tasks::getBusinessType, Collectors.counting()));
        List<TasksTypeStatisticsVO> resCount = new ArrayList<>();

        if(CollectionUtils.isNotEmpty(tasksMap)){
            tasksMap.forEach((bussinessType,count)->{
                if (Objects.nonNull(count) && totalCount.longValue() > 0) {
                    TasksTypeStatisticsVO vo = new TasksTypeStatisticsVO();
                    vo.setBusinessType(bussinessType);
                    vo.setCount(count);
                    Double ratio = new BigDecimal(count).multiply(new BigDecimal(100))
                            .divide(new BigDecimal(totalCount), RoundingMode.HALF_EVEN).setScale(2, RoundingMode.HALF_EVEN)
                            .doubleValue();
                    vo.setRatio(ratio);

                    resCount.add(vo);
                }
            });
        }

        return resCount;
    }

    @Override
    public List<TasksStatusStatisticsVO> statisticsStatusByCompleteness(Date startTime, Date endTime) {
        List<Tasks> dataList = findPermissionList(startTime, endTime);
        Long totalCount = Long.valueOf(dataList.size());
        Map<String, Long> tasksMap =
            dataList.stream().collect(Collectors.groupingBy(Tasks::getStatus, Collectors.counting()));

        return Arrays.stream(TasksStatusEnum.values())
            .filter(statusEnum -> !TasksStatusEnum.DISCARDED.getMsg().equals(statusEnum.getMsg())).map(typeEnum -> {
                String key = typeEnum.getMsg();
                TasksStatusStatisticsVO vo = new TasksStatusStatisticsVO();
                vo.setBusinessStatus(key);

                Long count = tasksMap.get(key);
                if (Objects.nonNull(count) && totalCount.longValue() > 0) {
                    Double ratio = new BigDecimal(count).multiply(new BigDecimal(100))
                        .divide(new BigDecimal(totalCount), RoundingMode.HALF_EVEN).setScale(2, RoundingMode.HALF_EVEN)
                        .doubleValue();
                    vo.setRatio(ratio);
                }

                return vo;
            }).collect(Collectors.toList());
    }

    @Override
    public List<TasksDateCountVO> statisticsByDate(Date startTime, Date endTime) {
        List<Tasks> dataList = findPermissionList(startTime, endTime);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        Map<String, Long> dateMap = dataList.stream().collect(
            Collectors.groupingBy(tasks -> DateFormatUtil.format(tasks.getStartTime(), DateFormatUtil.YYYY_MM_DD),
                Collectors.counting()));
        // 保证顺序
        List<String> dateList = dateMap.keySet().stream().sorted().collect(Collectors.toList());
        return dateList.stream().map(date -> {
            TasksDateCountVO vo = new TasksDateCountVO();
            vo.setDate(date);

            Long count = dateMap.get(date);
            if (Objects.nonNull(count)) {
                vo.setCount(count);
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MyTasksCountVO> mineCount(Date startTime, Date endTime) {
        List<MyTasksCountVO> currentPeriodTasksList = statisticsCount(startTime, endTime);
        // 如果没有指定起止时间，则数据不用返回平均任务数和增长率，下降率等信息
        if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
            return currentPeriodTasksList;
        }
        // 需要统计上一个周期的数据，然后比对算出增长率和下降率
        LocalDate start = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        // 计算周期长度
        Period periodLength = Period.between(start, end);
        // 计算上一个周期的起止日期
        LocalDate prevStart = start.minus(periodLength);
        LocalDate prevEnd = end.minus(periodLength);

        Date prevStartTime = Date.from(prevStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date prevEndTime = Date.from(prevEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<MyTasksCountVO> prevPeriodTasksList = statisticsCount(prevStartTime, prevEndTime);
        Map<String, MyTasksCountVO> prevPeriodTasksMap =
            prevPeriodTasksList.stream().collect(Collectors.toMap(MyTasksCountVO::getBusinessType, tasks -> tasks));
        currentPeriodTasksList.stream().forEach(currentTasks -> {
            String key = currentTasks.getBusinessType();
            MyTasksCountVO prevTasks = prevPeriodTasksMap.get(key);
            if (Objects.nonNull(prevTasks)) {
                Long prevAvgCount = prevTasks.getAvgCount();
                if (Objects.nonNull(prevAvgCount) && prevAvgCount.longValue() > 0) {
                    Double ratio = new BigDecimal(currentTasks.getAvgCount()).subtract(new BigDecimal(prevAvgCount))
                        .multiply(new BigDecimal(100)).divide(new BigDecimal(prevAvgCount), RoundingMode.HALF_EVEN)
                        .setScale(2, RoundingMode.HALF_EVEN).doubleValue();
                    currentTasks.setRatio(ratio);
                }
            }
        });

        return currentPeriodTasksList;
    }

    @Override
    public Integer mineToDoCount() {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            return 0;
        }
        return tasksMapper.countMyToDoTask(user.getIdCardNum());
    }

    private List<MyTasksCountVO> statisticsCount(Date startTime, Date endTime) {
        List<Tasks> dataList = findPersonalList(startTime, endTime);
        Map<String, Long> tasksMap =
            dataList.stream().collect(Collectors.groupingBy(Tasks::getBusinessType, Collectors.counting()));

        // 总共要查询的数据有多少天
        long totalDays =
            Objects.nonNull(startTime) && Objects.nonNull(endTime) ? Duration.between(startTime.toInstant(),
                endTime.toInstant()).toDays() : 0L;
        List<MyTasksCountVO> resCount = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(tasksMap)){
            tasksMap.forEach( (businessType,matchedCount) -> {
                if (Objects.nonNull(matchedCount) && 0L != matchedCount) {
                    MyTasksCountVO vo = new MyTasksCountVO();
                    vo.setBusinessType(businessType);
                    vo.setCount(matchedCount);
                    // 没有起止时间则不返回平均任务数
                    if (totalDays > 0) {
                        vo.setAvgCount(matchedCount / totalDays);
                    }
                     resCount.add(vo);
                }
            });
        }
        return resCount;
    }

    @Override
    public List<TasksTypeStatisticsVO> statisticsByCompleteness(Date startTime, Date endTime) {
        List<Tasks> dataList = findPersonalList(startTime, endTime);
        Map<String, List<Tasks>> tasksMap = dataList.stream().collect(Collectors.groupingBy(Tasks::getBusinessType));
        List<TasksTypeStatisticsVO> resCount = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(tasksMap)){
            tasksMap.forEach( (businessType,matchedList) -> {
                if (CollectionUtils.isNotEmpty(matchedList)) {
                    TasksTypeStatisticsVO vo = new TasksTypeStatisticsVO();
                    vo.setBusinessType(businessType);
                    Long doneCount = matchedList.stream().filter(
                        tasks -> StringUtils.isNotBlank(tasks.getStatus()) && TasksStatusEnum.COMPLETED.getMsg()
                            .equals(tasks.getStatus())).count();
                    Double ratio = new BigDecimal(doneCount).multiply(new BigDecimal(100))
                        .divide(new BigDecimal(matchedList.size()), RoundingMode.HALF_EVEN)
                        .setScale(2, RoundingMode.HALF_EVEN).doubleValue();
                    vo.setRatio(ratio);
                    resCount.add(vo);
                }
            });
        }
        return resCount;
    }

    @Override
    public List<TasksTypeStatisticsVO> avgDuration(Date startTime, Date endTime) {
        List<Tasks> dataList = findPersonalList(startTime, endTime);
        Map<String, List<Tasks>> tasksMap = dataList.stream().collect(Collectors.groupingBy(Tasks::getBusinessType));

        List<TasksTypeStatisticsVO> resCount = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(tasksMap)){
             tasksMap.forEach( (businessType,matchedList) -> {
                 if (CollectionUtils.isNotEmpty(matchedList)) {
                     List<Tasks> doneList = matchedList.stream().filter(
                                     tasks -> StringUtils.isNotBlank(tasks.getStatus()) && TasksStatusEnum.COMPLETED.getMsg()
                                             .equals(tasks.getStatus()) && Objects.nonNull(tasks.getCompleteTime()))
                             .collect(Collectors.toList());

                     if (CollectionUtils.isNotEmpty(doneList)) {
                         TasksTypeStatisticsVO vo = new TasksTypeStatisticsVO();
                         vo.setBusinessType(businessType);
                         long totalHours = doneList.stream().mapToLong(tasks -> {
                             Instant startInstant = tasks.getOperateTime().toInstant();
                             Instant endInstant = tasks.getCompleteTime().toInstant();
                             return Duration.between(startInstant, endInstant).toHours();
                         }).sum();

                         Double ratio =
                                 new BigDecimal(totalHours).divide(new BigDecimal(doneList.size()), RoundingMode.HALF_EVEN)
                                         .setScale(2, RoundingMode.HALF_EVEN).doubleValue();
                         vo.setRatio(ratio);
                         resCount.add(vo);
                     }else{
                         TasksTypeStatisticsVO vo = new TasksTypeStatisticsVO();
                         vo.setBusinessType(businessType);
                         resCount.add(vo);
                     }
                 }
             });
        }

        return resCount;
    }

    @Override
    public Page<TasksGroupVO> groupPage(Page<TasksGroupVO> page, TasksGroupQO tasksGroupQO) {
        var wrapper = new MPJLambdaWrapper<>(Tasks.class)//
            .selectAll(Tasks.class).distinct();
        if (tasksGroupQO.getGroupId() != null) {
            wrapper = wrapper.selectAs("IF(ttg.id is null, 0, 1)", TasksGroupVO::getBindFlag)//
                .leftJoin(TrTasksGroup.class, "ttg", //
                    on -> on//
                        .eq(TrTasksGroup::getTaskId, Tasks::getId)//
                        .eq(TrTasksGroup::getGroupId, tasksGroupQO.getGroupId()))//
                //                    .isNull(tasksGroupQO.getBindFlag() != null && tasksGroupQO.getBindFlag() == 0, TrTasksGroup::getId)//
                .isNotNull(tasksGroupQO.getBindFlag() != null && tasksGroupQO.getBindFlag() == 1, TrTasksGroup::getId)
                .orderByDesc("bindFlag").orderByDesc(Tasks::getOperateTime);
        } else {
            wrapper = wrapper.selectAs("0", TasksGroupVO::getBindFlag).orderByDesc(Tasks::getOperateTime);
        }
        List<String> orgCodeList = getPermissionOrgCodeList();
        UserInfo user = SecurityUtils.getUser();
        String idCard = Objects.nonNull(user) ? user.getIdCardNum() : null;
        wrapper = wrapper.gt(tasksGroupQO.getStartTime() != null, Tasks::getStartTime, tasksGroupQO.getStartTime())
                .lt(tasksGroupQO.getEndTime() != null, Tasks::getStartTime, tasksGroupQO.getEndTime())
            // if (配置了数据权限) { return 创建人部门或执行人部门包含在内的数据;}
            .and(CollectionUtils.isNotEmpty(orgCodeList), c -> c.in(Tasks::getCreatorDepartmentCode, orgCodeList).or()
                .exists(TasksExecutors.class, w -> w.select("1").eq(Tasks::getId, TasksExecutors::getTaskId)
                    .in(TasksExecutors::getDepartmentCode, orgCodeList)))
            // if (没有配置数据权限&&身份证号不为空) { return 个人的数据;}
            .exists(CollectionUtils.isEmpty(orgCodeList) && StringUtils.isNotBlank(idCard), TasksExecutors.class,
                w -> w.select("1").eq(Tasks::getId, TasksExecutors::getTaskId).eq(TasksExecutors::getIdCard, idCard))
            .and(StringUtils.isNotBlank(tasksGroupQO.getKeywords()),
                w -> w.like(Tasks::getName, tasksGroupQO.getKeywords()).or()
                    .like(Tasks::getContent, tasksGroupQO.getKeywords()).or()
                    .like(Tasks::getSystemName, tasksGroupQO.getKeywords()).or()
                    .like(Tasks::getCreatorName, tasksGroupQO.getKeywords()).or()
                    .like(Tasks::getCreatorDepartment, tasksGroupQO.getKeywords()));

        return tasksMapper.selectJoinPage(page, TasksGroupVO.class, wrapper);
    }

    @Override
    public boolean bindGroup(Long groupId, List<Long> tasksIds) {
        deleteBinding(groupId, null);
        if (tasksIds.isEmpty()) {
            return false;
        }
        var collect = tasksIds.stream().map(tid -> new TrTasksGroup(idWorker.nextId(), tid, groupId))
            .collect(Collectors.toList());
        tasksGroupMapper.insertBatch(collect);

        return true;
    }

    @Override
    public boolean deleteBinding(Long groupId, Long taskId) {
        tasksGroupMapper.delete(Wrappers.lambdaQuery(TrTasksGroup.class).eq(TrTasksGroup::getGroupId, groupId)
            .eq(taskId != null, TrTasksGroup::getTaskId, taskId));
        return true;
    }

    @Override
    public Map<Long, Integer> countBinding(List<Long> groupIdList) {
        var cnts = tasksGroupMapper.selectJoinList(TasksGroupCntVO.class,
            MPJWrappers.lambdaJoin(TrTasksGroup.class).select(TrTasksGroup::getGroupId)
                .selectAs("count(1)", TasksGroupCntVO::getCnt).in(TrTasksGroup::getGroupId, groupIdList)
                .groupBy(TrTasksGroup::getGroupId));
        if (cnts == null || cnts.isEmpty()) {
            return Collections.emptyMap();
        }
        return cnts.stream()
            .collect(Collectors.toMap(TasksGroupCntVO::getGroupId, TasksGroupCntVO::getCnt, (a, b) -> a));
    }

    /**
     * 仅查个人的任务
     *
     * @param startTime
     * @param endTime
     * @return
     */
    private List<Tasks> findPersonalList(Date startTime, Date endTime) {
        UserInfo user = SecurityUtils.getUser();
        String idCard = Objects.nonNull(user) ? user.getIdCardNum() : null;
        if (StringUtils.isBlank(idCard)) {
            log.info("user idCard is null: {}", user);
            return Collections.emptyList();
        }
        return tasksMapper.findByIdCard(startTime, endTime, idCard);
    }

    private List<Tasks> findPersonalToDoList(Date startTime, Date endTime) {
        UserInfo user = SecurityUtils.getUser();
        String idCard = Objects.nonNull(user) ? user.getIdCardNum() : null;
        if (StringUtils.isBlank(idCard)) {
            log.info("user idCard is null: {}", user);
            return Collections.emptyList();
        }
        return tasksMapper.findToDoList(startTime, endTime, idCard);
    }

    /**
     * 查询数据权限可见的任务
     *
     * @param startTime
     * @param endTime
     * @return 配置了数据权限则按数据权限返回，否则返回个人的任务
     */
    private List<Tasks> findPermissionList(Date startTime, Date endTime) {
        List<String> orgCodeList = getPermissionOrgCodeList();
        if (CollectionUtils.isNotEmpty(orgCodeList)) {
            return tasksMapper.findByOrgCodeList(startTime, endTime, orgCodeList);
        }
        return findPersonalList(startTime, endTime);
    }

    private List<String> getPermissionOrgCodeList() {
        UserInfo user = SecurityUtils.getUser();
        List<String> orgCodeList = new ArrayList<>();
        if (user != null) {
            orgCodeList = user.getImOrgPrivCodes();
        }
        return orgCodeList;
    }

}