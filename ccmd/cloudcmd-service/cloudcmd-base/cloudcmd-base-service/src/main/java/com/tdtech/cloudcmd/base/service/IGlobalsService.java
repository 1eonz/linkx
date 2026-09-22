package com.tdtech.cloudcmd.base.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.base.entity.Globals;
import com.tdtech.cloudcmd.bean.UserInfo;

/**
 * <p>
 * 全局变量信息表 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
public interface IGlobalsService extends IService<Globals> {

    /**
     * 根据名称查询值
     * 
     * @param name
     * @return
     */
    String getValueByName(String name);

    /**
     * 获取所有全局配置值
     * 
     * @return
     */
    List<Globals> getGlobalsList();

    List<Globals> getTenantGlobalsList(UserInfo user);
}
