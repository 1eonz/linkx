package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksExpired;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksExpiredVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExpiredService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksExpiredMapper;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TasksExpiredServiceImpl extends ServiceImpl<TasksExpiredMapper, TasksExpired> implements ITasksExpiredService {

    @Resource
    private TasksExpiredMapper tasksExpiredMapper;

    @Resource
    private ImService imService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Override
    public Page<TasksExpiredVO> findPage(int pageNum, int pageSize, String keywords, String deptCode, String startTime, String endTime) {
        List<String> orgCodeList = new ArrayList<>();
        if (StringUtils.isNotBlank(deptCode)) {
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(deptCode);
            orgCodeList = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        }

        Page<TasksExpiredVO> page = new Page<>(pageNum, pageSize);
        Page<TasksExpiredVO> resultPage = tasksExpiredMapper.findPage(page, keywords, orgCodeList, startTime, endTime);
        for (TasksExpiredVO vo : resultPage.getRecords()) {
            String relatedUserIds = vo.getRelatedUserIds();
            String userName = vo.getPostUserNames();
            String[] split = relatedUserIds.split(",");
            String[] names = userName.split(",");
            for (int i = 0; i < split.length; i++){
                if(split[i].equals(vo.getUserId()+"")){
                    vo.setPostUserNames(names[i]);
                    break;
                }
            }
            if (Boolean.FALSE.equals(vo.getIsHandle())) {
                vo.setHandleTime(null);
            }
        }
        return resultPage;
    }
    private String removeSpacesInBrackets(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll(" ", "");
    }

    @Override
    public Long countByCondition(List<Long> orgIdList, String startTime, String endTime) {
        return tasksExpiredMapper.countByCondition(orgIdList, startTime, endTime);
    }
}