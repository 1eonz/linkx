package com.tdtech.cloudcmd.im.jingxin.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.*;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CollaborationRpcApi {
    CollaborationStatisticsVO count(String departmentCode, String startTime, String endTime);

    List<CollaborationAttendance> listZeroOnDutyPosts(String departmentCode);

    Map<Long, List<Long>> getGroupIds(List<Long> collaborationIds, String startTime, String endTime);

    List<CollaborationTaskCountResponse> getCollabCount(List<Long> collaborationIds, String startTime, String endTime);

    List<CollaborationReplyDurationVO> replyDuration(String departmentCode, String startTime, String endTime);

    List<CollaborationReplyTotalVO> replyCount(String departmentCode, String startTime, String endTime);

    /**
     * 获取所有部门的回复数统计
     */
    List<CollaborationReplyTotalVO> replyCountAll(String startTime, String endTime);

    List<CollaborationPostOnlineVO> onlineStatistics(String departmentCode, String startTime, String endTime);

    CcmdPage<CollaborationPostOnlineDurationVO> onlineDuration(CcmdPageParam pageParam, Date startTime, Date endTime,
        String departmentCode);

    List<CollaborationDispositionVO> dispositionReplyDurationList(String departmentCode, String startTime,
        String endTime);

    Page<CollaborationOverdueVO> getOverdueList(String departmentCode, String startTime, String endTime,
        Integer pageSize, Integer pageNum);

    Page<CollaborationAttendance> getPage(int pageNum, int pageSize, String postName, String orgName, Long orgId,
        String personName, String startTime, String endTime);

    CcmdPage<CollaborationPostVO> postPage(CcmdPageParam pageParam, String departmentCode, Long groupId);

    CollaborationPostVO getCollaborationPostDetail(Long postId);

    void updateUserName(Long userId, String newName);

    CollaborationTasksCountVO getCollaborationTasksCount(Long collaborationId);

    /**
     * 查询指定类型协同岗关联的所有用户ID（去重）。
     * <p>
     * 用于给用户列表标注"是否被协同岗绑定"。
     *
     * @param type 协同岗类型；为 null 时不过滤类型
     * @return 被绑定的用户ID集合
     */
    Set<Long> listBoundUserIds(Integer type);
}
