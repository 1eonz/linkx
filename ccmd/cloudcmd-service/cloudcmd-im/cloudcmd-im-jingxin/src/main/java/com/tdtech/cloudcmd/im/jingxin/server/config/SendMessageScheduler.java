package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutySchedule;
import com.tdtech.cloudcmd.im.jingxin.server.entity.SystemMsg;
import com.tdtech.cloudcmd.im.jingxin.server.enums.NotifyTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.SwitchTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceSwitchMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostMapper;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.service.rpc.GroupMsgSendStaticRpcService;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import dto.GroupMsgSendStaticDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class SendMessageScheduler {


    @Resource
    private ImHttpClient imHttpClient;

    @DubboReference
    private GroupMsgSendStaticRpcService groupMsgSendStaticRpcService;

    @Resource
    private CollaborationPostMapper collaborationPostMapper;


    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @Resource
    private RedisUtil redisUtil;

    private static final String TARGETDATETIME = "cloudcmd:im:message:target:time";
    private static final String MESSAGESENDETIME = "cloudcmd:im:message:time";


    @Scheduled(initialDelay = 10000L, fixedDelay = 30L * 1000L)
    public void scheduled() {
        // 1. 获取配置周期
        String syncGroupMsgCountPeriod = globalsRpcService.getGlobalsValueByName("SYNC_GROUP_MSG_COUNT_PERIOD");
        log.info("获取到的同步参数周期为:{}", syncGroupMsgCountPeriod);

        // 2. 参数校验
        if ("-1".equals(syncGroupMsgCountPeriod)) {
            log.info("同步参数周期为-1，停止执行");
            return;
        }
        if (!isInteger(syncGroupMsgCountPeriod)) {
            log.warn("同步参数周期为:{}不是数字,请检查", syncGroupMsgCountPeriod);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        log.info("message scheduled now: {}", now);

        // 3. 获取Redis中的数据
        LocalDateTime targetDateTime = redisUtil.get(TARGETDATETIME, LocalDateTime.class);
        String oldsyncGroupMsgCountPeriod = redisUtil.get(MESSAGESENDETIME, String.class);

        // 4. 判断是否需要执行任务
        boolean shouldExecute = false;
        boolean isFirst = false;

        // 4.1 第一次执行（targetDateTime为null）
        if (targetDateTime == null || oldsyncGroupMsgCountPeriod == null) {
            log.info("第一次执行定时任务");
            shouldExecute = true;
            isFirst = true;
            targetDateTime = now.plusMinutes(Long.valueOf(syncGroupMsgCountPeriod));
        } else if (!syncGroupMsgCountPeriod.equals(oldsyncGroupMsgCountPeriod)) {
            // 4.2 周期改变
            log.info("周期从 {} 变为 {}，重新计算执行时间", oldsyncGroupMsgCountPeriod, syncGroupMsgCountPeriod);
            targetDateTime = now.plusMinutes(Long.valueOf(syncGroupMsgCountPeriod));
        } else if (now.isAfter(targetDateTime) || now.isEqual(targetDateTime)) {
            // 4.3 周期不变，判断是否到达执行时间
            log.info("到达执行时间: {}", targetDateTime);
            shouldExecute = true;
            targetDateTime = targetDateTime.plusMinutes(Long.valueOf(syncGroupMsgCountPeriod));
        }

        // 5. 更新Redis数据
        redisUtil.set(TARGETDATETIME, targetDateTime);
        redisUtil.set(MESSAGESENDETIME, syncGroupMsgCountPeriod);

        // 6. 执行任务
        if (shouldExecute) {
            log.info("开始执行统计消息数任务，下次执行时间: {}", targetDateTime);
            extracted(isFirst);
        }
    }

    private void extracted(boolean isFirst) {
        List<CollaborationPost> collaborationPosts = collaborationPostMapper.selectList(new LambdaQueryWrapper<>());
        if (CollectionUtils.isEmpty(collaborationPosts)) {
            log.info("协同岗数据为空");
            return;
        }

        List<String> relateUserIds = collaborationPosts.stream().map(CollaborationPost::getRelatedUserIds).collect(Collectors.toList());
        Map<String, Long> userDepartMentMap = collaborationPosts.stream().collect(
                Collectors.toMap(CollaborationPost::getRelatedUserIds, CollaborationPost::getOrgId, (k1, k2) -> k1));
        Set<String> userIdSet = relateUserIds.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .collect(Collectors.toSet());
        log.info("userIdSet数量:{},开始同步消息数", userIdSet.size());
        userIdSet.forEach(userId -> {
            // 先查询上一次消息发送的最大时间
            try {
                Long departmentId = getDepartMentByUserId(userId, userDepartMentMap);
                long userIdLong = Long.parseLong(userId);
                GroupMsgSendStaticDto userMessage = groupMsgSendStaticRpcService.getLatestByUserId(userIdLong);

                List<GroupMsgSendStaticDto> allGroupMsgSendStaticDtos = new ArrayList<>();

                if (userMessage != null && userMessage.getMsgTime() != null) {
                    // 有上次记录，从上次时间开始查询
                    Date beginTime = userMessage.getMsgTime();
                    IMSendbyMsgReq imendbyMsgReq = new IMSendbyMsgReq();
                    imendbyMsgReq.setUserId(userIdLong);
                    imendbyMsgReq.setBeginTime(beginTime);
                    List<IMSendbyPageMsgVo.IMSendbyMsgVo> sendMessageList = getSendMessage(imendbyMsgReq);

                    if (!CollectionUtils.isEmpty(sendMessageList)) {
                        allGroupMsgSendStaticDtos = convertList(sendMessageList, userIdLong, departmentId);
                    }
                } else {
                    // 没有上次记录
                    if (isFirst) {
                        // 第一次执行，从2025-01-01 00:00:00开始查询，每3个月查一次
                        log.info("userId为:{}第一次执行，从2025-01-01开始查询", userId);
                        allGroupMsgSendStaticDtos = queryMessagesByQuarter(userIdLong, departmentId);
                    } else {
                        // 非第一次执行，不传开始时间和结束时间
                        log.info("userId为:{}没有历史记录且非第一次执行，不传时间参数查询", userId);
                        IMSendbyMsgReq imendbyMsgReq = new IMSendbyMsgReq();
                        imendbyMsgReq.setUserId(userIdLong);
                        // 不设置 beginTime 和 endTime
                        List<IMSendbyPageMsgVo.IMSendbyMsgVo> sendMessageList = getSendMessage(imendbyMsgReq);

                        if (!CollectionUtils.isEmpty(sendMessageList)) {
                            allGroupMsgSendStaticDtos = convertList(sendMessageList, userIdLong, departmentId);
                        }
                    }
                }

                if (CollectionUtils.isEmpty(allGroupMsgSendStaticDtos)) {
                    log.info("userId为:{}的消息数为空,无需同步", userId);
                } else {
                    log.info("userId为:{}共查询到{}条消息，开始分批插入", userId, allGroupMsgSendStaticDtos.size());
                    // 分批处理，每1000条调用一次接口
                    batchInsertMessages(allGroupMsgSendStaticDtos);
                }
            } catch (Exception e) {
                log.error("userId为:{}的消息同步失败", userId, e);
            }
        });
    }

    private void batchInsertMessages(List<GroupMsgSendStaticDto> allGroupMsgSendStaticDtos) {
        Lists.partition(allGroupMsgSendStaticDtos, 1000).forEach(groupMsgSendStaticDtos -> {
            groupMsgSendStaticRpcService.batchCreateGroupMsgSendStaticWithFilter(groupMsgSendStaticDtos);
        });
    }

    /**
     * 从2025-01-01 00:00:00开始查询，每3个月查一次
     */
    private List<GroupMsgSendStaticDto> queryMessagesByQuarter(Long userId, Long departmentId) {
        List<GroupMsgSendStaticDto> allMessages = new ArrayList<>();

        // 开始时间：2025-01-01 00:00:00
        LocalDateTime startDateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime now = LocalDateTime.now();

        // 循环查询，每次查询3个月
        LocalDateTime currentStart = startDateTime;
        int queryCount = 0;

        while (currentStart.isBefore(now)) {
            // 计算本次查询的结束时间（开始时间 + 3个月）
            LocalDateTime currentEnd = currentStart.plus(3, ChronoUnit.MONTHS);

            // 如果结束时间超过当前时间，则使用当前时间
            if (currentEnd.isAfter(now)) {
                currentEnd = now;
            }

            queryCount++;
            log.info("userId为:{}第{}次查询，时间范围: {} 到 {}", userId, queryCount, currentStart, currentEnd);

            // 查询消息
            Date beginTime = Date.from(currentStart.atZone(ZoneId.systemDefault()).toInstant());
            Date endTime = Date.from(currentEnd.atZone(ZoneId.systemDefault()).toInstant());

            IMSendbyMsgReq imSendbyMsgReq = new IMSendbyMsgReq();
            imSendbyMsgReq.setUserId(userId);
            imSendbyMsgReq.setBeginTime(beginTime);
            imSendbyMsgReq.setEndTime(endTime);

            List<IMSendbyPageMsgVo.IMSendbyMsgVo> messageList = getSendMessage(imSendbyMsgReq);

            if (!CollectionUtils.isEmpty(messageList)) {
                log.info("userId为:{}第{}次查询到{}条消息", userId, queryCount, messageList.size());
                allMessages.addAll(convertList(messageList, userId, departmentId));
            }

            // 移动到下一个时间段
            currentStart = currentEnd;
        }

        log.info("userId为:{}共执行{}次查询，总计{}条消息", userId, queryCount, allMessages.size());
        return allMessages;
    }


    private Long getDepartMentByUserId(String userId, Map<String, Long> userDepartMentMap) {
        return userDepartMentMap.entrySet().stream()
                .filter(entry -> {
                    String[] userIds = entry.getKey().split(",");
                    return Arrays.stream(userIds)
                            .map(String::trim)
                            .anyMatch(id -> id.equals(userId.trim()));
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }


    private List<GroupMsgSendStaticDto> convertList(List<IMSendbyPageMsgVo.IMSendbyMsgVo> sendMessageList,
                                                    Long userId, Long departmentId) {
        return sendMessageList.stream().map(item -> {
            GroupMsgSendStaticDto groupMsgSendStaticDto = new GroupMsgSendStaticDto();
            groupMsgSendStaticDto.setMsgId(item.getMsgId());
            groupMsgSendStaticDto.setMsgTime(item.getTime());
            groupMsgSendStaticDto.setUserId(userId);
            groupMsgSendStaticDto.setSessionId(item.getSessionId());
            groupMsgSendStaticDto.setCategory(item.getCategory());
            groupMsgSendStaticDto.setMsgType(item.getMsgType());
            groupMsgSendStaticDto.setGmtCreated(new Date());
            groupMsgSendStaticDto.setDepartmentId(departmentId);
            return groupMsgSendStaticDto;
        }).collect(Collectors.toList());
    }

    private List<IMSendbyPageMsgVo.IMSendbyMsgVo> getSendMessage(IMSendbyMsgReq imendbyMsgReq) {
        try {
            IMSendbyPageMsgVo sendMessage = imHttpClient.getSendMessage(imendbyMsgReq);
            return sendMessage.getImMsgs();
        } catch (Exception e) {
            log.info("userId为:{}的消息同步失败", imendbyMsgReq.getUserId());
            return null;
        }
    }

    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
