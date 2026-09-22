package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import com.tdtech.cloudcmd.admin.resource.entity.SystemConfig;

/**
 * 系统配置 Service 接口
 *
 * @author: S063874
 * @date: 2026-03-10 14:05
 */
public interface ISystemConfigService {

    /**
     * 创建系统配置
     *
     * @param systemConfig 系统配置信息
     * @return 编号
     */
    Long createSystemConfig(SystemConfig systemConfig);

    /**
     * 更新系统配置
     *
     * @param systemConfig 系统配置信息
     */
    void updateSystemConfig(SystemConfig systemConfig);

    /**
     * 删除系统配置
     *
     * @param id 编号
     */
    void deleteSystemConfig(Long id);

    /**
     * 批量删除系统配置
     *
     * @param ids 编号列表
     */
    void deleteSystemConfigByIds(List<Long> ids);

    /**
     * 根据ID获取系统配置
     *
     * @param id 编号
     * @return 系统配置
     */
    SystemConfig getSystemConfigById(Long id);

    /**
     * 根据key获取系统配置
     *
     * @param key 配置名称
     * @return 系统配置
     */
    SystemConfig getSystemConfigByKey(String key);

    /**
     * 获取所有系统配置
     *
     * @return 系统配置列表
     */
    List<SystemConfig> listSystemConfig();

    /**
     * 根据key获取配置值
     *
     * @param key 配置名称
     * @return 配置值
     */
    String getConfigValue(String key);
}
