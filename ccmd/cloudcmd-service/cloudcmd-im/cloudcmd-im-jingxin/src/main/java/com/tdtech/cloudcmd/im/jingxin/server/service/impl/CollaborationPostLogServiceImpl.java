package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPostLog;
import com.tdtech.cloudcmd.im.jingxin.server.enums.Constant;
import com.tdtech.cloudcmd.im.jingxin.server.enums.LogTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.excel.CollaborationPostLogExcel;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostLogService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostLogMapper;
import com.tdtech.cloudcmd.im.jingxin.server.util.DataPermissionUtil;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Slf4j
@Service
public class CollaborationPostLogServiceImpl extends ServiceImpl<CollaborationPostLogMapper, CollaborationPostLog> implements CollaborationPostLogService {

    @Resource
    private CollaborationPostLogMapper collaborationPostLogMapper;

    @Resource
    private IdWorker idWorker;

    @Resource
    DataPermissionUtil dataPermissionUtil;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private IOrganizationService organizationService;

    @Override
    public Page<CollaborationPostLog> page(int pageNum, int pageSize, String postName, Long orgId, String orgName, String relatedUserNames, String startTime, String endTime) {
        LambdaQueryWrapper<CollaborationPostLog> queryWrapper = buildCommonQueryWrapper(postName, orgId, orgName, relatedUserNames, startTime, endTime);
        Page<CollaborationPostLog> collaborationPostLogPage = collaborationPostLogMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        collaborationPostLogPage.getRecords().forEach(log -> {
                    var org = organizationService.findOneById(log.getOrgId());
                    if (org != null) {
                        log.setOrgName(org.getName());
                    }
                }
        );

        return collaborationPostLogPage;
    }

    private LambdaQueryWrapper<CollaborationPostLog> buildCommonQueryWrapper(String postName, Long orgId, String orgName, String relatedUserNames, String startTime, String endTime) {
        LambdaQueryWrapper<CollaborationPostLog> queryWrapper = new LambdaQueryWrapper<CollaborationPostLog>()
                .like(StringUtils.isNotBlank(postName), CollaborationPostLog::getPostName, postName)
                .like(StringUtils.isNotBlank(orgName), CollaborationPostLog::getOrgName, orgName)
                .like(StringUtils.isNotBlank(relatedUserNames), CollaborationPostLog::getRelatedUserNames, relatedUserNames)
                .orderByDesc(CollaborationPostLog::getOperateTime);
        List<Long> orgIds =
                organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                        .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(orgIds)) {
            queryWrapper.in(CollaborationPostLog::getOrgId, orgIds);
        }
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge(CollaborationPostLog::getOperateTime, DateFormatUtil.parseDate(startTime + " 00:00:00", DateFormatUtil.YYYY_MM_DD_HH_MM_SS));
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le(CollaborationPostLog::getOperateTime, DateFormatUtil.parseDate(endTime + " 23:59:59", DateFormatUtil.YYYY_MM_DD_HH_MM_SS));
        }
        return queryWrapper;
    }

    @Override
    public void exportToExcel(String fileName, String postName, Long orgId, String orgName, String relatedUserNames, String startTime, String endTime, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<CollaborationPostLog> queryWrapper = buildCommonQueryWrapper(postName, orgId, orgName, relatedUserNames, startTime, endTime);
        List<CollaborationPostLog> dbList = collaborationPostLogMapper.selectList(queryWrapper);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition",
                "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
        List<CollaborationPostLogExcel> dataList = dbList.stream().map(log -> {
            CollaborationPostLogExcel excel = new CollaborationPostLogExcel();
            BeanUtils.copyProperties(log, excel);
            Integer operationType = excel.getOperationType();
            if (Objects.nonNull(operationType)) {
                LogTypeEnum logTypeEnum = LogTypeEnum.matchCode(operationType);
                if (Objects.nonNull(logTypeEnum)) {
                    excel.setOperationTypeName(logTypeEnum.getMsg());
                }
            }
            return excel;
        }).collect(Collectors.toList());

        EasyExcel.write(response.getOutputStream(), CollaborationPostLogExcel.class).sheet("协同岗操作记录表").doWrite(dataList);
    }

    @Override
    public void saveLog(CollaborationPost collaborationPost, Integer operationType, String content) {
        UserInfo user = SecurityUtils.getUser();
        saveLog(collaborationPost, user, operationType, content);
    }

    @Override
    public void saveLog(CollaborationPost collaborationPost, UserInfo user, Integer operationType, String content) {
        CollaborationPostLog log = new CollaborationPostLog();
        log.setId(idWorker.nextId());
        log.setPostName(collaborationPost.getPostName());
        log.setOrgId(collaborationPost.getOrgId());
        log.setOrgCode(collaborationPost.getOrgCode());
        log.setOrgName(collaborationPost.getOrgName());
        log.setRelatedUserIds(collaborationPost.getRelatedUserIds());
        log.setRelatedUserNames(collaborationPost.getRelatedUserNames());
        log.setOperatorId(user.getUserId());
        log.setOperatorName(user.getUserName());
        log.setOperationType(operationType);
        log.setContent(content);
        log.setOperateTime(new Date());
        save(log);
    }


    @Scheduled(cron = "0 0 0 * * ?")   // 每天 00:00:00 执行
    public void deletePostLogHistoryTask() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, - Constant.SAVE_YEARS);
        Date threeYearsAgo = cal.getTime();
        LambdaQueryWrapper<CollaborationPostLog> lambdaQueryWrapper = new LambdaQueryWrapper<CollaborationPostLog>()
                .lt(CollaborationPostLog::getOperateTime, threeYearsAgo);
        remove(lambdaQueryWrapper);
    }
}