package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DynamicGroupQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DynamicGroupVO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.ExitLocationShareVO;
import com.tdtech.cloudcmd.icp.proxy.entity.DynamicGroupMember;
import com.tdtech.cloudcmd.icp.proxy.repo.DynamicGroupMemberMapper;
import com.tdtech.cloudcmd.linkx.third.api.dto.LocationShareExitDto;
import com.tdtech.cloudcmd.linkx.third.api.rpc.LocationShareRpcService;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Valid
@Service
@Slf4j
public class DynamicGroupService {
    private static final String BUTTON_REDIS_KEY = "cloudcmd:icp:dynamicgroup:button:";
    private static final String HEARTBEAT_REDIS_KEY = "cloudcmd:icp:heartbeat:%s:%s";

    @Resource
    private DynamicGroupMemberMapper dynamicGroupMemberMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private StreamBridge streamBridge;
    @Resource
    private RedisUtil redisUtil;
    @DubboReference
    private LocationShareRpcService locationShareRpcService;

    /**
     * 统一退出位置共享
     * 编排三个操作：1.群主重置按钮状态 2.调用linkx-third退出位置共享 3.删除动态群组成员
     * @param vo 退出位置共享请求
     */
    public void exitLocationShare(ExitLocationShareVO vo) {
        var groupId = vo.getGroupId();
        var userId = vo.getUserId();
        var isOwner = vo.getIsOwner();

        if (isOwner != null && isOwner == 1) {
            try {
                var buttonStatus = vo.getStatus() != null ? vo.getStatus() : "true";
                updateButton(groupId, buttonStatus);
            } catch (Exception e) {
                log.error("群主退出时重置按钮状态失败, groupId={}", groupId, e);
            }
        }

        if (vo.getShareId() != null) {
            try {
                var dto = new LocationShareExitDto();
                dto.setShareId(vo.getShareId());
                dto.setUserId(Long.parseLong(userId));
                dto.setExitType(vo.getExitType() != null ? vo.getExitType() : 0);
                dto.setExitDesc(vo.getExitDesc() != null ? vo.getExitDesc() : "");
                locationShareRpcService.exitLocationShare(dto);
                log.info("退出位置共享成功, shareId={}, userId={}", vo.getShareId(), userId);
            } catch (Exception e) {
                log.error("调用linkx-third退出位置共享失败, shareId={}, userId={}", vo.getShareId(), userId, e);
            }
        }

        delMembers(groupId, List.of(userId), isOwner != null ? isOwner : 0);
    }

    public void upsertGroup(@Validated DynamicGroupVO dynamicGroupVO) {
        var groupId = dynamicGroupVO.getGroupId();
        delMembers(groupId, null,1);
        var dynamicGroupMembers = BeanCopyUtils.copyList(dynamicGroupVO.getMembers(), DynamicGroupMember::new);
        addMember(groupId, dynamicGroupVO.getGroupName(), dynamicGroupVO.getOwnerId(), dynamicGroupMembers);
        var uids = dynamicGroupMembers.stream().map(DynamicGroupMember::getUserId).collect(Collectors.toList());
        cagentMessage("GROUP_UPSERT", uids, groupId);
    }

    public void addMember(String groupId, String groupName, String ownerId,
        List<DynamicGroupMember> dynamicGroupMembers) {
        for (var dynamicGroupMember : dynamicGroupMembers) {
            dynamicGroupMember.setGroupId(groupId);
            dynamicGroupMember.setId(idWorker.nextId());
            dynamicGroupMember.setGroupName(groupName);
            dynamicGroupMember.setOwnerId(ownerId);
        }
        dynamicGroupMemberMapper.insertBatch(dynamicGroupMembers);
        var members = dynamicGroupMemberMapper.selectList(
            Wrappers.lambdaQuery(DynamicGroupMember.class).eq(DynamicGroupMember::getGroupId, groupId));
        cagentMessage("MEMBER_ADD", members.stream().map(DynamicGroupMember::getUserId).collect(Collectors.toList()),
            Map.of("groupId", groupId, "members", dynamicGroupMembers));
    }

