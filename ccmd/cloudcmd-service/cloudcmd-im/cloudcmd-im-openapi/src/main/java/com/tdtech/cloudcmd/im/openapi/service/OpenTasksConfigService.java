package com.tdtech.cloudcmd.im.openapi.service;

import com.tdtech.cloudcmd.im.openapi.controller.entity.OpenTasksConfigVO;

/**
 * 任务标准件配置 Service
 */
public interface OpenTasksConfigService {

    /**
     * 设置任务标准件配置（upsert）
     *
     * @param vo 配置请求
     * @return 设置成功返回 true
     */
    boolean setConfig(OpenTasksConfigVO vo);

    /**
     * 确保当前调用方有该 module 的配置记录
     * 规则：
     * - module 未登记：自动插入一条 show_in_pc=0 的记录，归属当前 systemCode
     * - module 已登记且属于当前 systemCode：允许
     * - module 已登记但属于其他 systemCode：拒绝
     *
     * @param module 任务标准件模块名称
     */
    void ensureModule(String module);
}