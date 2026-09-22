package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.CollaborationRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationAttendanceService;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationStatisticsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskService;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@DubboService
public class CollaborationRpc implements CollaborationRpcApi {

    @Resource
    private CollaborationStatisticsService collaborationStatisticsService;
    @Resource
    private ICollaborationTaskService collaborationTaskService;
    @Resource
    private CollaborationAttendanceService collaborationAttendanceService;
    @Resource
    private CollaborationPostService collaborationPostService;

    @Override
    public CollaborationStatisticsVO count(String departmentCode, String startTime, String endTime) {
        return collaborationStatisticsService.count(departmentCode, startTime, endTime);
    }

    /**
     * 获取0人员在线的协同岗列表
     */
    @Override
    public List<CollaborationAttendance> listZeroOnDutyPosts(String departmentCode) {
        return collaborationStatisticsService.listZeroOnDutyPosts(departmentCode, null, null);
    }

    /**
     * 返回协同岗对应的所有群组id,参数支持批量, 协同岗标签 List<协同岗id，去查询群组id> Map<协同岗id, 群组>
     */
    @Override
    public Map<Long, List<Long>> getGroupIds(List<Long> collaborationIds, String startTime, String endTime) {
        return collaborationStatisticsService.getGroupIds(collaborationIds, startTime, endTime);
    }

    /**
     * 返回协同岗和被@总次数，支持批量查找分页 传入 List<协同岗id，去查询用户id> task 表去查询次数
     */
    @Override
    public List<CollaborationTaskCountResponse> getCollabCount(List<Long> collaborationIds, String startTime,
        String endTime) {
        return (collaborationStatisticsService.getCollabCount(collaborationIds, startTime, endTime));
    }

    /**
     * 协同岗处理问题平均时长统计
     */
    @Override
    public List<CollaborationReplyDurationVO> replyDuration(String departmentCode, String startTime, String endTime) {
        return (collaborationStatisticsService.replyDuration(departmentCode, startTime, endTime));
    }

    @Override
    public List<CollaborationReplyTotalVO> replyCount(String departmentCode, String startTime, String endTime) {
        return collaborationStatisticsService.replyCount(departmentCode, startTime, endTime);
    }

    @Override
    public List<CollaborationReplyTotalVO> replyCountAll(String startTime, String endTime) {
        return collaborationStatisticsService.replyCountAll(startTime, endTime);
    }

    @Override
    public List<CollaborationPostOnlineVO> onlineStatistics(String departmentCode, String startTime, String endTime) {
        return collaborationStatisticsService.onlineStatistics(departmentCode, startTime, endTime);
    }

    @Override
    public CcmdPage<CollaborationPostOnlineDurationVO> onlineDuration(CcmdPageParam pageParam, Date startTime,
        Date endTime, String departmentCode) {
        return collaborationStatisticsService.onlineDuration(pageParam, startTime, endTime, departmentCode);
    }

    @Override
    public List<CollaborationDispositionVO> dispositionReplyDurationList(String departmentCode, String startTime,
        String endTime) {
        return (collaborationStatisticsService.dispositionReplyDuration(departmentCode, startTime, endTime));
    }

    @Override
    public Page<CollaborationOverdueVO> getOverdueList(String departmentCode, String startTime, String endTime,
        Integer pageSize, Integer pageNum) {
        return (collaborationTaskService.getOverdueList(departmentCode, startTime, endTime, pageSize, pageNum));
    }

    @Override
    public Page<CollaborationAttendance> getPage(int pageNum, int pageSize, String postName, String orgName, Long orgId,
        String personName, String startTime, String endTime) {
        return (collaborationAttendanceService.getPage(pageNum, pageSize, postName, orgName, orgId, personName,
            startTime, endTime));
    }

    @Override
    public CcmdPage<CollaborationPostVO> postPage(CcmdPageParam pageParam, String departmentCode, Long groupId) {
        var page = collaborationPostService.listByDepartmentCode(pageParam, departmentCode, groupId);
        return page.mult(BeanCopyUtils.copyList(page.getRecords(), CollaborationPostVO::new));
    }

    @Override
    public CollaborationPostVO getCollaborationPostDetail(Long postId) {
        var data = collaborationPostService.getById(postId);
        return BeanCopyUtils.copyBean(data, CollaborationPostVO::new);
    }

    @Override
    public void updateUserName(Long userId, String newName) {
        collaborationPostService.updateUserName(userId, newName);
    }

    @Override
    public CollaborationTasksCountVO getCollaborationTasksCount(Long collaborationId) {
        return collaborationPostService.getCollaborationTasksCount(collaborationId);
    }

    @Override
    public Set<Long> listBoundUserIds(Integer type) {
        return collaborationPostService.listBoundUserIds(type);
    }
}