package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiGroupCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ArchiveTimelineDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.GroupExtends;
import com.tdtech.cloudcmd.im.jingxin.server.entity.GroupExtendsVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCareGroupDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Mapper
public interface GroupExtendsMapper extends BaseMapper<GroupExtends> {
    Logger log = LoggerFactory.getLogger(GroupExtendsMapper.class);

    GroupExtends selectByGroupId(@Param("groupId") Long groupId);

    int updateArchiveStatus(@Param("groupId") Long groupId, @Param("extends") GroupExtends groupExtends);

    IPage<UserCareGroupDTO> selectPageList(
            IPage<UserCareGroupDTO> page,
            @Param("userId") Long userId,
            @Param("archiveds") List<Integer> archiveds,
            @Param("archivedTime") String archivedTime,
            @Param("keywords") String keywords,
            @Param("orgIds") List<String> orgIds,
            @Param("postIdList") List<String> postIdList,
            @Param("groupType") Integer groupType);

    IPage<UserCareGroupDTO> getTagsPageList(IPage<UserCareGroupDTO> page,
                                            @Param("userId") Long userId,
                                            @Param("archived") Integer archived,
                                            @Param("archivedTime") String archivedTime,
                                            @Param("keywords") String keywords,
                                            @Param("groupName") String groupName,
                                            @Param("tagName") String tagName,
                                            @Param("orgIds") List<String> orgIds,
                                            @Param("postIdList") List<String> postIdList,
                                            @Param("groupType") Integer groupType,
                                            @Param("scope") Integer scope);

    List<ArchiveTimelineDTO> selectCombinedArchiveTimeline(
            @Param("userId") Long userId,
            @Param("keywords") String keywords,
            @Param("orgIds") List<String> orgIds,
            @Param("postIdList") List<String> postIdList);

    List<ArchiveTimelineDTO> selectCaredArchivedPageList(@Param("userId") Long userId,
                                                         @Param("keywords") String keywords, @Param("orgIds") List<String> orgIds ,@Param("postIdList")List<String> postIdList);

    IPage<GroupExtendsVO> getArchiveList(IPage<GroupExtendsVO> page,@Param("startTime") String startTime, @Param("endTime") String endTime, @Param("keywords") String keywords);

    List<GroupExtendsVO> getArchiveListByIds(@Param("ids")List<Long> ids);

    default void insertIgnoreDuplicated(GroupExtends groupExtends) {
        try {
            this.insert(groupExtends);
        } catch (DuplicateKeyException e) {
            log.warn("insertIgnoreDuplicated on duplicated insert:{}", groupExtends);
        }
    }

    IPage<UserCareGroupDTO> listGroupsByUser(
            IPage<UserCareGroupDTO> page,
            @Param("userId") Long userId,
            @Param("scope") Integer scope,
            @Param("groupType") Integer groupType,
            @Param("createType") Integer createType,
            @Param("postIdList") List<String> postIdList,
            @Param("orgIds") List<String> orgIds);

    OpenApiGroupCountVO getGroupCountByUserId(
            @Param("userId") Long userId,
            @Param("postIdList") List<String> postIdList
    );
}
