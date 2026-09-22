package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.service.RoleRpcService;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.MsgBody;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceUserDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.CollaborationTaskDelayDto;
import com.tdtech.cloudcmd.im.jingxin.server.enums.Constant;
import com.tdtech.cloudcmd.im.jingxin.server.enums.SwitchTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.excel.CollaborationAttendanceExcel;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationAttendanceService;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostGroupService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICooperationUnattendedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceSwitchMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostMapper;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author syf
 * @date 2025/7/16
 **/
@Service
@Slf4j
public class CollaborationAttendanceServiceImpl extends ServiceImpl<CollaborationAttendanceMapper, CollaborationAttendance> implements CollaborationAttendanceService {

    private static final String HEARTBEAT_REDIS_KEY = "cloudcmd:im:jinxing:heartbeat";
    private static final String SHEET_NAME = "协同岗人员上下岗记录";

    @Value("${cloudcmd.im.jinxing.heartbeat.threshold:30000}")
    private Long heartbeat_threshold;

    @Resource
    private StreamBridge streamBridge;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private CollaborationAttendanceMapper collaborationAttendanceMapper;
    @Resource
    private CollaborationAttendanceSwitchMapper collaborationAttendanceSwitchMapper;
    @Resource
    private CollaborationPostMapper collaborationPostMapper;
    @Resource
    private CollaborationPostGroupService collaborationPostGroupService;

    @Resource
    private ICooperationUnattendedService cooperationUnattendedService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Autowired
    private ICollaborationTaskService taskService;

    @DubboReference
    RoleRpcService roleRpcService;

    @Resource
    private IdWorker idWorker;

    @Scheduled(initialDelay = 10L * 1000L, fixedDelay = 10L * 1000L)
    public void heartbeatCheckout() {
        // 心跳维持30秒
        var offlineTime = System.currentTimeMillis() - heartbeat_threshold;
        var longs = redisUtil.zRangeByScore(HEARTBEAT_REDIS_KEY, 0d, offlineTime, Long.class);
        redisUtil.zRemRangeByScore(HEARTBEAT_REDIS_KEY, 0d, offlineTime);
        longs.forEach(userId -> {
            var collaborationPosts = collaborationPostMapper.listByUserId(userId + "");
            if (collaborationPosts == null || collaborationPosts.isEmpty()) {
                log.warn("user:{} has no post", userId);
                return;
            }
            // update
            var wrapper = Wrappers.lambdaUpdate(CollaborationAttendanceSwitch.class)
                .eq(CollaborationAttendanceSwitch::getPersonId, userId)
                .set(CollaborationAttendanceSwitch::getSwitchStatus, 1)
                .set(CollaborationAttendanceSwitch::getUpdateTime, new Date());
            var update = collaborationAttendanceSwitchMapper.update(null, wrapper);
            if (update == 0) {
                collaborationAttendanceSwitchMapper
                    .insert(CollaborationAttendanceSwitch.builder().personId(userId).switchStatus(1).build());
            }
            collaborationPostGroupService.changeUserStatus(1, userId);
            collaborationPosts.stream().forEach(post -> {
                var uIds = post.getRelatedUserIds().split(",");
                var uNames = post.getRelatedUserNames().split(",");
                String uName = null;
                for (int i = 0; i < uIds.length; i++) {
                    if (Objects.equals(uIds[i], userId + "")) {
                        uName = uNames[i];
                        break;
                    }
                }
                var ca = CollaborationAttendance.builder().postId(post.getId()).postName(post.getPostName())
                    .orgId(post.getOrgId()).orgName(post.getOrgName()).personId(userId).personName(uName)
                    .createTime(new Date()).build();
                saveRecord(ca, 1);
                // 状态通过websocket推送另一端
                Map<String, String> sendMap = new HashMap<>();
                sendMap.put("personId", String.valueOf(ca.getPersonId()));
                sendMap.put("type", ca.getType());
                sendSwitchStatusToCagent(sendMap);
            });
        });
    }

    @Override
    public void heartbeat(Long userId) {
        redisUtil.zAdd(HEARTBEAT_REDIS_KEY, System.currentTimeMillis(), userId);
    }

