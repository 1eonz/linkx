package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCoopShared;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserCandidateVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserShareVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.SharedNodeVO;

import java.util.List;
import java.util.Set;

/**
 * 协同岗分享授权信息 Service
 */
public interface IUserCoopSharedService extends IService<UserCoopShared> {

    /**
     * 分享协同岗
     * 由分享方节点调用，处理流程：
     * 1. 校验间接分享约束
     * 2. 写入 tb_user_coop_shared
     * 3. 调用警信接口更新协同岗可见范围
     * 4. 推送 WS share 消息给接收方
     *
     * @param coopUserId     协同岗ID
     * @param peerId         目标节点ID
     * @param orgId          目标组织ID
     * @param sharedByUserId 分享人ID
     */
    void shareCoopUser(Long coopUserId, String peerId, Long orgId, String orgName, Long sharedByUserId);

    /**
     * 取消分享协同岗
     * 由取消方节点调用，处理流程：
     * 1. 删除 tb_user_coop_shared 记录
     * 2. 调用警信接口移除可见范围
     * 3. 推送 WS unshare 消息给接收方
     *
     * @param coopUserId 协同岗ID
     * @param peerId     目标节点ID
     * @param orgId      目标组织ID
     */
    void unshareCoopUser(Long coopUserId, String peerId, Long orgId);

    /**
     * 查询指定协同岗是否已被间接分享
     *
     * @param coopUserId 协同岗ID
     * @return true=已被间接分享
     */
    boolean isIndirectlyShared(Long coopUserId);

    /**
     * 查询指定节点被分享的协同岗列表
     *
     * @param peerId 节点ID
     * @return 分享记录列表
     */
    List<UserCoopShared> listByPeerId(String peerId);

    /**
     * 查询指定协同岗的所有分享记录
     *
     * @param coopUserId 协同岗ID
     * @return 分享记录列表
     */
    List<UserCoopShared> listByCoopUserId(Long coopUserId);

    /**
     * 获取协同岗的来源 peerId（本节点的返回 null）
     *
     * @param coopUserId 协同岗ID
     * @return 来源 peerId，本节点的协同岗返回 null
     */
    String getCoopUserOrigin(Long coopUserId);

    /**
     * 记录间接分享链路
     * 当本节点（原始归属方）收到间接通知时调用，在 tb_user_coop_shared 中记录链路
     *
     * @param coopUserId   协同岗ID
     * @param fromPeerId   中间方节点ID（如 B）
     * @param targetPeerId 最终目标节点ID（如 C）
     * @param targetOrgId  目标组织ID
     */
    void recordIndirectShare(Long coopUserId, String fromPeerId, String targetPeerId, Long targetOrgId);

    /**
     * 级联取消分享
     * 当本节点收到 unshare 通知后，检查本节点是否把该协同岗分享给了其他节点，
     * 如果有则级联取消（删除 shared 记录、调警信移除可见范围、推 WS unshare）
     *
     * @param coopUserId 协同岗ID
     */
    void cascadeUnshare(Long coopUserId);

    /**
     * 查询协同岗分享列表（分页）
     *
     * @param type        类型：0=全部；1=当前节点的；2=接收的
     * @param pageNum     页码（从1开始）
     * @param pageSize    每页大小
     * @param coopUserName 协同岗名称（模糊搜索，null=不搜索）
     * @return 分页分享信息
     */
    IPage<CoopUserShareVO> pageCoopUsers(Integer type, int pageNum, int pageSize, String coopUserName);

    /**
     * 查询协同岗已分享的节点列表（含节点名称等友好信息）
     *
     * @param coopUserId 协同岗ID
     * @return 分享节点列表
     */
    List<SharedNodeVO> listSharedNodes(Long coopUserId);

    /**
     * 批量查询已存在分享记录的协同岗ID集合
     * 用于列表接口填充 isShared 字段：返回值包含某 coopUserId 即表示该协同岗已分享
     *
     * @param coopUserIds 协同岗ID列表（空或 null 返回空集合）
     * @return 已分享的协同岗ID集合
     */
    Set<Long> listSharedCoopUserIds(List<Long> coopUserIds);

    /**
     * 查询建群候选协同岗列表（分页）
     * peerId 为空：查本节点协同岗（CollaborationPost），iconUrl 返回相对路径
     * peerId 不为空：查对端分享给本节点的协同岗（UserCoopRecieved），iconUrl 拼对端完整 URL
     *
     * @param peerId       目标节点ID（null=本节点）
     * @param pageNum      页码（从1开始）
     * @param pageSize     每页大小
     * @param coopUserName 协同岗名称（模糊搜索，null=不搜索）
     * @return 分页候选协同岗
     */
    IPage<CoopUserCandidateVO> pageGroupCandidates(String peerId, int pageNum, int pageSize, String coopUserName);
}
