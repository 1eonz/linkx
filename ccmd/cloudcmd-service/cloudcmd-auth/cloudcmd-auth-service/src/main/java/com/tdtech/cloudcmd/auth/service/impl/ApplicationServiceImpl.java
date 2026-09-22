package com.tdtech.cloudcmd.auth.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.entity.Application;
import com.tdtech.cloudcmd.auth.mapper.ApplicationMapper;
import com.tdtech.cloudcmd.auth.service.IApplicationService;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用) 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Service
@Slf4j
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements IApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;

    @Override
    public Application getApplicationById(String appKey) {
        QueryWrapper<Application> wrapper = new QueryWrapper<Application>().eq(Application.APP_KEY, appKey);
        return applicationMapper.selectOne(wrapper);
    }

    @Override
    public List<Application> getApplicationList() {
        return applicationMapper.selectList(null);
    }
}
