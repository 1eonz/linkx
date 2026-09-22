package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CooperationUnattended;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CooperationUnattendedVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICooperationUnattendedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.SendPreWarningMessageService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CooperationUnattendedMapper;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CooperationUnattendedServiceImpl extends ServiceImpl<CooperationUnattendedMapper, CooperationUnattended> implements ICooperationUnattendedService {

    @Resource
    private CooperationUnattendedMapper cooperationUnattendedMapper;


    @Resource
    private SendPreWarningMessageService sendPreWarningMessageService;

    @Resource
    private ImService imService;

    @Resource
    private IdWorker idWorker;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Override
    public Page<CooperationUnattendedVO> findPage(int pageNum, int pageSize, String keywords, String deptCode, String startTime, String endTime) {
        List<String> orgCodeList = new ArrayList<>();
        var imStartMills = System.currentTimeMillis();
        if (StringUtils.isNotBlank(deptCode)) {
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(deptCode);
            log.info("query from im used mills: {}", System.currentTimeMillis() - imStartMills);
            orgCodeList = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        }
        Page<CooperationUnattendedVO> page = new Page<>(pageNum, pageSize);
        var dbStartMills = System.currentTimeMillis();
        Page<CooperationUnattendedVO> result = cooperationUnattendedMapper.findPage(page, keywords, orgCodeList, startTime, endTime);
        log.info("query from db used mills: {}", System.currentTimeMillis() - dbStartMills);
        return result;
    }

    @Override
    public boolean saveUnattendedRecord(Long postId) {
        CooperationUnattended record = new CooperationUnattended();
        record.setId(idWorker.nextId());
        record.setPostId(postId);
        record.setCreateTime(new Date());

        // 推送无人值守消息
        boolean save = save(record);
        sendPreWarningMessageService.sendUnattendedMessage(postId);
        return save;
    }
}