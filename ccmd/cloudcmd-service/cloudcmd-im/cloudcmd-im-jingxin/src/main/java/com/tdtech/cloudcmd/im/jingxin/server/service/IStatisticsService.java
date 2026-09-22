package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCoopDutySwitchDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCreateGroupDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCursorResult;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticTaskDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticTaskResponseDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统计明细增量拉取聚合服务。
 * <p>
 * 从源表按双游标 (时间字段, 源表唯一键) 增量拉取，聚合关联表后返回完整 DTO，
 * 供 linkx-dashboard 侧去重写入统计明细表。
 * <p>
 * 双游标条件：{@code (time, id) > (timeAfter, idAfter)}，
 * 即 {@code time > timeAfter OR (time = timeAfter AND id > idAfter)}，保证不漏不重。
 * 首次拉取 timeAfter/idAfter 传 null。
 */
public interface IStatisticsService {

    /**
     * 增量拉取协同任务统计明细（tb_static_task）。
     * 游标：(gmt_modified, id)，源表 tb_task。
     */
    StaticCursorResult<StaticTaskDTO> listStaticTaskByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 增量拉取建群记录统计明细（tb_static_create_group）。
     * 游标：(update_time, group_id)，源表 tb_create_group。
     */
    StaticCursorResult<StaticCreateGroupDTO> listStaticCreateGroupByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 增量拉取任务回复统计明细（tb_static_task_response）。
     * 游标：(gmt_created, id)，源表 tb_task_response。
     */
    StaticCursorResult<StaticTaskResponseDTO> listStaticTaskResponseByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 增量拉取协同岗上下岗统计明细（tb_static_coop_duty_switch）。
     * 游标：(create_time, id)，源表 tb_collaboration_attendance。
     */
    StaticCursorResult<StaticCoopDutySwitchDTO> listStaticCoopDutySwitchByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 查询指定类型的协同岗列表（供 tb_static_photo_check 在 dashboard 侧聚合时使用）。
     * 协同岗数量 ≤300，全量拉到内存匹配。
     *
     * @param type 协同岗类型（1 表示 1:14E 协同岗）
     * @return 协同岗列表
     */
    List<CollaborationPostVO> listCoopPostsByType(Integer type);

    /**
     * 按身份证号批量查警信 userId（供 tb_static_photo_check 使用）。
     *
     * @param idCards 身份证号列表
     * @return idCard→userId 映射（查不到的不包含）
     */
    Map<String, Long> findUserIdsByIdCards(List<String> idCards);
}