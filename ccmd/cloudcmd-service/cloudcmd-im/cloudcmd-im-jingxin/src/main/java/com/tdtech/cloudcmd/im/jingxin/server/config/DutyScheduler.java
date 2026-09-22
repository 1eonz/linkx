package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImMessageRequest;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.TxtMsgVo;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.group.GroupAiClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.CollaborationTaskDelayDto;
import com.tdtech.cloudcmd.im.jingxin.server.enums.Constant;
import com.tdtech.cloudcmd.im.jingxin.server.enums.NotifyTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.SwitchTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceSwitchMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class DutyScheduler {

    // 检查频率，单位分钟，要和下面的定时任务保持一致
    private static final Long SCHEDULE_RATE = 1L;

    @Resource
    private IDutyScheduleService dutyScheduleService;

    @Resource
    private CachedImConfig cachedImConfig;

    @Resource
    private CollaborationPostGroupService collaborationPostGroupService;

    @Resource
    private ImService imService;

    @Resource
    private CollaborationPostService collaborationPostService;

    @Resource
    private CollaborationAttendanceService collaborationAttendanceService;

    @Resource
    private GroupAiClient groupAiClient;

    @Resource
    private ISystemMsgService systemMsgService;

    @Resource
    private IdWorker idWorker;

    @Resource
    private CollaborationAttendanceSwitchMapper collaborationAttendanceSwitchMapper;
    
    @Resource
    private DutyTypeService dutyTypeService;

    @Autowired
    private ICollaborationTaskService taskService;

    @Scheduled(fixedRate = 1L * 60L * 1000L)
    public void scheduled() {
        LocalDateTime now = LocalDateTime.now();
        String enableFlag = cachedImConfig.getConfig("DUTY_SCHEDULE_ENABLE");
        boolean isClosed = StringUtils.isBlank(enableFlag) || (!"1".equals(enableFlag));
        if (isClosed) {
            return;
        }
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        // 查询当前时间正在值班的排班（支持跨天值班）
        List<DutySchedule> list = findActiveDutySchedules(today, currentTime);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<DutyType> dutyTypes = dutyTypeService.getAllList();
        List<Long> userIdList = list.stream().map(DutySchedule::getUserId).collect(Collectors.toList());
        Map<Long, CollaborationAttendanceSwitch> userMap = collaborationAttendanceSwitchMapper.getByPersonIds(userIdList)
                .stream().collect(Collectors.toMap(CollaborationAttendanceSwitch::getPersonId, o -> o));
        list.stream().forEach(dutySchedule -> {
            // 兼顾一个人可能关联多个协同岗的情况
            List<CollaborationPost> postList =
                    collaborationPostService.listByUserId(String.valueOf(dutySchedule.getUserId()));
            if (CollectionUtils.isEmpty(postList)) {
                log.warn("用户: {}, ID: {}, 不是协同岗，跳过不处理", dutySchedule.getUserName(), dutySchedule.getUserId());
                return;
            }
            CollaborationAttendanceSwitch matchedSwitch = userMap.get(dutySchedule.getUserId());

            Long dutyTypeId = dutySchedule.getDutyType();
            DutyType matchedDutyType = dutyTypes.stream()
                    .filter(it -> Objects.equals(it.getType(), dutyTypeId))
                    .findFirst().orElse(null);
            String dutyTypeName = Optional.ofNullable(matchedDutyType).map(DutyType::getName).orElse(null);
            // 是否需要发送通知
            boolean isTime2SendMsg = isTime2SendMsg(dutySchedule, now);
            if (isTime2SendMsg) {
                // 发送im消息
                sendReady2WorkMsg(dutySchedule, dutyTypeName);
            }
            // 排班计划结束时，所有类型都发送排版计划结束通知
            boolean isOverDuty = isOverDuty(dutySchedule, now);
            if (isOverDuty) {
                sendOfflineMsg(dutySchedule, dutyTypeName);
            }
            //se:只有值班类型为自动上下岗时，才需要处理自动上下岗逻辑,默认类型不处理（存量数据用户需要此功能就重新导入）
            boolean isAutoDutyType = Objects.equals(dutyTypeId, Constant.AUTO_DUTY_TYPE_ID)
                    || (matchedDutyType != null && Constant.AUTO_DUTY_TYPE_NAME.equals(matchedDutyType.getName()));
            if (!isAutoDutyType) {
                return;
            }
            // 是否需要自动上岗 = 到了排班时间范围 && 用户未上岗
            boolean isTime2OnDuty = isTime2OnDuty(dutySchedule, now);
            boolean needAutoOnline = isTime2OnDuty && isNotOnDuty(matchedSwitch, dutySchedule);
            if (needAutoOnline) {
                handleBusiness(matchedSwitch, 0);
            } else {
                if (isTime2OnDuty) {
                    // 到了排班时间但是未上岗，具体原因
                    log.info("到了排班时间但是未上岗参数：matchedSwitch：{}", matchedSwitch);
                }
            }

            // 是否需要自动下岗 = 超过值班结束时间 && 用户在岗
            boolean needAutoOffline = isOverDuty && isOnDuty(matchedSwitch, dutySchedule);
            if (needAutoOffline) {
                handleBusiness(matchedSwitch, 1);
            }
        });

    }

    /**
     * 查询当前需要处理的排班列表（支持跨天值班）
     * <p>
     * 包含三种场景的排班：
     * 1. 即将开始的排班（用于提前发送通知）
     * 2. 正在进行的排班（用于自动上岗）
     * 3. 刚刚结束的排班（用于自动下岗）
     * <p>
     * 各场景的具体时间判断由 isTime2SendMsg、isTime2OnDuty、isOverDuty 自行完成。
     */
    private List<DutySchedule> findActiveDutySchedules(LocalDate today, LocalTime currentTime) {
        List<DutySchedule> allList = dutyScheduleService.findListByDateRange(today, today);
        if (CollectionUtils.isEmpty(allList)) {
            return Collections.emptyList();
        }
        //se代码 过滤条件过于严格，导致通知和自动下岗功能失效
        // 过滤出当前时间正在值班的
//        return allList.stream()
//                .filter(duty -> {
//                    LocalDateTime dutyStart = LocalDateTime.of(duty.getDutyStartDate(), duty.getDutyStartTime());
//                    LocalDateTime dutyEnd = LocalDateTime.of(duty.getDutyEndDate(), duty.getDutyEndTime());
//                    LocalDateTime now = LocalDateTime.of(today, currentTime);
//                    // 当前时间在值班时间范围内（半开区间：开始<=当前<结束）
//                    return !now.isBefore(dutyStart) && now.isBefore(dutyEnd);
//                })
//                .collect(Collectors.toList());
        LocalDateTime now = LocalDateTime.of(today, currentTime);
        return allList.stream()
                .filter(duty -> {
                    LocalDateTime dutyEnd = LocalDateTime.of(duty.getDutyEndDate(), duty.getDutyEndTime());
                    return now.isBefore(dutyEnd.plusMinutes(SCHEDULE_RATE * 2));
                })
                .collect(Collectors.toList());
    }


    private String buildDutyTypeNameMsg(String dutyTypeName) {
        if (StringUtils.isBlank(dutyTypeName)) {
            return "";
        }
        return "排班类型为" + dutyTypeName + "的";
    }

    /**
     * 发送值班提醒消息
     * <p>
     * 在值班开始前，向用户发送提醒消息，告知即将开始的排班计划。
     * 消息格式：您在{开始日期} {开始时间}至{结束日期} {结束时间}有协同岗支撑排班计划，请关注上岗时间！
     *
     * @param dutySchedule 值班排班信息
     */
    private void sendReady2WorkMsg(DutySchedule dutySchedule, String dutyTypeName) {
        // 构建提醒消息文本
        String text = String.format("您在%s %s至%s %s有%s协同岗支撑排班计划，请关注上岗时间！",
            dutySchedule.getDutyStartDate(), dutySchedule.getDutyStartTime(),
            dutySchedule.getDutyEndDate(), dutySchedule.getDutyEndTime(), buildDutyTypeNameMsg(dutyTypeName));
        ImMessageRequest<TxtMsgVo> msgRequest = buildIMMsgReq(text, String.valueOf(dutySchedule.getUserId()));
        String msgId = sendIMMsg(msgRequest);

        // 保存消息记录，用于防止重复发送
        saveSystemMsgRecord(msgRequest, dutySchedule.getId(), msgId);
    }

    /**
     * 发送自动下岗通知消息
     * <p>
     * 当值班时间结束，系统自动执行下岗操作后，向用户发送通知消息。
     * 消息格式：您在{开始日期} {开始时间}至{结束日期} {结束时间}协同岗支撑排班计划已结束
     *
     * @param dutySchedule 值班排班信息
     */
    private void sendOfflineMsg(DutySchedule dutySchedule, String dutyTypeName) {
        // 构建下岗通知消息文本
        String text = String.format("您在%s %s至%s %s%s协同岗支撑排班计划已结束",
            dutySchedule.getDutyStartDate(), dutySchedule.getDutyStartTime(),
            dutySchedule.getDutyEndDate(), dutySchedule.getDutyEndTime(), buildDutyTypeNameMsg(dutyTypeName));
        ImMessageRequest<TxtMsgVo> msgRequest = buildIMMsgReq(text, String.valueOf(dutySchedule.getUserId()));
        String msgId = sendIMMsg(msgRequest);

        // 保存消息记录
        saveSystemMsgRecord(msgRequest, dutySchedule.getId(), msgId);
    }

    /**
     * 处理上岗/下岗业务逻辑
     * <p>
     * 执行以下操作：
     * 1. 更新用户上岗状态标记
     * 2. 更新群组支撑状态
     * 3. 保存上岗/下岗记录
     * 4. 如果是上岗操作，自动认领未分配的任务
     * 5. 推送状态变更消息给客户端
     *
     * @param attendanceSwitch 用户的考勤状态信息
     * @param status           操作状态：0-上岗，1-下岗
     */
    private void handleBusiness(CollaborationAttendanceSwitch attendanceSwitch, Integer status) {
        Long userId = attendanceSwitch.getPersonId();
        // 获取用户关联的协同岗列表（一个人可能关联多个协同岗）
        List<CollaborationPost> postList =
                collaborationPostService.listByUserId(String.valueOf(userId));
        if (CollectionUtils.isEmpty(postList)) {
            log.info("handleBusiness 人员没有关联协同岗: {}", attendanceSwitch);
            return;
        }
        // 1. 更新用户上岗状态标记
        updateSwitch(attendanceSwitch, status);
        // 2. 更新群组支撑状态
        updateSupport(userId, status);
        // 3. 保存上岗/下岗记录
        saveAttendanceRecord(userId, status, postList);

        // 4. 上岗时自动认领未分配的任务
        if (status == 0) {
            postList.forEach(post -> {
                List<CollaborationTaskDelayDto> delayDtos = taskService.dealUnassignTask(post.getId(), userId);
                taskService.pushToDealyQueue(delayDtos);
            });
        }

        // 5. 推送状态变更消息给客户端
        pushMsg(userId, status);
    }


    /**
     * 推送状态变更消息给客户端
     * <p>
     * 通过WebSocket将上岗/下岗状态推送给客户端，实现多端同步。
     *
     * @param userId 用户ID
     * @param status 操作状态：0-上岗，1-下岗
     */
    private void pushMsg(Long userId, Integer status) {
        String type = status == 0 ? "上岗" : "下岗";
        // 构建推送消息并通过WebSocket发送
        Map<String, String> sendMap = new HashMap<>();
        sendMap.put("personId", String.valueOf(userId));
        sendMap.put("type", type);
        collaborationAttendanceService.sendSwitchStatusToCagent(sendMap);
    }

    /**
     * 保存上岗/下岗记录
     * <p>
     * 为用户关联的每个协同岗创建一条上岗/下岗记录，记录到考勤表中。
     * 记录包含：组织信息、协同岗信息、人员信息、操作类型（自动操作）。
     *
     * @param userId   用户ID
     * @param status   操作状态：0-上岗，1-下岗
     * @param postList 用户关联的协同岗列表
     */
    private void saveAttendanceRecord(Long userId, Integer status, List<CollaborationPost> postList) {
        // 获取用户信息
        ImUser userInfo = imService.findUserInfo(String.valueOf(userId));
        // 为每个协同岗创建考勤记录
        postList.forEach(post -> {
            CollaborationAttendance attendance = CollaborationAttendance.builder().orgId(post.getOrgId())
                    .orgName(post.getOrgName())
                    .postId(post.getId())
                    .postName(post.getPostName())
                    .personId(userId)
                    .personName(Objects.nonNull(userInfo) ? userInfo.getName() : "")
                    .switchType(SwitchTypeEnum.DUTY_AUTO.getCode())
                    .build();
            collaborationAttendanceService.saveRecord(attendance, status);
        });
    }

    /**
     * 更新用户在群组中的支撑状态
     *
     * @param userId 用户ID
     * @param status 状态：0-上岗（可支撑），1-下岗（不可支撑）
     */
    private void updateSupport(Long userId, Integer status) {
        collaborationPostGroupService.changeUserStatus(status, userId);
    }

    /**
     * 更新用户上岗状态标记
     * <p>
     * 更新数据库中的上岗状态记录，包括：
     * - switchStatus：上岗状态（0-在岗，1-下岗）
     * - updateTime：更新时间
     * - switchType：操作类型（标记为自动操作）
     *
     * @param attendanceSwitch 用户的考勤状态信息
     * @param status           状态：0-上岗，1-下岗
     */
    private void updateSwitch(CollaborationAttendanceSwitch attendanceSwitch, Integer status) {
        attendanceSwitch.setSwitchStatus(status);
        attendanceSwitch.setUpdateTime(new Date());
        attendanceSwitch.setSwitchType(SwitchTypeEnum.DUTY_AUTO.getCode());
        collaborationAttendanceSwitchMapper.updateById(attendanceSwitch);
    }

    /**
     * 判断用户是否处于”未上岗”状态，可以执行自动上岗操作
     * <p>
     * 需同时满足以下三个条件：
     * 1. IM在线：用户的IM状态为在线（imStatus = 1）
     * 2. 当前下岗：用户的上岗状态为”下岗”（switchStatus = 1）
     * 3. 无手动操作：在当前值班时间区间内没有手动上下岗操作记录
     * <p>
     * 第3个条件的目的是：如果用户在值班时间内手动操作过上下岗，
     * 则尊重用户的意愿，不执行自动上岗，避免覆盖用户的手动操作。
     *
     * @param attendanceSwitch 用户的考勤状态信息，包含IM状态、上岗状态、最后手动操作时间等
     * @param dutySchedule     值班排班信息，用于确定值班时间区间
     * @return true-用户处于未上岗状态，可以执行自动上岗；false-不满足自动上岗条件
     */
    private boolean isNotOnDuty(CollaborationAttendanceSwitch attendanceSwitch, DutySchedule dutySchedule) {
        // 条件1：IM在线（imStatus = 1 表示在线）
        boolean imStatusIsOnline = Objects.nonNull(attendanceSwitch) && Objects.nonNull(attendanceSwitch.getImStatus())
                && 1 == attendanceSwitch.getImStatus();
        // 条件2：当前是下岗状态（switchStatus = 1 表示下岗，0 表示在岗）
        boolean isOffline = Objects.nonNull(attendanceSwitch) && 1 == attendanceSwitch.getSwitchStatus();

        // 条件3：该值班区间没有手动上下岗操作（尊重用户手动操作意愿）
        return imStatusIsOnline && isOffline && (!hasRecordInRange(attendanceSwitch, dutySchedule));
    }

    /**
     * 判断在指定时间区间内是否有手动上下岗操作记录
     * <p>
     * 通过检查 lastManualOperationTime（最后手动操作时间）和 switchType（操作类型）来判断：
     * - 手动操作时间必须在值班时间区间内
     * - 操作类型必须是用户点击或管理员点击（非自动操作）
     * <p>
     * 使用专用字段 lastManualOperationTime 而非 updateTime，避免被 IM 状态变化等
     * 非手动操作更新的 updateTime 干扰判断。
     *
     * @param attendanceSwitch   用户的考勤状态信息
     * @param dutyStartDateTime  值班开始时间
     * @param dutyEndDateTime    值班结束时间
     * @return true-在区间内有手动操作记录；false-无手动操作记录
     */
    private static boolean hasRecordInRange(CollaborationAttendanceSwitch attendanceSwitch, LocalDateTime dutyStartDateTime, LocalDateTime dutyEndDateTime) {
        // 获取最后手动操作时间（专用字段，不受IM状态变化影响）
        Date manualOpTime = attendanceSwitch.getLastManualOperationTime();
        if (manualOpTime == null) {
            return false;
        }
        // 将 Date 转换为 LocalDateTime
        LocalDateTime manualOpDateTime = manualOpTime.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        // 判断手动操作时间是否在值班时间区间内（闭区间）
        boolean inRange = !manualOpDateTime.isBefore(dutyStartDateTime)
                && !manualOpDateTime.isAfter(dutyEndDateTime);

        // 必须同时满足：时间在区间内 且 操作类型为手动操作（用户点击或管理员点击）
        return inRange &&
                (SwitchTypeEnum.USER_CLICK.getCode() == attendanceSwitch.getSwitchType()
                || SwitchTypeEnum.ADMIN_CLICK.getCode() == attendanceSwitch.getSwitchType());
    }

    /**
     * 判断在值班时间区间内是否有手动上下岗操作记录（重载方法）
     * <p>
     * 从 DutySchedule 对象中提取值班开始和结束时间，调用 hasRecordInRange 方法。
     * 支持跨天值班场景。
     *
     * @param attendanceSwitch 用户的考勤状态信息
     * @param dutySchedule     值班排班信息
     * @return true-在区间内有手动操作记录；false-无手动操作记录
     */
    private static boolean hasRecordInRange(CollaborationAttendanceSwitch attendanceSwitch, DutySchedule dutySchedule) {
        // 从排班信息中构建值班时间区间（支持跨天值班）
        LocalDateTime dutyStartDateTime = LocalDateTime.of(dutySchedule.getDutyStartDate(), dutySchedule.getDutyStartTime());
        LocalDateTime dutyEndDateTime = LocalDateTime.of(dutySchedule.getDutyEndDate(), dutySchedule.getDutyEndTime());
        return hasRecordInRange(attendanceSwitch, dutyStartDateTime, dutyEndDateTime);
    }

    /**
     * 判断用户是否已经上岗
     * <p>
     * 只要用户的上岗状态为"在岗"（switchStatus = 0），即认为已上岗。
     * 不检查是否有手动操作记录，因为下岗操作需要覆盖任何状态。
     *
     * @param attendanceSwitch 用户的考勤状态信息
     * @param dutySchedule     值班排班信息（当前未使用，保留用于扩展）
     * @return true-用户已上岗；false-用户未上岗
     */
    private boolean isOnDuty(CollaborationAttendanceSwitch attendanceSwitch, DutySchedule dutySchedule) {
//        LocalDate dutyDate = dutySchedule.getDutyDate();
//        LocalTime dutyStartTime = dutySchedule.getDutyStartTime();
//        // 多加一个检查的周期，在值班结束日期之后，如果用户手动切换了状态，以手工切换为主
//        LocalTime dutyEndTime = dutySchedule.getDutyEndTime().plusMinutes(SCHEDULE_RATE);
//        LocalDateTime dutyStartDateTime = LocalDateTime.of(dutyDate, dutyStartTime);
//        LocalDateTime dutyEndDateTime = LocalDateTime.of(dutyDate, dutyEndTime);
        // 人员在岗 在岗就下岗，不管有没有手动切换过
        return Objects.nonNull(attendanceSwitch)
                && 0 == attendanceSwitch.getSwitchStatus();
                // 值班开始时间到当前没有有手动上下岗操作
//                && (!hasRecordInRange(attendanceSwitch, dutyStartDateTime, dutyEndDateTime));
    }

    /**
     * 判断是否需要发送值班提醒消息
     * <p>
     * 分两种情况：
     * 1. 特殊情况：排班开始时间距离当前时间不足配置的检测间隔，且未发送过提醒，
     *    则立即发送提醒（只发一次）
     * 2. 正常情况：按照配置的周期和次数，在排班开始前提前发送提醒
     * <p>
     * 配置项：
     * - DUTY_SCHEDULE_NOTICE_PERIOD：提醒周期，单位分钟，默认180分钟
     * - DUTY_SCHEDULE_NOTICE_COUNT：提醒次数，默认2次
     *
     * @param dutySchedule 值班排班信息
     * @return true-需要发送提醒；false-不需要发送提醒
     */
    private boolean isTime2SendMsg(DutySchedule dutySchedule, LocalDateTime now) {
//        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dutyStart = LocalDateTime.of(dutySchedule.getDutyStartDate(), dutySchedule.getDutyStartTime());
        if (now.isAfter(dutyStart)) {
            return false;
        }
        // 间隔时间，单位分钟
        String period = cachedImConfig.getConfig("DUTY_SCHEDULE_NOTICE_PERIOD");
        Integer periodInt = StringUtils.isNoneBlank(period) ? Integer.parseInt(period) : 180;
        // 需要发送通知的次数
        String noticeCount = cachedImConfig.getConfig("DUTY_SCHEDULE_NOTICE_COUNT");
        Integer noticeCountInt = StringUtils.isNoneBlank(noticeCount) ? Integer.parseInt(noticeCount) : 2;

        // 正常导入的数据的情况（提前比较久）
        boolean isTime2SendMsgForNormal = isTime2SendMsgForNormal(dutySchedule, dutyStart, periodInt, noticeCountInt, now);
        if (isTime2SendMsgForNormal) {
            return true;
        }

        // 导入的排班开始时间到当前时间，如果不足指定的时间间隔，又没发送过提醒，则立马提醒（只提醒一次）
        return isTime2SendMsgForSpecial(dutySchedule, periodInt, now);
    }

    /**
     * 正常情况下的提醒判断（排班开始时间距离当前时间较远）
     * <p>
     * 按照配置的周期和次数，在排班开始前提前发送提醒。
     * 例如：排班12:00开始，周期5分钟，发送2次，则在11:50发第1次，11:55发第2次。
     * <p>
     * 计算方式：从排班开始时间往前推，每隔一个周期检查是否需要发送提醒。
     *
     * @param dutySchedule 排班数据
     * @param dutyStartTime 排班开始时间
     * @param period 间隔时间，单位分钟
     * @param noticeCount 需要发送通知的次数
     * @return 是否需要发送通知
     */
    private boolean isTime2SendMsgForNormal(DutySchedule dutySchedule, LocalDateTime dutyStartTime, Integer period, Integer noticeCount, LocalDateTime now) {
//        // 从排班开始时间往前推，检查每个提醒时间点
//        // 例如：12:00开始，周期5分钟，发送2次，则在11:50发1次，11:55发1次
//        for (int i = noticeCount; i > 0; i--) {
//            LocalDateTime sendTime = dutyStartTime.minusMinutes(i * period);
//            boolean isTime = sameDateTime(sendTime, now);
//            if (isTime) {
//                return true;
//    private boolean isTime2SendMsgForNormal(DutySchedule dutySchedule, LocalTime dutyStartTime, Integer period, Integer noticeCount, LocalTime now) {
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        // eg: 12:00开始，周期5分钟，发送2次，则在11:50~11:55发1次，11:55~12:00发1次
        // 极端情况下，定时任务开始于11:49:59，当本次周期任务执行完成时，时间为11:50，下次任务执行时间为11:51，按之前的逻辑，不会在11:50发送通知
        // 所以这里改为是否在11:50~11:55之间，如果在，且该周期未发送过通知，则直接发送通知
        for (long i = 1; i <= noticeCount; i++) {
            LocalDateTime sendTime = dutyStartTime.minusMinutes(i * period);
            if (sendTime.isBefore(now)) {
                startTime = sendTime;
                endTime = sendTime.plusMinutes(period);
                break;
            }
        }

        if (startTime == null || endTime == null) {
            return false;
        }
        Date startDate = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant());

        Long count = systemMsgService.count(NotifyTypeEnum.ON_DUTY_PERIOD.getCode(), dutySchedule.getId(), startDate, endDate);
        log.info("排班通知周期: {}, 本次通知周期开始时间: {}, 本次通知周期结束时间: {}, 本次已发送通知次数: {}", period, startDate, endDate, count);
        return Objects.isNull(count) || count.equals(0L);
    }

    /**
     * 特殊情况下的提醒判断（排班开始时间距离当前时间较近）
     * <p>
     * 当排班开始时间距离当前时间不足配置的检测间隔时，如果还未发送过提醒，
     * 则立即发送提醒（只发送一次）。
     * <p>
     * 这种情况通常发生在：临时导入排班数据，距离开始时间很近。
     *
     * @param dutySchedule 值班排班信息
     * @param period       提醒周期，单位分钟（用于判断是否属于特殊情况）
     * @return true-需要立即发送提醒；false-不需要
     */
    private boolean isTime2SendMsgForSpecial(DutySchedule dutySchedule, Integer period, LocalDateTime now) {
//        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dutyStart = LocalDateTime.of(dutySchedule.getDutyStartDate(), dutySchedule.getDutyStartTime());
        // 计算距离排班开始还有多少分钟
        long minutesUntil = now.until(dutyStart, ChronoUnit.MINUTES);
        // 判断是否属于特殊情况：距离开始时间不足配置周期，且还未开始
        boolean matched = minutesUntil < period && minutesUntil > 0;
//    private boolean isTime2SendMsgForSpecial(DutySchedule dutySchedule, Integer period, LocalTime now) {
//        LocalTime dutyStartTime = dutySchedule.getDutyStartTime();
//        long minutesUntil = now.until(dutyStartTime, ChronoUnit.MINUTES);
//        boolean matched = minutesUntil < period && minutesUntil >= 0;
        if (!matched) {
            // 不属于特殊情况，由正常逻辑处理
            return false;
        }
        // 检查是否已经发送过提醒（通过系统消息记录判断）
        Long count = systemMsgService.count(NotifyTypeEnum.ON_DUTY_PERIOD.getCode(), dutySchedule.getId());
        // 只有未发送过提醒才需要发送
//        return Objects.isNull(count) || count.longValue() == 0;
        return Objects.isNull(count) || count.equals(0L);
    }

    /**
     * 判断两个时间是否在同一分钟（忽略秒和纳秒）
     * <p>
     * 用于判断当前时间是否匹配提醒时间点。
     *
     * @param a 时间a
     * @param b 时间b
     * @return true-两个时间在同一分钟；false-不在同一分钟
     */
    public static boolean sameDateTime(LocalDateTime a, LocalDateTime b) {
        return a.truncatedTo(ChronoUnit.MINUTES)
                .equals(b.truncatedTo(ChronoUnit.MINUTES));
    }

    /**
     * 判断当前时间是否在值班时间范围内（是否需要上岗）
     * <p>
     * 使用半开区间判断：值班开始时间 <= 当前时间 < 值班结束时间。
     * 支持跨天值班场景。
     *
     * @param dutySchedule 值班排班信息
     * @param now          当前时间
     * @return true-当前时间在值班时间范围内，需要上岗；false-不在范围内
     */
    private boolean isTime2OnDuty(DutySchedule dutySchedule, LocalDateTime now) {
        LocalDateTime dutyStart = LocalDateTime.of(dutySchedule.getDutyStartDate(), dutySchedule.getDutyStartTime());
        LocalDateTime dutyEnd = LocalDateTime.of(dutySchedule.getDutyEndDate(), dutySchedule.getDutyEndTime());
        // 半开区间判断：开始 <= 当前 < 结束
        return !now.isBefore(dutyStart) && now.isBefore(dutyEnd);
    }

    /**
     * 判断是否已经超过值班结束时间（需要自动下岗）
     * <p>
     * 当当前时间超过值班结束时间，且超过的时间正好等于检测周期时，
     * 触发自动下岗。这样设计是为了避免重复处理：只在实际结束后的第一个检测周期内处理一次。
     * <p>
     * 例如：检测周期为1分钟，值班18:00结束，则在18:01检测时触发自动下岗。
     *
     * @param dutySchedule 值班排班信息
     * @param now          当前时间
     * @return true-已超过值班结束时间，需要自动下岗；false-未超过或已处理过
     */
    private boolean isOverDuty(DutySchedule dutySchedule, LocalDateTime now) {
        LocalDateTime dutyEndTime = LocalDateTime.of(dutySchedule.getDutyEndDate(), dutySchedule.getDutyEndTime());
        // 下岗就检查这个周期的，超过太久时间的不处理了，防止造成重复处理
        Duration between = Duration.between(dutyEndTime, now);
        // 加入容错，允许在 SCHEDULE_RATE 分钟内触发，避免在排班结束时间的误差范围内触发
        return between.toMinutes() >= SCHEDULE_RATE && between.toMinutes() < SCHEDULE_RATE * 2;
//    private boolean isOverDuty(DutySchedule dutySchedule, LocalDateTime now) {
//        LocalDateTime dutyEnd = LocalDateTime.of(dutySchedule.getDutyEndDate(), dutySchedule.getDutyEndTime());
//        // 只在结束时间后的第一个检测周期内触发，避免重复处理
//        Duration between = Duration.between(dutyEnd, now);
//        return between.toMinutes() == SCHEDULE_RATE;
    }

    /**
     * 构建IM消息请求对象
     *
     * @param text 消息文本内容
     * @param to   接收用户ID
     * @return IM消息请求对象
     */
    private ImMessageRequest<TxtMsgVo> buildIMMsgReq(String text, String to) {
        ImMessageRequest<TxtMsgVo> request = new ImMessageRequest<>();
        request.setCategory(1);
        request.setMsgType(1);
        request.setPlaintext(1);
        request.setFrom(groupAiClient.getDefaultProxyUserId());
        request.setTo(to);

        TxtMsgVo txtVo = new TxtMsgVo();
        txtVo.setText(text);
        request.setMsg(txtVo);

        return request;
    }

    /**
     * 发送IM消息
     *
     * @param request IM消息请求对象
     * @return 消息ID
     */
    private String sendIMMsg(ImMessageRequest<TxtMsgVo> request) {
        return groupAiClient.sendMsg(request).getMsgId();
    }

    /**
     * 保存系统消息记录
     * <p>
     * 将发送的提醒消息记录到数据库，用于：
     * 1. 防止重复发送提醒
     * 2. 消息历史追溯
     *
     * @param message IM消息请求对象
     * @param bizId   业务ID（排班记录ID）
     * @param result  发送结果（消息ID）
     */
    private void saveSystemMsgRecord(ImMessageRequest<TxtMsgVo> message, Long bizId, String result) {
        ImUser userInfo = imService.findUserInfo(message.getTo());
        if (Objects.isNull(userInfo)) {
            log.warn("saveSystemMsgRecord userInfo is empty, userId: {}", message.getTo());
            return;
        }
        SystemMsg msg = new SystemMsg();
        msg.setId(idWorker.nextId());
        msg.setFromUserId(message.getFrom());
        msg.setUserId(Long.parseLong(message.getTo()));
        msg.setUserName(userInfo.getName());
        msg.setMsgType(String.valueOf(message.getMsgType()));
        msg.setNotifyType(NotifyTypeEnum.ON_DUTY_PERIOD.getCode());
        msg.setBizId(bizId);
        msg.setContent(message.getMsg().getText());
        msg.setResult(result);
        msg.setGmtCreated(new Date());

        List<ImUser.UserDepartment> userDepartments = userInfo.getUserDepartments();
        if (userDepartments != null && !userDepartments.isEmpty()) {
            userDepartments.stream().filter(ImUser.UserDepartment::getIsPrimary).findAny().ifPresent(dep -> {
                msg.setDeptId(dep.getId());
                msg.setDeptName(dep.getDepartmentName());
            });
        }

        systemMsgService.save(msg);
    }
}