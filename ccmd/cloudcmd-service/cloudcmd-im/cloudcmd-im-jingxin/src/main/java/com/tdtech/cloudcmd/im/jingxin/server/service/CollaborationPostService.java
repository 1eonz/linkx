
package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationTasksCountVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImPage;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserCreateRequestBody;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CooperationUserVO;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CollaborationPostService {
    Page<CollaborationPost> getPage(int pageNum, int pageSize, String name, String orgName, Long orgId,
        String relatedUserNames, String startTime, String endTime,Integer type);

    /**
     * 根据userId查询用户所绑定到的协同岗（以不存在某用户id为另一用户id的subString为前提）
     * 
     * @param userId
     * @return
     */
    List<CollaborationPost> listByUserId(String userId);

    List<CollaborationPost> queryByName(String name);

    CcmdPage<CollaborationPost> listByDepartmentCode(CcmdPageParam pageParam, String orgCode, Long groupId);

    /**
     * 查询指定选择上下文中已选中的协同岗 ID。
     *
     * @param postIds 当前分页内的协同岗 ID
     * @param selectionType 选择上下文类型：coopLevel-协同岗层级；functionalDepartment-职能分类
     * @param selectionId 选择上下文 ID，保持字符串入参，由具体 selectionType 决定是否转换类型
     * @return 已选中的协同岗 ID
     */
    Set<Long> selectedPostIds(List<Long> postIds, String selectionType, String selectionId);

    boolean save(CollaborationPost collaborationPost);

    boolean savePost(UserCreateRequestBody userCreateRequestBody, CollaborationPost collaborationPost);

    void checkPostParam(CollaborationPost collaborationPost);

    boolean update(CollaborationPost collaborationPost, List<Long> typeIds);

    boolean delete(Long id);

    boolean delete(CollaborationPost collaborationPost);

    boolean deleteBatch(List<Long> ids);

    boolean deleteBatchPost(List<CollaborationPost> dataList);

    List<ImUser> queryUser(String code, Integer includeChildren, String keywords, String name);

    ImPage<ImUser> queryUserByPage(String code, Integer type, Integer pageNum, Integer pageSize, Integer includeChildren,
                                   String keywords, String name);

    /**
     * 从im同步协同岗数据-不存在持续增量数据故仅支持功能上线首次调用同步，不做额外增量数据校验及幂等操作
     * 
     * @param user 当前用户
     * @return 插入条数
     */
    Integer syncPostFromIm(UserInfo user);

    boolean getProcess();

    /**
     * 获取im同步状态
     * 
     * @return 同步状态 true已同步，false位同步
     */
    Boolean getImSyncStatus();

    void exportToExcel(String fileName, String name, String orgName, String orgId, String relatedUserNames,
        String startTime, String endTime, HttpServletResponse response) throws IOException;

    CollaborationPost getById(Long id);

    List<CollaborationPost> getByIds(List<Long> ids);

    List<CollaborationPost> findList(List<Long> orgIdList);

    /**
     * 批量查询，包含已删除的
     * @param idList
     * @return
     */
    List<CollaborationPost> findBatchContainsDeleted(List<Long> idList);

    List<CollaborationPost> findAll();

    List<ImUser> getUserList(List<String> userIdList);

    Map<Long, List<CollaborationPost>> buildUserIdListMap(List<String> userIdList);

    List<CooperationUserVO> queryByUserId(String userId);

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
