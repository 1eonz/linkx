package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.Tasks;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksAttachmentVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksExecutors;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksConfig;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksConfigVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksAttachmentService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExecutorsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.TasksConfigService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksConfigMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务标准件配置实现（PC 端展示）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TasksConfigServiceImpl implements TasksConfigService {

    private final TasksConfigMapper tasksConfigMapper;
    private final TasksMapper tasksMapper;
    private final ITasksExecutorsService tasksExecutorsService;
    private final ITasksAttachmentService tasksAttachmentService;

    @Override
    public List<TasksConfigVO> listPcModules() {
        List<TasksConfig> list = tasksConfigMapper.selectList(
                Wrappers.lambdaQuery(TasksConfig.class)
                        .eq(TasksConfig::getShowInPc, 1)
                        .orderByAsc(TasksConfig::getGmtCreated));
        return list.stream().map(c -> {
            TasksConfigVO vo = new TasksConfigVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<TasksVO> pageTasksByModule(String module, int pageNum, int pageSize,
                                           String name, String content,
                                           Integer scope, String idCard) {
        if (StringUtils.isBlank(module)) {
            throw new BusinessException("module不能为空");
        }
        int scopeValue = (scope == null) ? 1 : scope;
        Page<Tasks> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Tasks> wrapper = Wrappers.lambdaQuery(Tasks.class)
                .eq(Tasks::getModule, module)
                .like(StringUtils.isNotBlank(name), Tasks::getName, name)
                .like(StringUtils.isNotBlank(content), Tasks::getContent, content);
        // 范围过滤：1全部、2创建人、3执行人、4创建人+执行人
        // 执行人过滤采用先查执行人 taskId 集合再 in 过滤，避免 inSql 拼接 SQL 注入风险
        List<Long> executorTaskIds;
        if (scopeValue == 3 || scopeValue == 4) {
            List<TasksExecutors> executorList = tasksExecutorsService.list(
                    new LambdaQueryWrapper<TasksExecutors>().eq(TasksExecutors::getIdCard, idCard));
            executorTaskIds = executorList.stream()
                    .map(TasksExecutors::getTaskId)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            executorTaskIds = null;
        }
        switch (scopeValue) {
            case 2:
                wrapper.eq(Tasks::getCreatorIdCard, idCard);
                break;
            case 3:
                if (executorTaskIds.isEmpty()) {
                    Page<TasksVO> emptyPage = new Page<>();
                    emptyPage.setTotal(0L);
                    emptyPage.setPages(0L);
                    emptyPage.setCurrent(pageNum);
                    emptyPage.setSize(pageSize);
                    emptyPage.setRecords(new ArrayList<>());
                    return emptyPage;
                }
                wrapper.in(Tasks::getId, executorTaskIds);
                break;
            case 4:
                if (executorTaskIds.isEmpty()) {
                    wrapper.eq(Tasks::getCreatorIdCard, idCard);
                } else {
                    wrapper.and(w -> w.eq(Tasks::getCreatorIdCard, idCard)
                            .or().in(Tasks::getId, executorTaskIds));
                }
                break;
            case 1:
            default:
                // 全部：不加范围过滤
                break;
        }
        wrapper.orderByDesc(Tasks::getUpdateTime);
        Page<Tasks> dataPage = tasksMapper.selectPage(page, wrapper);
        List<Tasks> records = dataPage.getRecords();
        if (records.isEmpty()) {
            Page<TasksVO> emptyPage = new Page<>();
            emptyPage.setTotal(0L);
            emptyPage.setPages(0L);
            emptyPage.setCurrent(pageNum);
            emptyPage.setSize(pageSize);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        // 批量查处理人
        List<Long> taskIds = records.stream().map(Tasks::getId).collect(Collectors.toList());
        List<TasksExecutors> executorsList = tasksExecutorsService.list(
                new LambdaQueryWrapper<TasksExecutors>().in(TasksExecutors::getTaskId, taskIds));
        Map<Long, List<TasksExecutors>> executorsGroup =
                executorsList.stream().collect(Collectors.groupingBy(TasksExecutors::getTaskId));
        // 批量查附件
        List<String> taskNumbers = records.stream().map(Tasks::getNumber).collect(Collectors.toList());
        List<TasksAttachmentVO> tasksAttachments =
                tasksAttachmentService.listByTaskNumbers(taskNumbers);
        Map<String, List<TasksAttachmentVO>> attachmentsGroup =
                tasksAttachments.stream().collect(Collectors.groupingBy(
                        TasksAttachmentVO::getTaskNumber));
        List<TasksVO> dataList = records.stream().map(task -> {
            TasksVO vo = TasksVO.fromTasksView(task);
            vo.setExecutors(executorsGroup.getOrDefault(task.getId(), new ArrayList<>()));
            vo.setAttachments(attachmentsGroup.getOrDefault(task.getNumber(), new ArrayList<>()));
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
}