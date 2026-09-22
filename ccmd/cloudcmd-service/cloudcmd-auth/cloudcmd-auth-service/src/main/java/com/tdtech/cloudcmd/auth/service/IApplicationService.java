package com.tdtech.cloudcmd.auth.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.auth.entity.Application;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用) 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
public interface IApplicationService extends IService<Application> {

    /**
     * 根据appKey查询应用
     * 
     * @param appKey
     * @return
     */
    Application getApplicationById(String appKey);

    /**
     * 得到所有的应用
     * 
     * @return
     */
    List<Application> getApplicationList();
}
