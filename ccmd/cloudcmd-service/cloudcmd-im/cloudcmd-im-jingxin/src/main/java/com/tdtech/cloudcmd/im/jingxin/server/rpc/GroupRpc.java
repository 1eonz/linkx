package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.GroupRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.GroupCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.*;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketStatisticsCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CreateGroupMapper;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@DubboService
public class GroupRpc implements GroupRpcApi {
    @Resource
    private PoliceTicketService policeTicketService;
    @Resource
    private GroupExtendsService groupExtendsService;
    @Resource
    private CreateGroupMapper createGroupMapper;
    @Resource
    private TagService tagService;
    @Resource
    private UserGroupCareService userGroupCareService;
    @Resource
    private CollaborationStatisticsService collaborationStatisticsService;
    @Resource
    private LabelService labelService;

    @Override
    public R<Long> createGroup(OpenApiCreateGroupCO createGroupCO, String ownerId) {
        return R.success(policeTicketService.createGroup(createGroupCO, ownerId));
    }

    @Override
    public List<Long> listGroupIds(GroupQO groupQO) {
        return policeTicketService.groupIds(groupQO.getPolTicketId());
    }

    @Override
    public CcmdPage<GroupVO> listGroupPaged(@NotNull CcmdPageParam pageParam) {
        var createGroupCcmdPage = createGroupMapper.selectPageX(pageParam, Wrappers.lambdaQuery());
        return createGroupCcmdPage.mult(BeanCopyUtils.copyList(createGroupCcmdPage.getRecords(), GroupVO::new));
    }

    @Override
    public List<GroupTagVO> listTagsByGroups(@NotNull List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return tagService.listTagsByGroups(ids);
    }

    @Override
    public CcmdPage<GroupMemberVO> getArchiveMemberPage(Long groupId, @NotNull CcmdPageParam pageParam) {
        var page = groupExtendsService.getArchiveMemberPage(groupId, pageParam.getPageNum().intValue(),
            pageParam.getPageSize().intValue());
        return new CcmdPage<>(pageParam.getPageNum(), pageParam.getPageSize(), page.getTotal(),
            BeanCopyUtils.copyList(page.getRecords(), GroupMemberVO::new));
    }

    /**
     * 关注群组
     */
    @Override
    public void careGroup(@NotNull Long userId, @NotNull Long groupId) throws Exception {
        userGroupCareService.careGroup(userId, groupId);
    }

    /**
     * 取消关注群组
     */
    @Override
    public void uncareGroup(@NotNull Long userId, @NotNull Long groupId) throws Exception {
        userGroupCareService.uncareGroup(userId, groupId);
    }

    /**
     * 批量取消关注群组
     */
    @Override
    public void batchUncareGroups(@NotNull Long userId, @NotNull List<Long> groupIds) throws Exception {
        userGroupCareService.batchUncareGroups(userId, groupIds);
    }

    /**
     * 分页查询用户关注的群组
     */
    @Override
    public PageResult<Long> getCaredGroupsPage(@NotNull Long userId, @NotNull @Valid CcmdPageParam pageParam)
        throws Exception {
        var page = userGroupCareService.getCaredGroupsPage(userId, pageParam.getPageNum().intValue(),
            pageParam.getPageSize().intValue());
        var result = new PageResult<Long>();
        result.setCurrent(pageParam.getPageNum());
        result.setSize(pageParam.getPageSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords());
        return result;
    }

    /**
     * 返回当前协同群总数
     */
    @Override
    public List<GroupCreationCountVO> groupCreateCount(String departmentCode, String startTime, String endTime,
        Integer source) {
        return (collaborationStatisticsService.groupCreateCountByCode(departmentCode, startTime, endTime, source));
    }

    @Override
    public List<GroupCreationCountVO> groupCreateCountAll(String startTime, String endTime, Integer source) {
        return (collaborationStatisticsService.groupCreateCountAll(startTime, endTime, source));
    }

    @Override
    public Integer groupCreateAll(String departmentCode, String startTime, String endTime, Integer source) {
        return (collaborationStatisticsService.groupCreateAllCount(departmentCode, startTime, endTime, source));
    }

    @Override
    public R<Long> createGroupV2(OpenApiCreateGroupCOV2 openApiCreateGroupCOV2) {
        return R.success(policeTicketService.createGroupV2(openApiCreateGroupCOV2));
    }

    @Override
    public List<LabelVO> listAllLabels(String name, Integer scope, int level) {
        return labelService.getLabelVOs(name, scope, level);
    }

    @Override
    public List<PoliceTicketStatisticsCO> getPoliceTicketStatistics(List<Long> orgIds) {
        var data = policeTicketService.statistics(orgIds);
        return BeanCopyUtils.copyList(data, PoliceTicketStatisticsCO::new);
    }

    @Override
    public CcmdPage<OpenApiGroupVO> listGroupsByUser(OpenApiGroupQO qo) {
        var page = groupExtendsService.listGroupsByUser(qo);
        var records = page.getRecords().stream().map(dto -> {
            var vo = new OpenApiGroupVO();
            vo.setId(dto.getGroupId());
            vo.setName(dto.getGroupName());
            vo.setAvatar(dto.getAvatarImg());
            vo.setType(dto.getGroupType() != null ? String.valueOf(dto.getGroupType()) : null);
            vo.setOwnerId(dto.getOwnerId());
            vo.setGmtCreated(dto.getGmtCreated() != null ? Date.from(dto.getGmtCreated().atZone(ZoneId.systemDefault()).toInstant()) : null);
            return vo;
        }).collect(Collectors.toList());
        return new CcmdPage<>((long) qo.getPageNum(), (long) qo.getPageSize(), page.getTotal(), records);
    }

    @Override
    public OpenApiGroupCountVO getGroupCountByUserId(Long userId) {
        return groupExtendsService.getGroupCountByUserId(userId);
    }

    @Override
    public UpdateGroupMemberResultVO updateGroupMember(Long groupId, UpdateGroupMemberCO co, Long operatorUserId) {
        return groupExtendsService.updateGroupMember(groupId, co, operatorUserId);
    }

    @Override
    public R<Long> createGroupByIdCardsAndUserIds(OpenApiCreateGroupCOV3 openApiCreateGroupCOV3, String userId, String idCard) {
        return policeTicketService.createGroupByIdCardsAndUserIds(openApiCreateGroupCOV3, userId, idCard);
    }
}
