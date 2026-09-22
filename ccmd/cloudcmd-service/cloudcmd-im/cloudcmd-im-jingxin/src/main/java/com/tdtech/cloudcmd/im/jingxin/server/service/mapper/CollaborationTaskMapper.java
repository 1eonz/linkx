package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import java.util.List;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationDispositionVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationOverdueVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationReplyDurationVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationTaskCountResponse;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationReplyTotalVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTask;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollabsTaskListReqCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.StaticTaskAggVO;

/**
 * <p>
 * 设施资源信息表（包括摄像头、治安岗亭、消防栓等） Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-11
 */
@Mapper
public interface CollaborationTaskMapper extends MPJBaseMapper<CollaborationTask> {

    Page<CollaborationTask> listPage(Page<CollaborationTask> page,
        @Param("qo") CollabsTaskListReqCO collabsTaskListReqCO);

    CollaborationTask getCollaborationTask(@Param("id") Long taskId);

    Integer queryNumByStatus(@Param("statusList") List<Integer> status, @Param("userId") Long userId,
        @Param("postId") Long postId, @Param("userIdList") List<Long> userIdList, @Param("startTime") String startTime,
        @Param("endTime") String endTime);

    List<CollaborationTask> findListByCondition(@Param("statusList") List<Integer> status, @Param("userId") Long userId,
        @Param("postIdList") List<Long> postIdList, @Param("userIdList") List<Long> userIdList, @Param("startTime") String startTime,
        @Param("endTime") String endTime);

    Integer selectCountById(@Param("status") Integer status, @Param("userId") Long userId, @Param("min") Integer min,
        @Param("max") Integer max);

    Integer updateGroupName(@Param("groupId") Long groupId, @Param("groupName") String groupName);

    List<CollaborationReplyTotalVO> replyCount(@Param("departmentCode") List<Long> departmentCode,
                                               @Param("postIdList") List<Long> postIdList,
                                               @Param("startTime") String startTime, @Param("endTime") String endTime);

    List<CollaborationReplyDurationVO> replyDuration(@Param("departmentCode") List<Long> departmentCode,
        @Param("postIdList") List<Long> postIdList, @Param("startTime") String startTime,
        @Param("endTime") String endTime);

    List<CollaborationDispositionVO> dispositionCount(@Param("departmentCode") List<String> departmentCode,
        @Param("startTime") String startTime, @Param("endTime") String endTime);

    ;

    List<CollaborationDispositionVO> dispositionReplyDuration(@Param("departmentCode") List<String> departmentCode,
        @Param("startTime") String startTime, @Param("endTime") String endTime);

    Page<CollaborationOverdueVO> getOverdueList(Page<CollaborationOverdueVO> page,@Param("departmentCode") List<String> departmentCode,
                                                @Param("startTime") String startTime, @Param("endTime") String endTime);

    List<CollaborationTaskCountResponse> selectCountByUserIds(@Param("collaborationIds") List<Long> collaborationIds,
        @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 更新任务执行人和状态
     *
     * @param postId        协同岗id
     * @param userId        用户id
     * @param rawStatus     原本状态
     * @param targetStatus  目标状态
     * @return 更新的数量
     */
    int assignTask(@Param("postId") Long postId, @Param("userId") Long userId,
                   @Param("rawStatus") Integer rawStatus, @Param("targetStatus") Integer targetStatus);

    /**
     * 游标分页拉取协同任务统计明细（一条 SQL 关联出全部聚合字段）。
     *
     * <p>关联：
     * <ul>
     *   <li>tb_collaboration_post（post_name，LEFT JOIN by post_id）</li>
     *   <li>tb_task_expired（expired_time，LEFT JOIN by task_id）</li>
     *   <li>tb_task_response 首次回复（子查询取 gmt_created 最早一条）</li>
     *   <li>tb_task_status_history track(2)/finish(3)/ignore(4) 各最早一条（子查询取首条）</li>
     * </ul>
     *
     * <p>双游标：{@code (gmt_modified, id) > (timeAfter, idAfter)}，即
     * {@code gmt_modified > timeAfter OR (gmt_modified = timeAfter AND id > idAfter)}。
     * 首次拉取 timeAfter/idAfter 传 null。多查 1 条（pageSize+1）用于判断 hasMore。
     *
     * <p>SQL 实现见 mapper/CollaborationTaskMapper.xml#selectStaticTaskAggByCursor。
     *
     * @param timeAfter 游标时间（上次最后一条的 gmt_modified），首次传 null
     * @param idAfter   游标ID（上次最后一条的 id），首次传 null
     * @param pageSize  每页大小（实际查 pageSize+1 条）
     * @return 聚合 VO 列表
     */
    List<StaticTaskAggVO> selectStaticTaskAggByCursor(@Param("timeAfter") java.util.Date timeAfter,
                                                      @Param("idAfter") Long idAfter,
                                                      @Param("pageSize") int pageSize);
}