package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserCreateRequestBody;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserPolicyReq;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserReq;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCoopRecieved;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCoopShared;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserCandidateVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.CoopUserShareVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.SharedNodeVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopRecievedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopSharedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.UserCoopSharedMapper;
import com.tdtech.linkx.node.api.P2pWsPushRpcApi;
import com.tdtech.linkx.node.api.PeerNodeRpcApi;
import com.tdtech.linkx.node.util.PeerNodeIconUrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 协同岗分享授权信息 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCoopSharedServiceImpl extends ServiceImpl<UserCoopSharedMapper, UserCoopShared>
        implements IUserCoopSharedService {

    private final ImHttpClient imHttpClient;
    private final CollaborationPostService collaborationPostService;
    private final IUserCoopRecievedService coopRecievedService;

    @DubboReference
    private P2pWsPushRpcApi p2pWsPushRpcApi;

    @DubboReference
    private PeerNodeRpcApi peerNodeRpcApi;

    @Override
    @Transactional
    public void shareCoopUser(Long coopUserId, String peerId, Long orgId, String orgName, Long sharedByUserId) {
        // 判断协同岗是否在本地
        CollaborationPost post = collaborationPostService.getById(coopUserId);
        boolean isLocal = (post != null);

        // 校验间接分享约束：非本地协同岗，需检查是否为间接接收
        if (!isLocal) {
            if (isIndirectlyShared(coopUserId)) {
                throw new RuntimeException("协同岗已被间接分享，不允许再次分享: coopUserId=" + coopUserId);
            }
        }

        // 获取协同岗原始归属
        String originPeerId = isLocal ? null : getCoopUserOrigin(coopUserId);

        // 校验是否已分享给该节点
        UserCoopShared existing = getExistingRecord(coopUserId, peerId);
        if (existing != null) {
            throw new RuntimeException("协同岗已分享给该节点: coopUserId=" + coopUserId + ", peerId=" + peerId);
        }

        UserCoopShared record = new UserCoopShared();
        record.setCoopUserId(coopUserId);
        record.setCoopUserOrigin(originPeerId);
        record.setPeerId(peerId);
        record.setTargetOrgId(orgId);
        record.setTargetOrgName(orgName);
        record.setSharedByUserId(sharedByUserId);
        record.setGmtCreated(LocalDateTime.now());

        try {
            save(record);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("协同岗已分享给该节点: coopUserId=" + coopUserId + ", peerId=" + peerId);
        }

        // 调用警信接口更新协同岗可见范围
        updateCoopUserVisibility(coopUserId, orgId, true);

        // 推送 WS share 消息给接收方（携带协同岗详细信息）
        // 本地协同岗从 CollaborationPost 取，非本地协同岗从 UserCoopRecieved 取已存储的详细信息
        CollaborationPost sharePost = post;
        if (sharePost == null) {
            UserCoopRecieved received = coopRecievedService.getActiveByCoopUserId(coopUserId);
            if (received != null) {
                sharePost = new CollaborationPost();
                sharePost.setPostName(received.getCoopUserName());
                sharePost.setIconUrl(received.getIconUrl());
                sharePost.setOrgId(received.getOrgId());
                sharePost.setOrgName(received.getOrgName());
            }
        }
        pushShareMessage(peerId, coopUserId, originPeerId, orgId, sharePost);

        // 如果是间接分享（非本地协同岗），通知原始归属方记录链路
        if (!isLocal && originPeerId != null) {
            pushIndirectNotification(originPeerId, coopUserId, peerId, orgId);
        }

        log.info("shareCoopUser: coopUserId={}, peerId={}, orgId={}, isLocal={}", coopUserId, peerId, orgId, isLocal);
    }

    @Override
    @Transactional
    public void unshareCoopUser(Long coopUserId, String peerId, Long orgId) {
        UserCoopShared record = getExistingRecord(coopUserId, peerId);
        if (record == null) {
            log.warn("unshareCoopUser: record not found, coopUserId={}, peerId={}", coopUserId, peerId);
            return;
        }
        removeById(record.getId());

        // 如果是原始归属方取消直接分享，同时删除间接分享记录（级联取消下级）
        if (record.getCoopUserOrigin() == null) {
            cascadeUnshare(coopUserId);
        }

        // 调用警信接口移除可见范围
        updateCoopUserVisibility(coopUserId, orgId, false);

        // 推送 WS unshare 消息给接收方
        pushUnshareMessage(peerId, coopUserId, record.getCoopUserOrigin(), orgId);

        log.info("unshareCoopUser: coopUserId={}, peerId={}, orgId={}", coopUserId, peerId, orgId);
    }

    @Override
    public boolean isIndirectlyShared(Long coopUserId) {
        // 查 tb_user_coop_recieved：originPeerName 非 null 表示间接接收，不可再分享
        UserCoopRecieved received = coopRecievedService.getActiveByCoopUserId(coopUserId);
        return received != null && received.getOriginPeerName() != null;
    }

    @Override
    public List<UserCoopShared> listByPeerId(String peerId) {
        return list(new LambdaQueryWrapper<UserCoopShared>()
                .eq(UserCoopShared::getPeerId, peerId));
    }

    @Override
    public List<UserCoopShared> listByCoopUserId(Long coopUserId) {
        return list(new LambdaQueryWrapper<UserCoopShared>()
                .eq(UserCoopShared::getCoopUserId, coopUserId));
    }

    @Override
    public String getCoopUserOrigin(Long coopUserId) {
        // 本地协同岗返回 null
        CollaborationPost post = collaborationPostService.getById(coopUserId);
        if (post != null) {
            return null;
        }
        // 非本地协同岗，查 tb_user_coop_recieved 获取原始归属
        UserCoopRecieved received = coopRecievedService.getActiveByCoopUserId(coopUserId);
        return received != null ? received.getOriginPeerId() : null;
    }

    @Override
    @Transactional
    public void recordIndirectShare(Long coopUserId, String fromPeerId, String targetPeerId, Long targetOrgId) {
        // 检查是否已存在
        UserCoopShared existing = getOne(new LambdaQueryWrapper<UserCoopShared>()
                .eq(UserCoopShared::getCoopUserId, coopUserId)
                .eq(UserCoopShared::getPeerId, targetPeerId));
        if (existing != null) {
            log.info("recordIndirectShare: already exists, coopUserId={}, targetPeerId={}", coopUserId, targetPeerId);
            return;
        }

        UserCoopShared record = new UserCoopShared();
        record.setCoopUserId(coopUserId);
        record.setCoopUserOrigin(fromPeerId);
        record.setPeerId(targetPeerId);
        record.setTargetOrgId(targetOrgId);
        record.setGmtCreated(LocalDateTime.now());

        try {
            save(record);
        } catch (DuplicateKeyException e) {
            log.warn("recordIndirectShare: duplicate, coopUserId={}, targetPeerId={}", coopUserId, targetPeerId);
        }

        log.info("recordIndirectShare: coopUserId={}, fromPeerId={}, targetPeerId={}",
                coopUserId, fromPeerId, targetPeerId);
    }

    @Override
    @Transactional
    public void cascadeUnshare(Long coopUserId) {
        // 查询该协同岗的所有间接分享记录（coopUserOrigin 非空）
        List<UserCoopShared> indirectRecords = list(new LambdaQueryWrapper<UserCoopShared>()
                .eq(UserCoopShared::getCoopUserId, coopUserId)
                .isNotNull(UserCoopShared::getCoopUserOrigin));

        for (UserCoopShared record : indirectRecords) {
            // 删除分享记录
            removeById(record.getId());

            // 调用警信接口移除可见范围
            if (record.getTargetOrgId() != null) {
                updateCoopUserVisibility(coopUserId, record.getTargetOrgId(), false);
            }

            // 推送 WS unshare 消息
            pushUnshareMessage(record.getPeerId(), coopUserId, record.getCoopUserOrigin(), record.getTargetOrgId());

            log.info("cascadeUnshare: coopUserId={}, peerId={}", coopUserId, record.getPeerId());
        }
    }

    @Override
    public IPage<CoopUserShareVO> pageCoopUsers(Integer type, int pageNum, int pageSize, String coopUserName) {
        List<CoopUserShareVO> result = new java.util.ArrayList<>();

        // 本节点的协同岗（只展示有有效分享记录的）
        if (type == 0 || type == 1) {
            List<CollaborationPost> localPosts = collaborationPostService.findAll();
            List<Long> postIds = localPosts.stream().map(CollaborationPost::getId).collect(Collectors.toList());
            if (!postIds.isEmpty()) {
                Map<Long, List<UserCoopShared>> sharedMap = list(new LambdaQueryWrapper<UserCoopShared>()
                        .in(UserCoopShared::getCoopUserId, postIds))
                        .stream()
                        .collect(Collectors.groupingBy(UserCoopShared::getCoopUserId));

                localPosts.stream()
                        .filter(post -> sharedMap.containsKey(post.getId()))
                        .map(post -> {
                            CoopUserShareVO vo = new CoopUserShareVO();
                            vo.setCoopUserId(post.getId());
                            vo.setCoopUserName(post.getPostName());
                            vo.setOrgId(post.getOrgId());
                            vo.setOrgName(post.getOrgName());
                            vo.setOriginType("local");
                            vo.setSharedNodes(sharedMap.get(post.getId()));
                            // 本节点的协同岗走到这里说明已有分享记录
                            vo.setIsShared(1);
                            return vo;
                        })
                        .forEach(result::add);
            }
        }

        // 接收的协同岗
        if (type == 0 || type == 2) {
            List<UserCoopRecieved> receivedList = coopRecievedService.listActiveReceived();
            // 批量查询这些接收的协同岗是否已被本节点再次分享出去
            List<Long> receivedCoopUserIds = receivedList.stream()
                    .map(UserCoopRecieved::getCoopUserId)
                    .collect(Collectors.toList());
            Set<Long> reSharedIds = listSharedCoopUserIds(receivedCoopUserIds);

            // 批量查询来源节点名称（DB 中 origin_peer_name 可能为 null，通过 RPC 补全）
            Set<String> originPeerIds = receivedList.stream()
                    .map(UserCoopRecieved::getOriginPeerId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<String, String> peerNameMap = Collections.emptyMap();
            if (!originPeerIds.isEmpty()) {
                try {
                    Map<String, PeerNodeRpcApi.PeerNodeInfo> infoMap =
                            peerNodeRpcApi.getPeerInfoMap(new java.util.ArrayList<>(originPeerIds));
                    if (infoMap != null) {
                        peerNameMap = infoMap.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey,
                                        e -> e.getValue().getName() != null ? e.getValue().getName() : "",
                                        (a, b) -> a));
                    }
                } catch (Exception e) {
                    log.warn("pageCoopUsers: getPeerInfoMap failed", e);
                }
            }

            final Map<String, String> finalPeerNameMap = peerNameMap;
            receivedList.stream().map(received -> {
                CoopUserShareVO vo = new CoopUserShareVO();
                vo.setCoopUserId(received.getCoopUserId());
                vo.setCoopUserName(received.getCoopUserName());
                vo.setOrgId(received.getOrgId());
                vo.setOrgName(received.getOrgName());
                vo.setOriginPeerId(received.getOriginPeerId());
                // origin_peer_name 为 null 时，通过 RPC 查节点名称补全
                String originName = received.getOriginPeerName();
                if (originName == null && received.getOriginPeerId() != null) {
                    originName = finalPeerNameMap.get(received.getOriginPeerId());
                }
                vo.setOriginPeerName(originName);
                vo.setTargetOrgId(received.getTargetOrgId());
                vo.setOriginType("received");
                vo.setReceivedTime(received.getReceivedTime());
                // 接收的协同岗：本节点是否再次分享出去
                vo.setIsShared(reSharedIds.contains(received.getCoopUserId()) ? 1 : 0);
                return vo;
            }).forEach(result::add);
        }

        // 按协同岗名称模糊过滤
        if (coopUserName != null && !coopUserName.isEmpty()) {
            String keyword = coopUserName.toLowerCase();
            result = result.stream()
                    .filter(vo -> vo.getCoopUserName() != null
                            && vo.getCoopUserName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        // 内存分页
        int total = result.size();
        int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<CoopUserShareVO> pageRecords = (fromIndex < toIndex)
                ? result.subList(fromIndex, toIndex)
                : new java.util.ArrayList<>();

        Page<CoopUserShareVO> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.setRecords(pageRecords);
        return page;
    }

    @Override
    public List<SharedNodeVO> listSharedNodes(Long coopUserId) {
        List<UserCoopShared> sharedList = listByCoopUserId(coopUserId);
        if (sharedList.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        List<String> peerIds = sharedList.stream()
                .map(UserCoopShared::getPeerId)
                .distinct()
                .collect(Collectors.toList());
        Map<String, PeerNodeRpcApi.PeerNodeInfo> peerInfoMap = peerNodeRpcApi.getPeerInfoMap(peerIds);

        return sharedList.stream().map(record -> {
            SharedNodeVO vo = new SharedNodeVO();
            vo.setId(record.getId());
            vo.setPeerId(record.getPeerId());
            PeerNodeRpcApi.PeerNodeInfo info = peerInfoMap.get(record.getPeerId());
            if (info != null) {
                // 名称没有时展示IP
                vo.setPeerName(info.getName() != null ? info.getName() : info.getIp());
                vo.setPeerIp(info.getIp());
            }
            vo.setCoopUserOrigin(record.getCoopUserOrigin());
            vo.setTargetOrgId(record.getTargetOrgId());
            vo.setTargetOrgName(record.getTargetOrgName());
            vo.setSharedByUserId(record.getSharedByUserId());
            vo.setGmtCreated(record.getGmtCreated());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取已存在的分享记录
     */
    private UserCoopShared getExistingRecord(Long coopUserId, String peerId) {
        return getOne(new LambdaQueryWrapper<UserCoopShared>()
                .eq(UserCoopShared::getCoopUserId, coopUserId)
                .eq(UserCoopShared::getPeerId, peerId));
    }

    @Override
    public Set<Long> listSharedCoopUserIds(List<Long> coopUserIds) {
        if (coopUserIds == null || coopUserIds.isEmpty()) {
            return Collections.emptySet();
        }
        return list(new LambdaQueryWrapper<UserCoopShared>()
                .select(UserCoopShared::getCoopUserId)
                .in(UserCoopShared::getCoopUserId, coopUserIds))
                .stream()
                .map(UserCoopShared::getCoopUserId)
                .collect(Collectors.toSet());
    }

    /**
     * 调用警信接口更新协同岗可见范围
     */
    private void updateCoopUserVisibility(Long coopUserId, Long orgId, boolean isShare) {
        try {
            CollaborationPost post = collaborationPostService.getById(coopUserId);
            if (post == null) {
                log.warn("updateCoopUserVisibility: post not found, coopUserId={}", coopUserId);
                return;
            }

            UserCreateRequestBody requestBody = new UserCreateRequestBody();
            UserReq userReq = new UserReq();
            userReq.setName(post.getPostName());
            userReq.setAvatar(post.getFileId());
            userReq.setCategory(1);

            UserPolicyReq policy = new UserPolicyReq();
            if (orgId == null) {
                // 不指定组织，全局可见
                policy.setType(0);
            } else {
                policy.setType(1);
                String existingDepts = getExistingWhiteDepts(coopUserId);
                if (isShare) {
                    // 白名单追加目标组织，同时确保协同岗所属本端组织也在白名单中，避免本端不可见
                    String whiteDepts = mergeDepartmentIds(existingDepts, String.valueOf(orgId));
                    if (post.getOrgId() != null) {
                        whiteDepts = mergeDepartmentIds(whiteDepts, String.valueOf(post.getOrgId()));
                    }
                    policy.setWhiteDepartmentIds(whiteDepts);
                } else {
                    // 取消分享时，本端组织不移除（保底可见），只移除对端组织
                    String removed = removeDepartmentId(existingDepts, String.valueOf(orgId));
                    if (post.getOrgId() != null) {
                        removed = mergeDepartmentIds(removed, String.valueOf(post.getOrgId()));
                    }
                    policy.setWhiteDepartmentIds(removed);
                }
            }
            userReq.setPolicy(policy);

            if (post.getRelatedUserIds() != null && !post.getRelatedUserIds().isEmpty()) {
                List<Long> bindUserIds = Arrays.stream(post.getRelatedUserIds().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                userReq.setBindUserIds(bindUserIds);
            }

            requestBody.setUserReq(userReq);
            imHttpClient.updateCollborationUser(requestBody, String.valueOf(coopUserId));
            log.info("updateCoopUserVisibility: coopUserId={}, orgId={}, isShare={}", coopUserId, orgId, isShare);
        } catch (Exception e) {
            log.error("updateCoopUserVisibility: failed, coopUserId={}, orgId={}, isShare={}",
                    coopUserId, orgId, isShare, e);
            throw new RuntimeException("更新警信协同岗可见范围失败: " + e.getMessage(), e);
        }
    }

    private String getExistingWhiteDepts(Long coopUserId) {
        List<UserCoopShared> sharedList = listByCoopUserId(coopUserId);
        return sharedList.stream()
                .map(UserCoopShared::getTargetOrgId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String mergeDepartmentIds(String existing, String newId) {
        if (existing == null || existing.isEmpty()) {
            return newId;
        }
        List<String> ids = Arrays.stream(existing.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (!ids.contains(newId)) {
            ids.add(newId);
        }
        return String.join(",", ids);
    }

    private String removeDepartmentId(String existing, String removeId) {
        if (existing == null || existing.isEmpty()) {
            return "";
        }
        return Arrays.stream(existing.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals(removeId))
                .collect(Collectors.joining(","));
    }

    /**
     * 推送 WS share 消息给接收方
     * 携带协同岗详细信息（名称、图标、所属组织），接收方落库到 tb_user_coop_recieved
     */
    private void pushShareMessage(String peerId, Long coopUserId, String originPeerId, Long targetOrgId,
                                  CollaborationPost post) {
        try {
            String coopUserName = post != null ? post.getPostName() : null;
            String iconUrl = post != null ? post.getIconUrl() : null;
            Long orgId = post != null ? post.getOrgId() : null;
            String orgName = post != null ? post.getOrgName() : null;
            p2pWsPushRpcApi.pushShare(peerId, coopUserId, originPeerId, peerId, targetOrgId,
                    coopUserName, iconUrl, orgId, orgName);
        } catch (Exception e) {
            log.error("pushShareMessage: failed, peerId={}, coopUserId={}", peerId, coopUserId, e);
        }
    }

    /**
     * 通知原始归属方记录间接分享链路
     * 间接通知不需要携带详细信息（原始归属方已有协同岗信息）
     */
    private void pushIndirectNotification(String originPeerId, Long coopUserId, String targetPeerId, Long targetOrgId) {
        try {
            p2pWsPushRpcApi.pushShare(originPeerId, coopUserId, originPeerId, targetPeerId, targetOrgId,
                    null, null, null, null);
        } catch (Exception e) {
            log.error("pushIndirectNotification: failed, originPeerId={}, coopUserId={}", originPeerId, coopUserId, e);
        }
    }

    /**
     * 推送 WS unshare 消息给接收方
     */
    private void pushUnshareMessage(String peerId, Long coopUserId, String originPeerId, Long targetOrgId) {
        try {
            p2pWsPushRpcApi.pushUnshare(peerId, coopUserId, originPeerId, targetOrgId);
        } catch (Exception e) {
            log.error("pushUnshareMessage: failed, peerId={}, coopUserId={}", peerId, coopUserId, e);
        }
    }

    @Override
    public IPage<CoopUserCandidateVO> pageGroupCandidates(String peerId, int pageNum, int pageSize, String coopUserName) {
        List<CoopUserCandidateVO> result = new java.util.ArrayList<>();

        if (peerId == null || peerId.isEmpty()) {
            // 本节点协同岗：查 CollaborationPost，iconUrl 返回相对路径（前端用 getIp() 拼）
            List<CollaborationPost> posts = collaborationPostService.findAll();
            for (CollaborationPost post : posts) {
                CoopUserCandidateVO vo = new CoopUserCandidateVO();
                vo.setCoopUserId(post.getId());
                vo.setCoopUserName(post.getPostName());
                vo.setIconUrl(post.getIconUrl());
                vo.setOrgId(post.getOrgId());
                vo.setOrgName(post.getOrgName());
                result.add(vo);
            }
        } else {
            // 对端分享给本节点的协同岗：查 UserCoopRecieved，iconUrl 拼对端完整 URL
            List<UserCoopRecieved> receivedList = coopRecievedService.listActiveReceived().stream()
                    .filter(r -> peerId.equals(r.getOriginPeerId()))
                    .collect(Collectors.toList());
            if (!receivedList.isEmpty()) {
                // 查对端节点 IP
                String originName = null;
                String baseUrl = null;
                try {
                    Map<String, PeerNodeRpcApi.PeerNodeInfo> infoMap =
                            peerNodeRpcApi.getPeerInfoMap(Collections.singletonList(peerId));
                    if (infoMap != null && infoMap.containsKey(peerId)) {
                        originName = infoMap.get(peerId).getName();
                        baseUrl = PeerNodeIconUrlUtil.buildBaseUrl(infoMap.get(peerId).getIp());
                    }
                } catch (Exception e) {
                    log.warn("pageGroupCandidates: getPeerInfoMap failed, peerId={}", peerId, e);
                }

                for (UserCoopRecieved received : receivedList) {
                    CoopUserCandidateVO vo = getCoopUserCandidateVO(received, baseUrl, originName);
                    result.add(vo);
                }
            }
        }

        // 按协同岗名称模糊过滤
        if (coopUserName != null && !coopUserName.isEmpty()) {
            String keyword = coopUserName.toLowerCase();
            result = result.stream()
                    .filter(vo -> vo.getCoopUserName() != null
                            && vo.getCoopUserName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        // 内存分页
        int total = result.size();
        int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<CoopUserCandidateVO> pageRecords = (fromIndex < toIndex)
                ? result.subList(fromIndex, toIndex)
                : new java.util.ArrayList<>();

        Page<CoopUserCandidateVO> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.setRecords(pageRecords);
        return page;
    }

    @NotNull
    private static CoopUserCandidateVO getCoopUserCandidateVO(UserCoopRecieved received, String baseUrl, String originName) {
        CoopUserCandidateVO vo = new CoopUserCandidateVO();
        vo.setCoopUserId(received.getCoopUserId());
        vo.setCoopUserName(received.getCoopUserName());
        // iconUrl 拼对端完整 URL（方案1：图片直连）
        vo.setIconUrl(PeerNodeIconUrlUtil.prepend(baseUrl, received.getIconUrl()));
        vo.setOrgId(received.getOrgId());
        vo.setOrgName(received.getOrgName());
        vo.setOriginPeerId(received.getOriginPeerId());
        vo.setOriginPeerName(originName != null ? originName : received.getOriginPeerName());
        return vo;
    }
}
