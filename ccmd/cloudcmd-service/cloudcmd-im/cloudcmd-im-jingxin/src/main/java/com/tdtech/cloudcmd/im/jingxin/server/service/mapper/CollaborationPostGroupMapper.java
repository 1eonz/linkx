package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPostGroup;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CreateGroup;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper
public interface CollaborationPostGroupMapper extends MPJBaseMapper<CollaborationPostGroup> {

  default List<CollaborationPostGroup> listNoSupportedByPosts(List<CollaborationPost> collaborationPosts) {
    return this.selectList(
        Wrappers.lambdaQuery(CollaborationPostGroup.class)
            .in(
                CollaborationPostGroup::getPostId,
                collaborationPosts.stream()
                    .map(CollaborationPost::getId)
                    .collect(Collectors.toList()))
            .isNull(CollaborationPostGroup::getSupportUserId));
  }

  default List<CollaborationPostGroup> listByUserId(Long userId) {
    return this.selectList(
        Wrappers.lambdaQuery(CollaborationPostGroup.class)
            .eq(CollaborationPostGroup::getSupportUserId, userId));
  }

  default List<CollaborationPostGroup> listByPostIds(List<Long> postIds) {
    return this.selectList(
        Wrappers.lambdaQuery(CollaborationPostGroup.class)
            .in(CollaborationPostGroup::getPostId, postIds));
  }

  default List<CollaborationPostGroup> findList(List<Long> postIds, String startTime, String endTime) {
    var wrapper = new MPJLambdaWrapper<>(CollaborationPostGroup.class).selectAll(CollaborationPostGroup.class);
    if (CollectionUtils.isNotEmpty(postIds)) {
      wrapper.in(CollaborationPostGroup::getPostId, postIds);
    }
    if (StringUtils.isNotBlank(startTime) || StringUtils.isNotBlank(endTime)) {
      wrapper = wrapper.leftJoin(CreateGroup.class, on -> on.eq(CollaborationPostGroup::getGroupId, CreateGroup::getGroupId));
    }
    if (StringUtils.isNotBlank(startTime)) {
      wrapper = wrapper.ge(CreateGroup::getCreateTime, DateFormatUtil.parseDate(startTime + " 00:00:00", DateFormatUtil.YYYY_MM_DD_HH_MM_SS));
    }
    if (StringUtils.isNotBlank(endTime)) {
      wrapper = wrapper.le(CreateGroup::getCreateTime, DateFormatUtil.parseDate(endTime + " 23:59:59", DateFormatUtil.YYYY_MM_DD_HH_MM_SS));
    }
    return selectList(wrapper);
  }

  default Optional<CollaborationPostGroup> getOneByPostIdAndGroupId(Long postId, Long groupId) {
    return Optional.ofNullable(
        this.selectList(
            Wrappers.lambdaQuery(CollaborationPostGroup.class)
                .eq(CollaborationPostGroup::getPostId, postId)
                .eq(CollaborationPostGroup::getGroupId, groupId))).stream().flatMap(Collection::stream).findAny();
  }

  default void deleteByPostIdAndGroupId(Long postId, Long groupId) {
            this.delete(
                    Wrappers.lambdaUpdate(CollaborationPostGroup.class)
                            .eq(CollaborationPostGroup::getPostId, postId)
                            .eq(CollaborationPostGroup::getGroupId, groupId));
  }

  default void setUser(List<CollaborationPostGroup> groups, Long userId) {
    this.update(
        null,
        Wrappers.lambdaUpdate(CollaborationPostGroup.class)
            .in(
                CollaborationPostGroup::getId,
                groups.stream().map(CollaborationPostGroup::getId).collect(Collectors.toList()))
            .set(CollaborationPostGroup::getSupportUserId, userId));
  }

  @Getter
  @Setter
  @ToString
  class UserCnt{
    private Long supportUserId;
    private Integer cnt;
  }

  @Select("select support_user_id,count(1) as cnt from tb_collaboration_post_group cpg where post_id=#{postId} group by support_user_id")
  List<UserCnt>cntUserByPost(@Param("postId")Long postId);

  @Update("update tb_collaboration_post_group set support_user_id=#{supportUserId} where post_id=#{postId} and group_id=#{groupId} and support_user_id is null")
  int updateByPostIdAndGroupId(@Param("postId") Long postId, @Param("supportUserId") Long supportUserId, @Param("groupId") Long groupId);
}
