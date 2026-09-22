package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountResp;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationDispositionVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostOnlineDurationVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostOnlineVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationReplyDurationVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationReplyTotalVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationTaskCountResponse;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.GroupCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PersonnelVerificationVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCreationCountVO;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author lsc
 * @date 2025/7/18
 **/
public interface CollaborationStatisticsService {

    CollaborationStatisticsVO count(String departmentCode, String startTime, String endTime);

    CollaborationTaskStatisticsVO calTaskCount(String departmentCode, String startTime, String endTime);

    List<CollaborationAttendance> listZeroOnDutyPosts(String departmentCode, String startTime, String endTime);

    List<CollaborationReplyTotalVO> replyCount(String departmentCode, String startTime, String endTime);

    List<CollaborationReplyTotalVO> replyCountAll(String startTime, String endTime);

    List<CollaborationReplyDurationVO> replyDuration(String departmentCode, String startTime, String endTime);

    List<CollaborationDispositionVO> dispositionCount(String departmentCode, String startTime, String endTime);

    List<CollaborationDispositionVO> dispositionReplyDuration(String departmentCode, String startTime, String endTime);

    List<GroupCreationCountVO> groupCreateCountByCode(String departmentCode, String startTime, String endTime,
        Integer source);

    List<GroupCreationCountVO> groupCreateCountAll(String startTime, String endTime, Integer source);

    List<UserCreationCountVO> groupCreateCountByUser(String departmentCode, String startTime, String endTime);

    List<CollaborationPostOnlineVO> onlineStatistics(String departmentCode, String startTime, String endTime);

    CcmdPage<CollaborationPostOnlineDurationVO> onlineDuration(CcmdPageParam pageParam, Date startTime, Date endTime,
        String departmentCode);

    Map<Long, List<Long>> getGroupIds(List<Long> collaborationIds, String startTime, String endTime);

    int groupCreateAllCount(String departmentCode, String startTime, String endTime, Integer source);

    List<RecordCountResp> countAgentRecords(List<String> departmentCode, String startTime, String endTime,
        String personName, String agentName, String category, List<RecordCountReq.GroupEnum> group);

    List<PersonnelVerificationVO> personnelVerification(String departmentCode, String startTime, String endTime);

    List<CollaborationTaskCountResponse> getCollabCount(List<Long> collaborationIds, String startTime, String endTime);
}
