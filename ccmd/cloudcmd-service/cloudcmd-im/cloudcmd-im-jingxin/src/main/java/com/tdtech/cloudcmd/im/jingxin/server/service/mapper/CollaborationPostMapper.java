package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.mysql.enhance.ExMPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Mapper
public interface CollaborationPostMapper extends ExMPJBaseMapper<CollaborationPost> {
    Page<CollaborationPost> selectPageWithCondition(Page<CollaborationPost> page, @Param("name") String name,
                                                    @Param("orgName") String orgName, @Param("orgIds") List<Long> orgIds,
                                                    @Param("relatedUserNames") String relatedUserNames, @Param("startTime") String startTime,
                                                    @Param("endTime") String endTime, @Param("type") Integer type);

    /**
     * 根据userId查询用户所绑定到的协同岗（以不存在某用户id为另一用户id的subString为前提）
     *
     * @param userId
     * @return
     */
    List<CollaborationPost> listByUserId(@Param("userId") String userId);

    List<CollaborationPost> findPostList(@Param("userIdList") List<String> userIdList);

    /**
     * 获取存在的协同岗id
     *
     * @param ids 协同岗id
     * @return 存在的协同岗id列表
     */
    List<Long> getByIds(@Param("idList") Collection<Long> ids);

    /**
     * 查询所有协同岗id
     *
     * @return 协同岗id列表
     */
    List<Long> listAllIds();

    List<CollaborationPost> selectListByCondition(@Param("name") String name, @Param("orgName") String orgName,
                                                  @Param("orgIds") List<String> orgIds, @Param("relatedUserNames") String relatedUserNames,
                                                  @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 批量插入
     *
     * @param posts
     * @return
     */
    int insertBatch(@Param("posts") List<CollaborationPost> posts);

    /**
     * 批量查询，包含已删除的
     *
     * @param idList
     * @return
     */
    List<CollaborationPost> findBatchContainsDeleted(@Param("idList") List<Long> idList);

    List<CollaborationPost> findPostListByIds(@Param("userIdList") List<String> userIdList, @Param("departments") Collection<String> dept);

    List<String> selectMemberByPostIds(@Param("postMembers") List<Long> postMembers);

    List<CollaborationPost> findBatchByType(@Param("idList") List<Long> idList,@Param("type")Integer type);


    List<CollaborationPost> selectByTimeRange(@Param("startTime") String startTime, @Param("departmentIds") List<Long> departmentIds);
}