    @Override
    public Page<CollaborationAttendance> getPage(int pageNum, int pageSize, String postName, String orgName,
        Long orgId, String personName, String startTime, String endTime) {
        List<Long> orgIds =
                organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                        .collect(Collectors.toList());
        return collaborationAttendanceMapper.selectPageWithCondition(new Page<>(pageNum, pageSize), postName, orgName,
                orgIds, personName, startTime, endTime);
    }



    @Override
    public Map<String, Object> getLastNumByPostId(Long postId, Long userId) {
        Map<String, Object> resultMap = new HashMap<>();
        Integer lastNumByPostId = collaborationAttendanceMapper.getLastNumByPostId(postId);
        resultMap.put("lastPeopleNum", lastNumByPostId == null ? 0 : lastNumByPostId);
        resultMap.put("personName", collaborationAttendanceMapper.getLastRandomPerson(postId, userId));
        return resultMap;
    }

    @Override
    public boolean save(CollaborationAttendance collaborationPost) {
        long startTime = System.currentTimeMillis();
        log.trace("switchStatus startTime:{}", startTime);
        // 人员开关状态插入数据库
        var status = upsertSwitch(collaborationPost);
        log.trace("switchStatus status: {}, time: {}", status, System.currentTimeMillis() - startTime);
        collaborationPostGroupService.changeUserStatus(status, collaborationPost.getPersonId());
        log.trace("switchStatus change status :{}", System.currentTimeMillis() - startTime);
        saveRecord(collaborationPost, status);
        log.trace("switchStatus save status :{}", System.currentTimeMillis() - startTime);
        // 状态通过websocket推送另一端
        Map<String, String> sendMap = new HashMap<>();
        sendMap.put("personId", String.valueOf(collaborationPost.getPersonId()));
        sendMap.put("type", collaborationPost.getType());
        sendSwitchStatusToCagent(sendMap);
        log.trace("switchStatus endTime:{},时间差:{}", System.currentTimeMillis(), System.currentTimeMillis() - startTime);
        return true;
    }

    @Override
    public boolean saveAttendance(List<CollaborationPost> collaborationPosts, CollaborationAttendanceUserDTO user) {
        // 防止重复点击
        String key = "collaboration:attendance:lock:" + user.getUserId();
        boolean acquired = redisUtil.setNx(key, true, 10L, TimeUnit.SECONDS);
        if (!acquired) {
            throw new BusinessException("请勿频繁操作，两次操作请间隔10秒");
        }

        // 人员开关状态插入数据库
        Long userId = user.getUserId();
        String userName = user.getUserName();
        Integer switchType = user.getSwitchType();
        var status = upsertSwitchByPersonId(userId, switchType);
        collaborationPostGroupService.changeUserStatus(status, userId, collaborationPosts);
        // 兼顾一个人加入多个协同岗的场景
        Map<Long, CollaborationPost> postMap = collaborationPosts.stream()
                .collect(Collectors.toMap(CollaborationPost::getId, p -> p, (a, b) -> a));
        collaborationPosts.forEach(post -> {
            CollaborationAttendance attendance = CollaborationAttendance.builder().orgId(post.getOrgId())
                    .orgName(post.getOrgName())
                    .postId(post.getId())
                    .postName(post.getPostName())
                    .personId(userId)
                    .personName(userName)
                    .switchType(switchType == null ? SwitchTypeEnum.USER_CLICK.getCode() : switchType)
                    .build();
            saveRecord(attendance, status, postMap.get(post.getId()));
            // 处理待分配的协同岗任务
            if (status == 0) {
                // 拆成两步的目的，是为了保证事务执行完成。再进行通知
                List<CollaborationTaskDelayDto> delayDtos = taskService.dealUnassignTask(attendance.getPostId(), userId);
                taskService.pushToDealyQueue(delayDtos);
            }
        });
        String type = status == 0 ? "上岗" : "下岗";
        // 状态通过websocket推送另一端
        Map<String, String> sendMap = new HashMap<>();
        sendMap.put("personId", String.valueOf(userId));
        sendMap.put("type", type);
        sendSwitchStatusToCagent(sendMap);
        return true;
    }

    @Override
    public void saveRecord(CollaborationAttendance collaborationPost, int status) {
        saveRecord(collaborationPost, status, null);
    }