    /**
     * 成员为空时删除整个群组
     */
    public void delMembers(@NotNull String groupId, List<String> userId,Integer isOwner) {
        var members = dynamicGroupMemberMapper.selectList(
            Wrappers.lambdaQuery(DynamicGroupMember.class).eq(DynamicGroupMember::getGroupId, groupId));
        List<String> uids = members.stream().map(DynamicGroupMember::getUserId).collect(Collectors.toList());
        // 如果是群主退出，则直接删除动态组然后清空发送websocket消息 清空所有成员心跳

        if (1 == isOwner) {
            // 删除整个动态组
            dynamicGroupMemberMapper.delete(Wrappers.lambdaQuery(DynamicGroupMember.class)//
                    .eq(DynamicGroupMember::getGroupId, groupId));
            if(CollectionUtils.isNotEmpty(uids)){
                // 清空所有人心跳
                uids.forEach(e-> redisUtil.del(String.format(HEARTBEAT_REDIS_KEY, groupId, e)));
                // 通知成员，但是不通知群主自己
                uids = uids.stream().filter(e-> !userId.contains(e)).collect(Collectors.toList());
                log.info("通知成员：{}",uids);
                if(CollectionUtils.isNotEmpty(uids)){
                    cagentMessage("MEMBER_DEL",
                            uids,
                            Map.of("groupId", groupId, "userId", userId));
                }
            }
        }else{
            // 如果不是群主退出，则只删除该群组的成员
            dynamicGroupMemberMapper.delete(Wrappers.lambdaQuery(DynamicGroupMember.class)//
                    .eq(DynamicGroupMember::getGroupId, groupId)//
                    .in(userId != null && !userId.isEmpty(), DynamicGroupMember::getUserId, userId));
            // 清空自己的心跳
            if(CollectionUtils.isNotEmpty(userId)){
                userId.forEach(e-> redisUtil.del(String.format(HEARTBEAT_REDIS_KEY, groupId, e)));
                // 通知其他成员我退出了
                uids = uids.stream().filter(e-> !userId.contains(e)).collect(Collectors.toList());
                log.info("通知成员：{}",uids);
                if(CollectionUtils.isNotEmpty(uids)){
                    cagentMessage("MEMBER_DEL",
                            uids,
                            Map.of("groupId", groupId, "userId", userId));
                }
            }
        }
    }

    public DynamicGroupVO groupInfo(@NotNull String groupId) {
        var dynamicGroupMembers = dynamicGroupMemberMapper.selectList(
            Wrappers.lambdaQuery(DynamicGroupMember.class).eq(DynamicGroupMember::getGroupId, groupId));
        if (dynamicGroupMembers == null || dynamicGroupMembers.isEmpty()) {
            return null;
        }
        var dynamicGroupVO = new DynamicGroupVO();
        dynamicGroupVO.setGroupId(groupId);
        dynamicGroupVO.setGroupName(dynamicGroupMembers.get(0).getGroupName());
        dynamicGroupVO.setOwnerId(dynamicGroupMembers.get(0).getOwnerId());
        dynamicGroupVO.setMembers(dynamicGroupMembers);
        return dynamicGroupVO;
    }

    public CcmdPage<DynamicGroupVO> listGroup(@Validated DynamicGroupQO dynamicGroupQO) {
        var groupIdPage = dynamicGroupMemberMapper.selectDistinctGroupIdsByCondition(dynamicGroupQO, dynamicGroupQO);
        if (groupIdPage.getRecords() == null || groupIdPage.getRecords().isEmpty()) {
            return CcmdPage.empty(dynamicGroupQO.getPageNum(), dynamicGroupQO.getPageSize());
        }
        var dynamicGroupMembers =
            dynamicGroupMemberMapper.selectInX(DynamicGroupMember::getGroupId, groupIdPage.getRecords());
        var collect = dynamicGroupMembers.stream()//
            .collect(Collectors.groupingBy(DynamicGroupMember::getGroupId))//
            .entrySet()//
            .stream()//
            .map(entry -> new DynamicGroupVO(entry.getKey(), entry.getValue().get(0).getGroupName(),
                entry.getValue().get(0).getOwnerId(), entry.getValue()))//
            .collect(Collectors.toList());
        return groupIdPage.mult(collect);
    }

