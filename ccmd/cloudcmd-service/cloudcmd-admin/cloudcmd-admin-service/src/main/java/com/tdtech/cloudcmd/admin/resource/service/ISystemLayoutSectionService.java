package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import com.tdtech.cloudcmd.admin.resource.entity.SystemLayoutSection;
import com.tdtech.cloudcmd.bean.PageResult;

/**
 * 系统布局板块 Service 接口
 *
 * @author: S063874
 * @date: 2026-03-10 14:08
 */
public interface ISystemLayoutSectionService {

    /**
     * 创建系统布局板块
     *
     * @param systemLayoutSection 系统布局板块信息
     * @return 编号
     */
    Boolean createSystemLayoutSection(SystemLayoutSection systemLayoutSection);

    /**
     * 更新系统布局板块
     *
     * @param systemLayoutSection 系统布局板块信息
     */
    Boolean updateSystemLayoutSection(SystemLayoutSection systemLayoutSection);

    /**
     * 删除系统布局板块
     *
     * @param id 编号
     */
    void deleteSystemLayoutSection(Long id);

    /**
     * 批量删除系统布局板块
     *
     * @param ids 编号列表
     */
    void deleteSystemLayoutSectionByIds(List<Long> ids);

    /**
     * 根据ID获取系统布局板块
     *
     * @param id 编号
     * @return 系统布局板块
     */
    SystemLayoutSection getSystemLayoutSectionById(Long id);

    /**
     * 获取所有系统布局板块
     *
     * @return 系统布局板块列表
     */
    List<SystemLayoutSection> listSystemLayoutSection();

    /**
     * 获取展示的系统布局板块列表
     *
     * @return 系统布局板块列表
     */
    List<SystemLayoutSection> listShowSystemLayoutSection();

    /**
     * 根据类型获取系统布局板块列表
     *
     * @param type 板块类型
     * @return 系统布局板块列表
     */
    List<SystemLayoutSection> listByType(Integer type);

    boolean existSectionsByType(Integer type, Long id);
}