    private void saveRecord(CollaborationAttendance collaborationPost, int status, CollaborationPost post) {
        Long postId = collaborationPost.getPostId();
        collaborationPost.setType(status == 0 ? "上岗" : "下岗");
        CollaborationAttendance collaborationAttendance = collaborationAttendanceMapper
            .selectLatestRecordByPostAndPerson(postId, collaborationPost.getPersonId());
        if (collaborationAttendance == null || !collaborationPost.getType().equals(collaborationAttendance.getType())) {
            // 如果需要实时计算插入岗位剩余人数及剩余人员，则在此处计算填充
            calcLastDetail(collaborationPost, status, post);
            // 协同岗开关记录插入数据库
            collaborationAttendanceMapper.insert(collaborationPost);
            // 如果剩余在岗人数为0，则生成一条告警记录
            Integer lastPeopleNum = collaborationPost.getLastPeopleNum();
            log.info("协同岗id: {}, 剩余在岗人数: {}", postId, lastPeopleNum);
            if (Objects.nonNull(lastPeopleNum) && lastPeopleNum.intValue() == 0) {
                cooperationUnattendedService.saveUnattendedRecord(postId);
            }
        }
    }

    /**
     * 计算当前切换开关后协同岗剩余人员信息状态——需将库内当前状态统计结果结合本次操作类型综合计算
     *
     * @param attendance
     * @param currentStatus
     */
    private void calcLastDetail(CollaborationAttendance attendance, int currentStatus, CollaborationPost post) {
        Integer lastNum = collaborationAttendanceMapper.getLastNumByPostId(attendance.getPostId());
        lastNum = lastNum == null ? 0 : lastNum;
        List<String> peopleList = collaborationAttendanceMapper.listLastPeopleByPostId(attendance.getPostId());
        CollaborationPost collaborationPost = post != null ? post : collaborationPostMapper.selectById(attendance.getPostId());
        if (collaborationPost == null) {
            return;
        }
        String relatedUserNames = collaborationPost.getRelatedUserNames();
        if (peopleList != null && !peopleList.isEmpty() && relatedUserNames != null && !relatedUserNames.isBlank()) {
            // 拆分为数组并转换为集合
            Set<String> splitSet = new HashSet<>(peopleList);
            Set<String> split1Set = new HashSet<>(Arrays.asList(relatedUserNames.split(",")));
            // 找出移除的元素
            Set<String> removed = new HashSet<>(splitSet);
            removed.removeAll(split1Set);
            // 在集合中移除这些元素
            peopleList.removeAll(removed);
            // 输出或处理结果
            log.info("移除的元素: {}", removed);
        }
        Set<String> lastPeopleSet = new HashSet<>();
        String personName = attendance.getPersonName();
        if (currentStatus == 0) {
            // 上岗
            if (CollectionUtils.isNotEmpty(peopleList)) {
                lastPeopleSet.addAll(peopleList);
            }
            lastPeopleSet.add(personName);
            lastNum = lastPeopleSet.size();
        } else {
            // 下岗
            lastNum = lastNum.equals(0) ? lastNum : lastNum - 1;
            if (CollectionUtils.isNotEmpty(peopleList)) {
                peopleList.forEach(t -> {
                    if (!personName.equals(t)) {
                        lastPeopleSet.add(t);
                    }
                });
            }
        }
        attendance.setLastPeopleNum(lastNum);
        attendance.setLastPeople(CollectionUtils.isEmpty(lastPeopleSet) ? null : String.join(",", lastPeopleSet));
    }

    @Override
    public CollaborationAttendanceSwitch getSwitchStatusByPerson(Long personId) {
        return collaborationAttendanceSwitchMapper.getByPersonId(personId);
    }

