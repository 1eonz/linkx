package com.tdtech.cloudcmd.admin.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.resource.entity.Application;
import com.tdtech.cloudcmd.admin.resource.mapper.ApplicationMapper;
import com.tdtech.cloudcmd.admin.resource.service.IApplicationService;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.tdtech.cloudcmd.admin.exception.AdminErrorEnum.COMMON_ERROR_513;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用) 服务实现类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-28
 */
@Service
@Slf4j
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements IApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private IdWorker idWorker;

    @Override
    public List<Application> getApplicationList() {
        List<Application> applications = applicationMapper.selectList(null);
        for (Application application : applications) {
            application.setRemark(I18nUtil.get(application.getRemark()));
        }
        return applications;
    }

    @Override
    public void createApplication(Application application) {
        var count = applicationMapper.selectCount(
            new QueryWrapper<Application>().eq(Application.APP_KEY, application.getAppKey()));
        if (count > 0) {
            throw new AdminException(COMMON_ERROR_513.getCode(), I18nUtil.get(COMMON_ERROR_513.getMsg()));
        }
        application.setId(idWorker.nextId());
        applicationMapper.insert(application);
    }

    @Override
    public void updateApplication(Application application) {
        Application existApplication = applicationMapper.selectById(application.getId());
        if (!existApplication.getAppKey().equals(application.getAppKey())) {
            var count = applicationMapper.selectCount(
                new QueryWrapper<Application>().eq(Application.APP_KEY, application.getAppKey()));
            if (count > 0) {
                throw new AdminException(COMMON_ERROR_513.getCode(), I18nUtil.get(COMMON_ERROR_513.getMsg()));
            }
        }
        applicationMapper.updateById(application);
    }

    @Override
    public void deleteApplication(List<Long> ids) {
        applicationMapper.batchLogicDeleteApplication(ids);
    }

    @Override
    public Application getApplicationById(Long id) {
        return applicationMapper.selectById(id);
    }

    @Override
    public Application getApplicationByAppKey(String appKey) {
        QueryWrapper<Application> wrapper = new QueryWrapper<Application>().eq(Application.APP_KEY, appKey);
        return applicationMapper.selectOne(wrapper);
    }

    @Override
    public void batchInsertApplication(List<Application> applicationList) {
        applicationMapper.batchInsertApplication(applicationList);
    }

    @Override
    public void batchDeleteApplicationByIds(List<Long> applicationIds) {
        applicationMapper.deleteBatchIds(applicationIds);
    }
}
