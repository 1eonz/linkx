package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCoopRecieved;

import java.util.List;

/**
 * 协同岗接收信息 Service
 */
public interface IUserCoopRecievedService extends IService<UserCoopRecieved> {

    /**
     * 接收协同岗分享
     *
     * @param coopUserId     协同岗ID
     * @param originPeerId   来源节点ID
     * @param originPeerName 来源节点名称
     * @param targetOrgId    目标组织ID
     * @param coopUserName   协同岗名称
     * @param iconUrl        图标相对路径
     * @param orgId          协同岗所属组织ID
     * @param orgName        协同岗所属组织名称
     */
    void receiveCoopUser(Long coopUserId, String originPeerId, String originPeerName, Long targetOrgId,
                         String coopUserName, String iconUrl, Long orgId, String orgName);

    /**
     * 取消接收的协同岗（逻辑删除，置 status=0）
     *
     * @param coopUserId   协同岗ID
     * @param originPeerId 来源节点ID
     */
    void cancelReceived(Long coopUserId, String originPeerId);

    /**
     * 查询本节点已接收的有效协同岗列表
     *
     * @return 接收记录列表
     */
    List<UserCoopRecieved> listActiveReceived();

    /**
     * 查询指定协同岗的接收记录
     *
     * @param coopUserId   协同岗ID
     * @param originPeerId 来源节点ID
     * @return 接收记录
     */
    UserCoopRecieved getByCoopUserAndOrigin(Long coopUserId, String originPeerId);

    /**
     * 查询指定协同岗的有效接收记录（不限来源）
     *
     * @param coopUserId 协同岗ID
     * @return 有效接收记录，不存在返回 null
     */
    UserCoopRecieved getActiveByCoopUserId(Long coopUserId);
}