    /**
     * 切换开关状态
     *
     * @param collaborationPost
     * @return
     */
    private int upsertSwitch(CollaborationAttendance collaborationPost) {
        CollaborationAttendanceSwitch attendanceSwitch =
            collaborationAttendanceSwitchMapper.getByPersonId(collaborationPost.getPersonId());
        // 默认下岗，所以切为上岗
        int switchStatus = 0;
        Date now = new Date();
        if (attendanceSwitch == null) {
            // insert
            collaborationAttendanceSwitchMapper.insert(CollaborationAttendanceSwitch.builder()
                .personId(collaborationPost.getPersonId()).switchStatus(switchStatus)
                .lastManualOperationTime(now).build());
        } else {
            // update
            UpdateWrapper<CollaborationAttendanceSwitch> wrapper = new UpdateWrapper<>();
            wrapper.eq("person_id", collaborationPost.getPersonId());
            attendanceSwitch.setSwitchStatus(attendanceSwitch.getSwitchStatus() ^ 1);
            attendanceSwitch.setUpdateTime(now);
            attendanceSwitch.setGmtCreated(now);
            attendanceSwitch.setLastManualOperationTime(now);
            switchStatus = attendanceSwitch.getSwitchStatus();
            collaborationAttendanceSwitchMapper.update(attendanceSwitch, wrapper);
        }
        return switchStatus;
    }

    private int upsertSwitchByPersonId(Long personId, Integer switchType) {
        CollaborationAttendanceSwitch attendanceSwitch =
            collaborationAttendanceSwitchMapper.getByPersonId(personId);
        // 默认下岗，所以切为上岗
        int switchStatus = 0;
        Date now = new Date();
        Integer actualSwitchType = switchType == null ? SwitchTypeEnum.USER_CLICK.getCode() : switchType;
        if (attendanceSwitch == null) {
            // insert
            collaborationAttendanceSwitchMapper.insert(CollaborationAttendanceSwitch.builder()
                .personId(personId).switchStatus(switchStatus).switchType(actualSwitchType)
                .lastManualOperationTime(now).build());
        } else {
            // update
            UpdateWrapper<CollaborationAttendanceSwitch> wrapper = new UpdateWrapper<>();
            wrapper.eq("person_id", personId);
            attendanceSwitch.setSwitchStatus(attendanceSwitch.getSwitchStatus() ^ 1);
            attendanceSwitch.setSwitchType(actualSwitchType);
            setSwitchRemark(attendanceSwitch, switchType);
            attendanceSwitch.setUpdateTime(now);
            attendanceSwitch.setGmtCreated(now);
            attendanceSwitch.setLastManualOperationTime(now);
            switchStatus = attendanceSwitch.getSwitchStatus();
            collaborationAttendanceSwitchMapper.update(attendanceSwitch, wrapper);
        }
        return switchStatus;
    }

    private void setSwitchRemark(CollaborationAttendanceSwitch attendanceSwitch, Integer switchType) {
        if (switchType != null && switchType.equals(SwitchTypeEnum.ADMIN_CLICK.getCode())) {
            //管理员操作下岗时，记录id和名称
            UserInfo user = SecurityUtils.getUser();
            if (Objects.isNull(user)) {
                log.error("获取当前登录用户信息失败");
            } else {
                String remark = "管理员操作下岗，操作人：" + user.getUserName() + "，操作人id：" + user.getUserId();
                attendanceSwitch.setRemark(remark);
            }
        }
    }

    /**
     * 发送协同岗开关给到客户端
     *
     * @param dataMap
     */
    @Override
    public void sendSwitchStatusToCagent(Map<String, String> dataMap) {
        CagentMqFrame frame = new CagentMqFrame();
        frame.setSubsystem("CLOUDCMD_IM_JINGXIN");
        frame.setType(Short.parseShort("2"));
        String tokenKey = "*-*";
        frame.setTokenkey(tokenKey);
        MsgBody<Map<String, String>> body = new MsgBody<>();
        body.setModule("CLOUDCMD_IM_JINGXIN");
        body.setNotifyType("switch_status");
        body.setData(dataMap);
        frame.setBody(body);
        try {
            var msg = JsonUtil.toJsonStr(frame);
            streamBridge.send("cloudcmd-cagent", MessageBuilder.withPayload(msg).build());
        } catch (Exception e) {
            log.error("send message:{} failed", frame, e);
        }
    }

    @Override
    public void exportToExcel(String fileName, String postName, String orgName, String orgIds, String personName,
        String startTime, String endTime, HttpServletResponse response) throws IOException {
        List<String> orgArr;
        if (orgIds != null && !orgIds.isBlank()) {
            orgArr = Arrays.asList(orgIds.split(","));
        } else {
            orgArr = null;
        }
        List<CollaborationAttendance> list = collaborationAttendanceMapper.selectListByCondition(postName, orgName,
            orgArr, personName, startTime, endTime);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition",
            "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");

