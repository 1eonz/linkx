package com.tdtech.cloudcmd.im.jingxin.api;

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
 * 统计明细增量拉取 RPC 接口（Dubbo）。
 * <p>
 * 供 linkx-dashboard 定时任务调用，按双游标 (时间字段, 源表唯一键) 增量拉取聚合好的统计 DTO。
 * im-jingxin 侧聚合关联表后返回完整 DTO，dashboard 侧只管去重写入。
 * <p>
 * 双游标原理：time 精度到秒，同秒可能有多条记录。游标条件等价于元组比较
 * {@code (time, id) > (timeAfter, idAfter)}，即 {@code time > timeAfter OR (time = timeAfter AND id > idAfter)}，
 * 保证不漏不重。
 * <p>
 * 首次拉取：timeAfter 和 idAfter 传 null，表示从头拉取。
 * 后续拉取：传上次返回的 lastTime/lastId。
 */
public interface StatisticsRpcApi {

    /**
     * 增量拉取协同任务统计明细（tb_static_task）。
     * <p>
     * 游标：(gmt_modified, id)，源表 tb_task.gmt_modified + tb_task.id。
     * 聚合：tb_task + tb_collaboration_post(post_name) + tb_task_expired(expired_time)
     *       + tb_task_response(首次回复) + tb_task_status_history(ignore/track/finish)。
     *
     * @param timeAfter 游标时间（上次最后一条的 task_updated_time），首次传 null
     * @param idAfter   游标ID（上次最后一条的 task_id），首次传 null，需与 timeAfter 同时传
     * @param pageSize  每页大小
     * @return 游标分页结果
     */
    StaticCursorResult<StaticTaskDTO> listStaticTaskByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 增量拉取建群记录统计明细（tb_static_create_group）。
     * <p>
     * 游标：(update_time, group_id)，源表 tb_create_group.update_time + tb_create_group.group_id。
     * 聚合：tb_create_group + tb_group_extends(group_type) + 警信(创群人姓名) + 部门服务(code→id)。
     *
     * @param timeAfter 游标时间（上次最后一条的 group_updated_time），首次传 null
     * @param idAfter   游标ID（上次最后一条的 group_id），首次传 null，需与 timeAfter 同时传
     * @param pageSize  每页大小
     * @return 游标分页结果
     */
    StaticCursorResult<StaticCreateGroupDTO> listStaticCreateGroupByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 增量拉取任务回复统计明细（tb_static_task_response）。
     * <p>
     * 游标：(gmt_created, id)，源表 tb_task_response.gmt_created + tb_task_response.id。
     * 聚合：tb_task_response + tb_collaboration_post(post_name)。
     *
     * @param timeAfter 游标时间（上次最后一条的 response_time），首次传 null
     * @param idAfter   游标ID（上次最后一条的 response_id），首次传 null，需与 timeAfter 同时传
     * @param pageSize  每页大小
     * @return 游标分页结果
     */
    StaticCursorResult<StaticTaskResponseDTO> listStaticTaskResponseByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 增量拉取协同岗上下岗统计明细（tb_static_coop_duty_switch）。
     * <p>
     * 游标：(create_time, id)，源表 tb_collaboration_attendance.create_time + tb_collaboration_attendance.id。
     * 聚合：tb_collaboration_attendance（user_id 仅 switchType=0 取 person_id，其余留空）。
     *
     * @param timeAfter 游标时间（上次最后一条的 gmt_create_time），首次传 null
     * @param idAfter   游标ID（上次最后一条的 attendance_id），首次传 null，需与 timeAfter 同时传
     * @param pageSize  每页大小
     * @return 游标分页结果
     */
    StaticCursorResult<StaticCoopDutySwitchDTO> listStaticCoopDutySwitchByCursor(Date timeAfter, Long idAfter, int pageSize);

    /**
     * 查询指定类型的协同岗列表（供 tb_static_photo_check 在 dashboard 侧聚合时使用）。
     * <p>
     * 协同岗数量 ≤300，全量拉到内存匹配。type=1 表示 1:14E 协同岗。
     * 返回的 {@link CollaborationPostVO#getRelatedUserIds()} 为逗号分隔的用户ID串，
     * 由 dashboard 侧解析后构建 Map&lt;userId, (postId, postName)&gt;。
     *
     * @param type 协同岗类型（1 表示 1:14E 协同岗）
     * @return 协同岗列表
     */
    List<CollaborationPostVO> listCoopPostsByType(Integer type);

    /**
     * 按身份证号批量查警信 userId（供 tb_static_photo_check 在 dashboard 侧聚合时使用）。
     * <p>
     * 核查记录源表 ai_agent_record 只存身份证号，需转成警信 userId 才能关联协同岗。
     * 由 im-jingxin 内部调警信 HTTP 接口实现，dashboard 侧无需直连警信。
     * 查不到的身份证不在返回 Map 中，调用方按"查不到则记日志不入库"处理。
     *
     * @param idCards 身份证号列表
     * @return idCard→userId 映射（查不到的不包含）
     */
    Map<String, Long> findUserIdsByIdCards(List<String> idCards);
}