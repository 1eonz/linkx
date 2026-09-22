package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.GroupCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.*;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketStatisticsCO;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public interface GroupRpcApi {
    R<Long> createGroup(OpenApiCreateGroupCO createGroupVO, String ownerId) throws BusinessException;

    List<Long> listGroupIds(GroupQO groupQO) throws BusinessException;

    CcmdPage<GroupVO> listGroupPaged(CcmdPageParam pageParam);

    List<GroupTagVO> listTagsByGroups(List<Long> ids);

    CcmdPage<GroupMemberVO> getArchiveMemberPage(Long groupId, CcmdPageParam pageParam);

    void careGroup(@NotNull Long userId, @NotNull Long groupId) throws Exception;

    void uncareGroup(@NotNull Long userId, @NotNull Long groupId) throws Exception;

    void batchUncareGroups(@NotNull Long userId, @NotNull List<Long> groupIds) throws Exception;

    PageResult<Long> getCaredGroupsPage(@NotNull Long userId, @NotNull @Valid CcmdPageParam pageParam) throws Exception;

    List<GroupCreationCountVO> groupCreateCount(String departmentCode, String startTime, String endTime, Integer source);

    List<GroupCreationCountVO> groupCreateCountAll(String startTime, String endTime, Integer source);

    Integer groupCreateAll(String departmentCode, String startTime, String endTime,Integer source);

    R<Long> createGroupV2(OpenApiCreateGroupCOV2 openApiCreateGroupCOV2);

    List<LabelVO> listAllLabels(String name, Integer scope, int level);

    List<PoliceTicketStatisticsCO> getPoliceTicketStatistics(List<Long> orgIds);

    CcmdPage<OpenApiGroupVO> listGroupsByUser(OpenApiGroupQO qo);

    OpenApiGroupCountVO getGroupCountByUserId(Long userId);

    UpdateGroupMemberResultVO updateGroupMember(Long groupId, UpdateGroupMemberCO co, Long operatorUserId);

    R<Long> createGroupByIdCardsAndUserIds(OpenApiCreateGroupCOV3 openApiCreateGroupCOV3, String userId, String idCard);
}
