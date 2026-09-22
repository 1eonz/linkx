package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.admin.resource.entity.Application;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用) 服务类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-28
 */
public interface IApplicationService extends IService<Application> {
    /**
     * 查询所有应用
     * 
     * @return
     */
    List<Application> getApplicationList();

    /**
     * 创建应用
     * 
     * @param application
     */
    void createApplication(Application application);

    /**
     * 修改应用信息
     * 
     * @param application
     */
    void updateApplication(Application application);

    /**
     * 删除应用
     * 
     * @param ids
     */
    void deleteApplication(List<Long> ids);

    /**
     * 根据应用id获取应用
     * 
     * @param id
     * @return
     */
    Application getApplicationById(Long id);

    /**
     * 根据应用appKey获取应用
     * 
     * @param appKey
     * @return
     */
    Application getApplicationByAppKey(String appKey);

    /**
     * 批量插入应用
     * 
     * @param applicationList
     */
    void batchInsertApplication(List<Application> applicationList);

    /**
     * 根据组织id列表批量删除应用
     *
     * @param applicationIds
     */
    void batchDeleteApplicationByIds(List<Long> applicationIds);
}
