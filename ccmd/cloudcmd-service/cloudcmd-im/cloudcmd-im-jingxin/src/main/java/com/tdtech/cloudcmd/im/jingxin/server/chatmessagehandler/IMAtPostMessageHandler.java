package com.tdtech.cloudcmd.im.jingxin.server.chatmessagehandler;

import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.GroupMembers;
import com.tdtech.cloudcmd.im.jingxin.client.entity.GroupVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsResponse;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsTextMessage;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPostGroup;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostGroupMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class IMAtPostMessageHandler {

    @Resource
    private ICollaborationTaskService collaborationTaskService;

    @Resource
    @Qualifier("coopImHttpClient")
    private ImHttpClient coopImHttpClient;

    @Resource
    private CachedImConfig cachedImConfig;
    @Resource
    private CollaborationPostGroupMapper collaborationPostGroupMapper;

    @Resource
    private ImService imService;

    @Bean("atCollaborationPostMessage")
    public Consumer<WsResponse> processWsMessage() {
        return wsResponse -> {
            try {
                if (!Objects.equals(wsResponse.getModule(), "im")
                        || !Objects.equals(wsResponse.getNotifyType(), "TEXT_MSG")) {
                    return;
                }
                log.info("atCollaborationPostMessage on message: {}", wsResponse);
                String backendGenerationTask = cachedImConfig.getConfig("BACKEND_GENERATION_TASK");
                boolean isClosed = StringUtils.isBlank(backendGenerationTask) || (!"true".equals(backendGenerationTask));
                if (isClosed) {
                    log.info("atCollaborationPostMessage isClosed");
                    return;
                }
                var wsTextMessage = JsonUtil.convert(wsResponse.getData(), WsTextMessage.class);
                String text = wsTextMessage.getMsg().getText();
                boolean hasAtFlag = text.contains("[@");
                if (!hasAtFlag) {
                    log.info("atCollaborationPostMessage hasAtFlag false");
                    return;
                }
                // 根据群id查询所有协同岗
                Long groupId = Long.parseLong(wsTextMessage.getTo());
                GroupVo groupVo = coopImHttpClient.queryGroupDetail(groupId);
                List<GroupMembers> groupMembers = groupVo.getGroupMembers();
                List<Long> postIdList = groupMembers.stream().filter(member -> 2 == member.getType()).map(GroupMembers::getUserId).collect(Collectors.toList());
                log.info("atCollaborationPostMessage postIdList: {}", postIdList);
                List<Long> beenAtPostIdList = findBeenAtPostIdList(wsTextMessage, postIdList);
                log.info("atCollaborationPostMessage beenAtPostIdList: {}", beenAtPostIdList);
                if (CollectionUtils.isNotEmpty(beenAtPostIdList)) {
                    processAtCollaborationPostMsg(groupVo, wsTextMessage, beenAtPostIdList);
                }
            } catch (Exception e) {
                log.error("atCollaborationPostMessage consume error", e);
            }
        };
    }

    private List<Long> findBeenAtPostIdList(WsTextMessage wsTextMessage, List<Long> postIdList) {
        if (CollectionUtils.isEmpty(postIdList)) {
            return Collections.emptyList();
        }
        String text = wsTextMessage.getMsg().getText();
        // pc @所有人 || app @all
        boolean isAtAll = text.contains("@all") || text.contains("@所有人");
        if (isAtAll) {
            // 如果是协同岗@所有人，过滤掉本身
            Long from = wsTextMessage.getFrom();
            return postIdList.stream()
                    .filter(postId -> !postId.equals(from))
                    .collect(Collectors.toList());
        } else {
            // 看哪些协同岗被@了
            List<Long> beebAtList = postIdList.stream().filter(postId -> {
                String atFlag = String.format("[@%s:@", postId);
                return text.contains(atFlag);
            }).collect(Collectors.toList());
            // 返回被@的协同岗id
            return beebAtList;
        }
    }


    private List<CollaborationTaskCO> buildTaskParam(GroupVo groupVo, WsTextMessage wsTextMessage, List<Long> beenAtPostIdList) {
        // 消息来源用户id
        Long fromRealUserId = wsTextMessage.getFromRealUserId();
        Long from = wsTextMessage.getFrom();
        Long fromUserId = Objects.nonNull(fromRealUserId) ? fromRealUserId : from;
        ImUser fromUserInfo = imService.findUserInfo(String.valueOf(fromUserId));

        return beenAtPostIdList.stream().map(postId -> {
            // 根据协同岗id和群组id查询协同岗支持人员信息
            Optional<CollaborationPostGroup> collaborationPostGroup = collaborationPostGroupMapper.getOneByPostIdAndGroupId(postId, groupVo.getId());
            CollaborationTaskCO co = new CollaborationTaskCO();
            collaborationPostGroup.ifPresent(postGroup -> co.setUserId(postGroup.getSupportUserId()));
            co.setGroupId(groupVo.getId());
            co.setGroupName(StringUtils.isNotBlank(groupVo.getName()) ? groupVo.getName() : groupVo.getUndefinedName());
            co.setSeqid(wsTextMessage.getSeq());
            co.setText(wsTextMessage.getMsg().getText());
            co.setFromUserId(fromUserId);
            if (Objects.nonNull(fromUserInfo)) {
                co.setFromUserName(fromUserInfo.getName());
                co.setFromUserNick(fromUserInfo.getName());
                var userDepartments = fromUserInfo.getUserDepartments();
                if (userDepartments != null && !userDepartments.isEmpty()) {
                    userDepartments.stream().filter(ImUser.UserDepartment::getIsPrimary).findAny().ifPresent(dep -> {
                        co.setFromUserDepartmentId(dep.getId());
                        co.setFromUserDepartmentName(dep.getDepartmentName());
                    });
                }
            }
            co.setMsgFileId("");
            // 2026.04.24 逻辑变更，如果不存在在岗人员，创建待分配的任务
            co.setStatus(collaborationPostGroup.isPresent() &&  collaborationPostGroup.get().getSupportUserId() != null ? 1 : -1);
            co.setIsDeleted(0);
            co.setIcsMsgId(Long.parseLong(wsTextMessage.getMsgId()));
            co.setToExecutorId(fromUserId);
            co.setPostId(postId);

            return co;
        }).collect(Collectors.toList());

    }

    private void processAtCollaborationPostMsg(GroupVo groupVo, WsTextMessage wsTextMessage, List<Long> beenAtPostIdList) {
        List<CollaborationTaskCO> collaborationTaskCOS = buildTaskParam(groupVo, wsTextMessage, beenAtPostIdList);
        collaborationTaskCOS.forEach(task -> collaborationTaskService.save(task));
    }

}