        List<CollaborationAttendanceExcel> excelList = list.stream().map(data -> {
            CollaborationAttendanceExcel excel = new CollaborationAttendanceExcel();
            BeanUtils.copyProperties(data, excel);

            Date createTime = excel.getCreateTime();
            if (Objects.nonNull(createTime)) {
                excel.setCreateTimeStr(DateFormatUtil.format(createTime));
            }

            Integer switchType = excel.getSwitchType();
            if (Objects.nonNull(switchType)) {
                SwitchTypeEnum switchTypeEnum = SwitchTypeEnum.matchCode(switchType);
                if (Objects.nonNull(switchType)) {
                    excel.setSwitchTypeName(switchTypeEnum.getDesc());
                }
            }

            return excel;
        }).collect(Collectors.toList());

        EasyExcel.write(response.getOutputStream(), CollaborationAttendanceExcel.class).sheet(SHEET_NAME).doWrite(excelList);
    }

    @Override
    public List<ImUserDto> getOnlineByPostId(Long postId) {
        List<CollaborationAttendance> onDutyPeople = collaborationAttendanceMapper.lastPeopleListByPostIdList(List.of(postId));
        if(CollectionUtils.isNotEmpty(onDutyPeople)) {
            List<Long> ids = onDutyPeople.stream().map(CollaborationAttendance::getPersonId).collect(Collectors.toList());
            return roleRpcService.getUserInfoByUserId(ids);
        }
        return List.of();
    }

    @Override
    public boolean adminOffline(List<CollaborationPost> collaborationPosts, CollaborationAttendanceUserDTO user) {
        // 判断该成员是不是只在岗了一个并且就是要下载的这个协同岗
        List<CollaborationPost> onDutyList = collaborationPosts.stream().filter(post -> {
            CollaborationAttendance collaborationAttendance = collaborationAttendanceMapper.selectLatestRecordByPostAndPerson(post.getId(), user.getUserId());
            return collaborationAttendance != null &&  "上岗".equals(collaborationAttendance.getType());
        }).collect(Collectors.toList());
        if(onDutyList.size() == 1) {
            CollaborationPost collaborationPost = onDutyList.get(0);
            Long id = collaborationPost.getId();
            Long postId = user.getPostId();
            if(!Objects.equals(id, postId)) {
                throw new BusinessException("该成员所在的协同岗已经下岗，或从未上岗，不能下岗");
            }else{
                // 按照正常下岗逻辑下岗
                return saveAttendance(onDutyList, user);
            }
        }else{
            onDutyList = onDutyList.stream().filter(post -> Objects.equals(post.getId(), user.getPostId())).collect(Collectors.toList());
            if (onDutyList.size() != 1) {
                throw new BusinessException("该成员所在的协同岗已经下岗，或从未上岗，不能下岗");
            }
            var post = onDutyList.get(0);
            Long userId = user.getUserId();
            String userName = user.getUserName();
            Integer switchType = user.getSwitchType();
            // 如果当前人所在岗位不止一个在岗，则只需要新增一条下岗记录即可
            CollaborationAttendance attendance = CollaborationAttendance.builder().orgId(post.getOrgId())
                    .orgName(post.getOrgName())
                    .postId(post.getId())
                    .postName(post.getPostName())
                    .personId(userId)
                    .personName(userName)
                    .switchType(switchType == null ? SwitchTypeEnum.USER_CLICK.getCode() : switchType)
                    .build();
            saveRecord(attendance, 1);
            // 群里通知下岗
            collaborationPostGroupService.removeSupport(userId, post.getId());
        }

        return false;
    }

    @Scheduled(cron = "0 0 0 * * ?")   // 每天 00:00:00 执行
    public void deleteAttendanceHistoryTask() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, - Constant.SAVE_YEARS);
        Date threeYearsAgo = cal.getTime();
        LambdaQueryWrapper<CollaborationAttendance> lambdaQueryWrapper = new LambdaQueryWrapper<CollaborationAttendance>()
                .lt(CollaborationAttendance::getCreateTime, threeYearsAgo);
        remove(lambdaQueryWrapper);
    }

}