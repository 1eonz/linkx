package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupCountVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.UpdateGroupMemberCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.UpdateGroupMemberResultVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.IMOfflineMsgItemVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
public interface GroupExtendsService extends IService<GroupExtends> {
    GroupExtends getByGroupId(Long groupId) throws Exception;

    boolean saveOrUpdateGroupExtends(GroupExtends groupExtends) throws Exception;

    boolean updateArchiveInfo(Long groupId, GroupExtends groupExtends) throws Exception;

    IPage<UserCareGroupDTO> getPageList(Integer pageNum, Integer pageSize, Integer archived,String archivedTime, String keywords,Integer groupType) throws Exception;

    void updateGroupTags(Long groupId, Long tagIds,Long userId,boolean isPush) throws Exception;

    void batchUpdateGroupTags(Long groupId, List<Long> tagIds,Long userId,boolean isPush) throws Exception;

    IPage<UserCareGroupDTO> getTagsPageList(Integer pageNum, Integer pageSize, Integer archived, String archivedTime, String keywords, String groupName, String tagName,Integer groupType, Integer scope) throws Exception;

    List<ArchiveTimelineDTO> getArchiveTimeline(String keywords);

    List<TagCountDTO> getCountGroupTags(Long userId,String departmentCode,String startTime,String endTime);

    Integer updateArchive(Long groupId,Long userId);

    void asyncArchive(Long groupId, Long userId, String operationUserToken, GroupExtends groupExtend, Boolean isArchive);

    List<ArchiveTimelineDTO> getCaredArchivedPageList(String keywords);


    IPage<IMOfflineMsgItemVo> getArchiveMsgPage(Long groupId, String keywords, Integer pageNum, Integer pageSize, Date startTime, Date endTime, String from);

    IPage<TbChatMember> getArchiveMemberPage(Long groupId, Integer pageNum, Integer pageSize);

    IMOfflineMsgItemVo getGroupMessageById(Long groupId, Long messageId);
    IPage<GroupExtendsVO> getArchiveList(String startTime, String endTime, String keywords, Integer pageNum, Integer pageSize);

    void downloadArchivedGroupFiles(List<GroupExtendsVO> groupExtendsList, HttpServletResponse response);

    List<GroupExtendsVO> getGroupExtendsList(List<Long> groupIds);

    void deleteArchivedGroupFiles(List<GroupExtendsVO> groupExtendsList);

    List<GroupExtends> findListByGroupIds(List<Long> groupIdList);

    IPage<UserCareGroupDTO> listGroupsByUser(OpenApiGroupQO qo);

    OpenApiGroupCountVO getGroupCountByUserId(Long userId);

    UpdateGroupMemberResultVO updateGroupMember(Long groupId, UpdateGroupMemberCO co, Long operatorUserId);
}
