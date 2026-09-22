package com.tdtech.cloudcmd.linkx.dashboard.service;

/**
 * 统计明细同步服务。
 * <p>
 * 供 5 个定时任务调用，按双游标增量拉取源数据，聚合后 upsert 写入对应统计明细表。
 */
public interface IStaticSyncService {

    /**
     * 同步 tb_static_task（协同任务统计明细）。
     */
    void syncStaticTask();

    /**
     * 同步 tb_static_create_group（建群记录统计明细）。
     */
    void syncStaticCreateGroup();

    /**
     * 同步 tb_static_task_response（任务回复统计明细）。
     */
    void syncStaticTaskResponse();

    /**
     * 同步 tb_static_photo_check（人员核查统计明细，dashboard 侧跨体系聚合）。
     */
    void syncStaticPhotoCheck();

    /**
     * 同步 tb_static_coop_duty_switch（协同岗上下岗统计明细）。
     */
    void syncStaticCoopDutySwitch();
}