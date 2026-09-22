package com.tdtech.cloudcmd.linkx.dashboard.service;

import java.util.Date;
import java.util.List;

import com.tdtech.cloudcmd.linkx.dashboard.entity.GroupMsgSendStatic;
import com.tdtech.cloudcmd.linkx.dashboard.vo.GroupMsgSendStaticVO;

/**
 * 群组消息统计 Service 接口
 *
 * @author: S063874
 * @date: 2026-03-10
 */
public interface IGroupMsgSendStaticService {

    /**
     * 创建群组消息统计
     *
     * @param groupMsgSendStatic 群组消息统计信息
     * @return 消息ID
     */
    Long createGroupMsgSendStatic(GroupMsgSendStatic groupMsgSendStatic);

    /**
     * 批量创建群组消息统计
     *
     * @param groupMsgSendStaticList 群组消息统计列表
     * @return 插入数量
     */
    int batchCreateGroupMsgSendStatic(List<GroupMsgSendStatic> groupMsgSendStaticList);

    /**
     * 批量创建群组消息统计（过滤已存在的消息ID）
     *
     * @param groupMsgSendStaticList 群组消息统计列表
     * @return 插入数量
     */
    int batchCreateGroupMsgSendStaticWithFilter(List<GroupMsgSendStatic> groupMsgSendStaticList);

    /**
     * 更新群组消息统计
     *
     * @param groupMsgSendStatic 群组消息统计信息
     */
    void updateGroupMsgSendStatic(GroupMsgSendStatic groupMsgSendStatic);

    /**
     * 删除群组消息统计
     *
     * @param msgId 消息ID
     */
    void deleteGroupMsgSendStatic(Long msgId);

    /**
     * 批量删除群组消息统计
     *
     * @param msgIds 消息ID列表
     */
    void deleteGroupMsgSendStaticByIds(List<Long> msgIds);

    /**
     * 根据消息ID获取群组消息统计
     *
     * @param msgId 消息ID
     * @return 群组消息统计
     */
    GroupMsgSendStatic getGroupMsgSendStaticById(Long msgId);

    /**
     * 获取所有群组消息统计
     *
     * @return 群组消息统计列表
     */
    List<GroupMsgSendStatic> listGroupMsgSendStatic();

    /**
     * 根据会话ID获取群组消息统计列表
     *
     * @param sessionId 会话ID
     * @return 群组消息统计列表
     */
    List<GroupMsgSendStatic> listBySessionId(Long sessionId);

    /**
     * 根据用户ID获取群组消息统计列表
     *
     * @param userId 用户ID
     * @return 群组消息统计列表
     */
    List<GroupMsgSendStatic> listByUserId(Long userId);

    /**
     * 根据部门ID获取群组消息统计列表
     *
     * @param departmentId 部门ID
     * @return 群组消息统计列表
     */
    List<GroupMsgSendStatic> listByDepartmentId(Long departmentId);

    /**
     * 根据用户ID查询最新的消息统计（msgTime最晚的一条）
     *
     * @param userId 用户ID
     * @return 群组消息统计
     */
    GroupMsgSendStatic getLatestByUserId(Long userId);

    List<GroupMsgSendStaticVO> countTopMessageByUserId(String departmentCode, String startTime,
                                                       String endTime, Integer topn);
}