    private void cagentMessage(String notifyType, List<String> uids, Object payload) {
        log.info("通知类型：{}，用户ID：{}，消息内容：{}",notifyType, uids, payload);
        var build =
            new CagentMqFrame().toBuilder().typeSubSystemMessage("ICP_DYNAMIC_GROUP").unicast().userIds(uids).build()
                .body("ICP_DYNAMIC_GROUP", notifyType, payload).build();
        streamBridge.send("cloudcmd-cagent", build);
    }

    public String getButton(String groupId) {
        return redisUtil.get(BUTTON_REDIS_KEY + groupId, String.class);
    }

    public void updateButton(String groupId, String status) {
        redisUtil.set(BUTTON_REDIS_KEY + groupId, status);
        var members = dynamicGroupMemberMapper.selectList(
            Wrappers.lambdaQuery(DynamicGroupMember.class).eq(DynamicGroupMember::getGroupId, groupId));
        cagentMessage("GROUP_BUTTON", members.stream().map(DynamicGroupMember::getUserId).collect(Collectors.toList()),
            Map.of("groupId", groupId, "status", status));
    }

    /**
     * 更新心跳时间戳
     */
    public void updateHeartbeat(String groupId, String userId) {
        String key = String.format(HEARTBEAT_REDIS_KEY, groupId, userId);
        // 设置过期时间为3个心跳间隔（假设前端每30秒发送一次心跳）
        redisUtil.set(key, String.valueOf(System.currentTimeMillis()), Duration.ofSeconds(120));
    }

    /**
     * 检查心跳是否超时
     */
    public boolean isHeartbeatTimeout(String groupId, String userId) {
        String key = String.format(HEARTBEAT_REDIS_KEY, groupId, userId);
        String timestampStr = redisUtil.get(key, String.class);
        if (timestampStr == null) {
            return true;
        }
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            // 超过90秒（3个心跳间隔，假设30秒一次）视为超时
            return now - timestamp > 90000;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * 处理心跳超时，删除超时用户
     */
    public void handleHeartbeatTimeout(String groupId, List<String> userIds) {
        List<String> timeoutUsers = new ArrayList<>();
        for (String userId : userIds) {
            if (isHeartbeatTimeout(groupId, userId)) {
                timeoutUsers.add(userId);
            }
        }
        if (!timeoutUsers.isEmpty()) {
            log.info("定时任务调用删除人员接口");
            delMembers(groupId, timeoutUsers,0);
        }
    }

    /**
     * 定时检查所有心跳超时的用户
     */
    @Scheduled(initialDelay = 60000L, fixedDelay = 30000L) // 每分钟检查一次
    public void checkAllHeartbeatTimeouts() {
        try {
            // 获取所有心跳相关的Redis键
            Set<String> keys = redisUtil.keys("cloudcmd:icp:heartbeat:*:*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            // 按群组分组
            Map<String, List<String>> groupUsersMap = new HashMap<>();
            for (String key : keys) {
                // 解析key: cloudcmd:icp:heartbeat:{groupId}:{userId}
                String[] parts = key.split(":");
                if (parts.length == 5) {
                    String groupId = parts[3];
                    String userId = parts[4];
                    if (!groupUsersMap.containsKey(groupId)) {
                        groupUsersMap.put(groupId, new ArrayList<>());
                    }
                    groupUsersMap.get(groupId).add(userId);
                }
            }

            // 检查每个群组的用户心跳
            for (Map.Entry<String, List<String>> entry : groupUsersMap.entrySet()) {
                String groupId = entry.getKey();
                List<String> userIds = entry.getValue();
                handleHeartbeatTimeout(groupId, userIds);
            }
        } catch (Exception e) {
            // 记录错误但不影响其他操作
            e.printStackTrace();
        }
    }
}
