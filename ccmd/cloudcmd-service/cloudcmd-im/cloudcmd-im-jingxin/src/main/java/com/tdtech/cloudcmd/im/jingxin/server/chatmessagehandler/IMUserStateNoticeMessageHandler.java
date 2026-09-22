package com.tdtech.cloudcmd.im.jingxin.server.chatmessagehandler;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsResponse;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsUserStateEvents;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsUserStateNoticeMessage;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.enums.SwitchTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationAttendanceService;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostGroupService;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceSwitchMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class IMUserStateNoticeMessageHandler {

    @Resource
    private CollaborationAttendanceSwitchMapper collaborationAttendanceSwitchMapper;

    @Resource
    private CollaborationPostService collaborationPostService;

    @Resource
    private CachedImConfig cachedImConfig;

    @Resource
    private CollaborationAttendanceService collaborationAttendanceService;

    @Resource
    private CollaborationPostGroupService collaborationPostGroupService;

    @Resource
    private ImService imService;


    @Bean("userStateNoticeMessage")
    public Consumer<WsResponse> processWsMessage() {
        return wsResponse -> {
            try {
                if (!Objects.equals(wsResponse.getModule(), "addressbook")
                        || !Objects.equals(wsResponse.getNotifyType(), "userStateNotice")) {
                    return;
                }
                log.info("userStateNoticeMessage on message: {}", wsResponse);
                String signFlag = cachedImConfig.getConfig("FEATURE_COMPATIBILITY_SIGN");
                boolean isClosed = StringUtils.isBlank(signFlag) || (!"0".equals(signFlag));
                if (isClosed) {
                    log.info("userStateNoticeMessage isClosed");
                    return;
                }
                var message = JsonUtil.convert(wsResponse.getData(), WsUserStateNoticeMessage.class);
                processUserStateNoticeMsg(message);
            } catch (Exception e) {
                log.error("userStateNoticeMessage consume error", e);
            }
        };
    }

    private void processUserStateNoticeMsg(WsUserStateNoticeMessage message) {
        List<WsUserStateEvents> userStateEvents = message.getUserStateEvents();
        if (CollectionUtils.isEmpty(userStateEvents)) {
            return;
        }
        List<Long> userIdList = userStateEvents.stream().map(WsUserStateEvents::getUserId).collect(Collectors.toList());
        List<CollaborationAttendanceSwitch> switchList = collaborationAttendanceSwitchMapper.getByPersonIds(userIdList);
        Map<Long, CollaborationAttendanceSwitch> switchMap = switchList.stream().collect(Collectors.toMap(CollaborationAttendanceSwitch::getPersonId, o -> o));
        userStateEvents.stream().forEach(user -> {
            Long userId = user.getUserId();
            CollaborationAttendanceSwitch matchedSwitch = switchMap.get(userId);
            saveOrUpdateSwitch(userId, user.getState(), matchedSwitch);

            // 是否需要设置用户为离岗
            boolean need2Offline = need2OffLine(matchedSwitch, user.getState());
            if (!need2Offline) {
                return;
            }
            // 兼顾一个人可能关联多个协同岗的情况
            List<CollaborationPost> postList =
                    collaborationPostService.listByUserId(String.valueOf(userId));
            if (CollectionUtils.isEmpty(postList)) {
                log.warn("当前用户ID：{}，不是协同岗用户，跳过自动上下岗处理", user);
                return;
            }
            Integer flag = 1;
            log.info("收到IM人员状态变化通知，准备更新状态，flag: {}", flag);
            // 更新在线状态
            updateSwitch(userId, flag);
            // 群组支撑
            support(userId, flag);
            // 新增上下岗记录
            saveAttendanceRecord(postList, userId, flag);
            // 推送消息给客户端
            pushMsg(userId, flag);
        });
    }

    private boolean need2OffLine(CollaborationAttendanceSwitch matchedSwitch, Integer state) {
        // 有上下岗记录 && 当前上岗 && im离线
        return Objects.nonNull(matchedSwitch) && 0 == matchedSwitch.getSwitchStatus() && 0 == state;
    }

    /**
     * 是否需要设置用户为在岗
      */
    private boolean need2Online(CollaborationAttendanceSwitch matchedSwitch, Integer state) {
        // 有上下岗记录 && 当前下岗 && im在线 && 最后一次下岗是im状态离线修改的
        return Objects.nonNull(matchedSwitch)
                && 1 == matchedSwitch.getSwitchStatus()
                && 1 == state
                && Objects.nonNull(matchedSwitch.getSwitchType())
                && SwitchTypeEnum.IM_NOTIFY.getCode() == matchedSwitch.getSwitchType();
    }

    private void saveOrUpdateSwitch(Long userId, Integer imStatus, CollaborationAttendanceSwitch matchedSwitch) {
        if (Objects.isNull(matchedSwitch)) {
            collaborationAttendanceSwitchMapper.insert(CollaborationAttendanceSwitch.builder()
                    .personId(userId)
                    .switchStatus(1)
                    .imStatus(imStatus)
                    .switchType(SwitchTypeEnum.IM_NOTIFY.getCode())
                    .gmtCreated(new Date())
                    .updateTime(new Date())
                    .build());
        } else {
            UpdateWrapper<CollaborationAttendanceSwitch> wrapper = new UpdateWrapper<>();
            wrapper.eq("person_id", userId);
            matchedSwitch.setUpdateTime(new Date());
            matchedSwitch.setImStatus(imStatus);
            collaborationAttendanceSwitchMapper.update(matchedSwitch, wrapper);
        }
    }

    private void saveAttendanceRecord(List<CollaborationPost> postList, Long userId, Integer status) {
        ImUser userInfo = imService.findUserInfo(String.valueOf(userId));
        postList.forEach(post -> {
            CollaborationAttendance attendance = CollaborationAttendance.builder().orgId(post.getOrgId())
                    .orgName(post.getOrgName())
                    .postId(post.getId())
                    .postName(post.getPostName())
                    .personId(userId)
                    .personName(Objects.nonNull(userInfo) ? userInfo.getName() : "")
                    .switchType(SwitchTypeEnum.IM_NOTIFY.getCode())
                    .build();
            collaborationAttendanceService.saveRecord(attendance, status);
        });
    }

    private void support(Long userId, Integer status) {
        collaborationPostGroupService.changeUserStatus(status, userId);
    }

    private void pushMsg(Long userId, Integer status) {
        Map<String, String> sendMap = new HashMap<>();
        String type = status == 0 ? "上岗" : "下岗";
        sendMap.put("personId", String.valueOf(userId));
        sendMap.put("type", type);
        collaborationAttendanceService.sendSwitchStatusToCagent(sendMap);
    }

    private void updateSwitch(Long userId, Integer switchStatus) {
        var wrapper = Wrappers.lambdaUpdate(CollaborationAttendanceSwitch.class)
                .eq(CollaborationAttendanceSwitch::getPersonId, userId)
                .set(CollaborationAttendanceSwitch::getSwitchStatus, switchStatus)
                .set(CollaborationAttendanceSwitch::getSwitchType, SwitchTypeEnum.IM_NOTIFY.getCode())
                .set(CollaborationAttendanceSwitch::getUpdateTime, new Date());
        collaborationAttendanceSwitchMapper.update(null, wrapper);
    }

}
