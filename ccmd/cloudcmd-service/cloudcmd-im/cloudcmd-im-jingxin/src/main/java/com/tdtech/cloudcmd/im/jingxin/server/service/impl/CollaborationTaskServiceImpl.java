package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.toolkit.MPJWrappers;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationOverdueVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.CollaborationTaskDelayDto;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupExtendsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksExpiredService;
import com.tdtech.cloudcmd.im.jingxin.server.service.SendPreWarningMessageService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationTaskMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationTaskResponseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationTaskStatusHistoryMapper;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.*;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Service
@Slf4j
public class CollaborationTaskServiceImpl extends ServiceImpl<CollaborationTaskMapper, CollaborationTask>
    implements ICollaborationTaskService {
    private static final String UPDATE_TASK = "UPDATE_TASK";
    private static final String MSG_TOPIC = "cloudcmd-cagent";
    private static final String MSG_NOTIFY_TYPE = "task";
    private static final Integer TASK_RESPONSE = 6;
    private static final Integer TASK_ALL = 5;
    /**
     * 待办消息
     */
    private static final Integer TODO_TASKS = 1;
    /**
     * 跟踪（需埋点：每个 task 只记首次）
     */
    private static final Integer TRACK_TASKS = 2;
    /**
     * 办结（需埋点：终态天然唯一）
     */
    private static final Integer FINISH_TASKS = 3;
    /**
     * 忽略（需埋点：终态天然唯一。代码注释历史遗留为"无需回复"，业务确认为"忽略"）
     */
    private static final Integer IGNORE_TASKS = 4;
    /**
     * 未及时回复
     */

    private static final Integer DELAY_TASKS = 7;
    /**
     * 已逾期消息
     */
    private static final Integer BACK_TASKS = 8;
    private static final String TASK_DELAY_QUEUE_1 = "task_delay_queue_1";
    private static final String TASK_DELAY_QUEUE_2 = "task_delay_queue_2";
    @Resource
    private CollaborationTaskMapper collaborationTaskMapper;
    @Resource
    private CollaborationTaskResponseMapper collaborationTaskResponseMapper;
    /**
     * 任务状态历史埋点 Mapper（写 tb_task_status_history）
     * 仅在任务忽略/跟踪/办结状态变更时写入，服务于 tb_static_task 统计表
     */
    @Resource
    private CollaborationTaskStatusHistoryMapper collaborationTaskStatusHistoryMapper;
    @DubboReference
    private GlobalsRpcService globalsService;
    @Resource
    private StreamBridge streamBridge;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ImService imService;
    @Resource
    private GroupExtendsService groupExtendsService;
    @Resource
    private RedisLockFactory redisLockFactory;

    @Resource
    private IdWorker idWorker;

    @Resource
    private ITasksExpiredService tasksExpiredService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    private SendPreWarningMessageService sendPreWarningMessageService;

    @Override
    public void save(CollaborationTaskCO collaborationTaskCO) {
        var existTask = collaborationTaskMapper.selectList(Wrappers.lambdaQuery(CollaborationTask.class)//
            .eq(CollaborationTask::getUserId, collaborationTaskCO.getUserId())//
            .eq(CollaborationTask::getIcsMsgId, collaborationTaskCO.getIcsMsgId())//
            .eq(CollaborationTask::getPostId, collaborationTaskCO.getPostId()));
        var exist = Optional.ofNullable(existTask).stream().flatMap(Collection::stream).findFirst();
        if (exist.isPresent()) {
            return;
        }
        var key = String.format("cloudcmd:im-jingxin:col-task:save-lock:%d:%d:%d", collaborationTaskCO.getUserId(),
            collaborationTaskCO.getIcsMsgId(), collaborationTaskCO.getPostId());
        var redisLock = redisLockFactory.newRedisLock(key, Duration.ofMinutes(3L));
        try {
            redisLock.tryLockWithException(30L, TimeUnit.SECONDS);
            //double check lock
            existTask = collaborationTaskMapper.selectList(Wrappers.lambdaQuery(CollaborationTask.class)//
                .eq(CollaborationTask::getUserId, collaborationTaskCO.getUserId())//
                .eq(CollaborationTask::getIcsMsgId, collaborationTaskCO.getIcsMsgId())//
                .eq(CollaborationTask::getPostId, collaborationTaskCO.getPostId()));
            exist = Optional.ofNullable(existTask).stream().flatMap(Collection::stream).findFirst();
            if (exist.isPresent()) {
                return;
            }
            collaborationTaskCO.setGmtCreated(new Date());
            CollaborationTask collaborationTask = BeanCopyUtils.copyBean(collaborationTaskCO, new CollaborationTask());
            collaborationTaskMapper.insert(collaborationTask);
            // 2026.04.24 逻辑变更，待分配的任务不执行任务推送和延迟修改为未及时回复状态
            if (collaborationTaskCO.getStatus() != -1) {
                dealDelayTask(collaborationTaskCO, collaborationTask);
            }
        } finally {
            redisLock.unlock();
        }
    }

    /**
     * 任务推送及延迟未回复的处理
     */
    private void dealDelayTask(CollaborationTaskCO collaborationTaskCO, CollaborationTask collaborationTask) {
        // 推送
        if (collaborationTaskCO.getToExecutorId() != null && collaborationTaskCO.getStatus() < 3) {
//                List<Long> ids = new ArrayList<>();
//                ids.add(collaborationTaskCO.getToExecutorId());
//                var cagentMqFrame =
//                    new CagentMqFrame().toBuilder().typeSubSystemMessage(UPDATE_TASK).unicast().executorIds(ids)
//                        .appKeys(Collections.singletonList("CDC-1000")).build().body(UPDATE_TASK, MSG_NOTIFY_TYPE, 1)
//                        .build();
//                streamBridge.send(MSG_TOPIC, cagentMqFrame);

            var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(UPDATE_TASK).broadcast()
                    .body(UPDATE_TASK, MSG_NOTIFY_TYPE, 1).build();
            streamBridge.send("cloudcmd-cagent", cagentMqFrame);

            log.debug("send cagent msg:{} done", cagentMqFrame);
        }
        log.info("CollaborationTask save:{}", collaborationTask);
        // 延迟队列
        CollaborationTask task = collaborationTaskMapper.getCollaborationTask(collaborationTask.getId());
        if (task != null) {
            collaborationTaskCO.setId(task.getId());
            log.debug("add redis collaborationTaskCO:{}", collaborationTaskCO);
            redisUtil.zAdd(TASK_DELAY_QUEUE_1, task.getGmtCreated().getTime(), collaborationTaskCO);
            redisUtil.zAdd(TASK_DELAY_QUEUE_2, task.getGmtCreated().getTime(), collaborationTaskCO);
        }
    }

    @Override
    public PageResult<CollaborationTask> listTask(CollabsTaskListReqCO collabsTaskListReqCO) {
        PageResult<CollaborationTask> result = new PageResult<>();
        IPage<CollaborationTask> iPage = new Page<>(collabsTaskListReqCO.getPage(), collabsTaskListReqCO.getPageSize());
//        QueryWrapper<CollaborationTask> queryWrapper = new QueryWrapper<>();
        var queryWrapper = MPJWrappers.lambdaJoin(CollaborationTask.class)
        .selectAll(CollaborationTask.class).distinct()
        .leftJoin(CollaborationPost.class, on -> on
                .eq(CollaborationPost::getId, CollaborationTask::getPostId)
                .eq(CollaborationPost::getDeleted, 0));
        // 2026.04.24 待分配的任务不应该返回
        queryWrapper.ne(CollaborationTask::getStatus, -1);
        if (collabsTaskListReqCO.getUserId() != null) {
            queryWrapper.eq(CollaborationTask::getUserId, collabsTaskListReqCO.getUserId());
        }
        if (collabsTaskListReqCO.getPostId() != null) {
            queryWrapper.eq(CollaborationTask::getPostId, collabsTaskListReqCO.getPostId());
        }
        if (StringUtils.isNotBlank(collabsTaskListReqCO.getPostIds())) {
            String[] postIdList = collabsTaskListReqCO.getPostIds().split(",");
            queryWrapper.in(CollaborationTask::getPostId, postIdList);
        }
        if (collabsTaskListReqCO.getKeywords() != null && !collabsTaskListReqCO.getKeywords().isEmpty()) {
            String keywords = collabsTaskListReqCO.getKeywords();
            if ("@所有人".equals(keywords)) {
                queryWrapper.like(CollaborationTask::getText, "@all");
            } else {
                queryWrapper.apply("SUBSTRING(`text`, LOCATE(':', `text`) + 1) LIKE {0}", "%" + keywords + "%");
            }
        }
        // 按时间倒序排序
        queryWrapper.orderByDesc(CollaborationTask::getGmtCreated);
        IPage<CollaborationTask> collaborationTaskIPage;
        // 判断是否是回复问题
        if (!Objects.equals(collabsTaskListReqCO.getType(), TASK_RESPONSE)) {
            if (!Objects.equals(collabsTaskListReqCO.getType(), TASK_ALL)
                && !Objects.equals(collabsTaskListReqCO.getType(), TODO_TASKS)) {
                queryWrapper.eq(CollaborationTask::getStatus, collabsTaskListReqCO.getType());
            }
            if (Objects.equals(collabsTaskListReqCO.getType(), TODO_TASKS)) {
                queryWrapper.in(CollaborationTask::getStatus, Arrays.asList(TODO_TASKS, DELAY_TASKS, BACK_TASKS));
            }
        } else {
            queryWrapper.eq(CollaborationTask::getReply, 1);
        }
        collaborationTaskIPage = collaborationTaskMapper.selectJoinPage(iPage, CollaborationTask.class, queryWrapper);
        // 处理回复内容
        List<CollaborationTask> records = collaborationTaskIPage.getRecords();
        if(CollectionUtils.isNotEmpty(records)) {
            records.forEach(task -> task.setText(removeSpacesInBrackets(task.getText())));
        }
        result.setRecords(records);
        result.setTotal(collaborationTaskIPage.getTotal());
        result.setPages(collaborationTaskIPage.getPages());
        result.setCurrent(collaborationTaskIPage.getCurrent());
        result.setSize(collaborationTaskIPage.getSize());
        return result;
    }

    private String removeSpacesInBrackets(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll(" ", "");
    }

    @Override
    public Map<String, Long> statistics(Long userId, String postIds, String departmentCode, String startTime,
        String endTime) {
        Map<String, Long> map = new HashMap<>();
        List<Long> collect = new ArrayList<>();
        log.info("statistics queryTime start :{}", System.currentTimeMillis());
        if (StringUtils.isNotBlank(departmentCode)) {
            // 查找当前部门编号
            List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
            List<String> codeList = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
            // 根据当前部门及下级部门，查找所有的协同用户，
            for (String code : codeList) {
                List<ImUser> imUsers = imService.queryUser(code, null, null, null, null);
                List<Long> userIdList = imUsers.stream().map(ImUser::getId).collect(Collectors.toList());
                collect.addAll(userIdList);
            }

        }
        log.info("statistics queryTime end :{}", System.currentTimeMillis());
        if (ListUtils.isBlankList(collect) && userId == null && StringUtils.isBlank(postIds)) {
            log.info("statistics collect is null");
            map.put("1", 0L);
            map.put("2", 0L);
            map.put("3", 0L);
            map.put("4", 0L);
            map.put("5", 0L);
            map.put("6", 0L);
            map.put("7", 0L);
            return map;
        }
        log.info("statistics collect:{}", collect);
        List<Long> postIdList = new ArrayList<>();
        if (StringUtils.isNotBlank(postIds)) {
            postIdList.addAll((Arrays.stream(postIds.split(",")).map(Long::parseLong).collect(Collectors.toList())));
        }

        List<CollaborationTask> taskList = collaborationTaskMapper.findListByCondition(List.of(1, 2, 3, 4, 7, 8), userId, postIdList, collect, startTime, endTime);
        Map<Integer, Long> taskCountMap =
                taskList.stream().collect(Collectors.groupingBy(CollaborationTask::getStatus, Collectors.counting()));

        Long aLong = taskCountMap.getOrDefault(1, 0L);

        Long bLong = taskCountMap.getOrDefault(2, 0L);

        Long cLong = taskCountMap.getOrDefault(3, 0L);

        Long dLong = taskCountMap.getOrDefault(4, 0L);

        Long eLong = taskCountMap.getOrDefault(7, 0L);

        Long fLong = taskCountMap.getOrDefault(8, 0L);

        // 查找待办问题统计
        map.put("1", aLong + eLong + fLong);

        // 查找跟踪问题统计
        map.put("2", bLong);

        // 查找已办结问题统计
        map.put("3", cLong);

        // 无需回复问题统计
        map.put("4", dLong);

        // 全部问题统计
        map.put("5", aLong + bLong + cLong + dLong + eLong + fLong);

        // 全部回复问题统计
        Long replyNum = collaborationTaskResponseMapper.queryNumById(userId, postIdList, collect, startTime, endTime);
        map.put("6", replyNum);

        map.put("7", fLong);
        log.info("statistics allTime end :{}", System.currentTimeMillis());
        return map;
    }

    @Override
    public CTaskRespVo getCollaborationTaskDetail(Long taskId, Long userId, Long postId) {
        CTaskRespVo cTaskRespVo = new CTaskRespVo();
        // 查提问人详情
        CollaborationTask imCollaborationTask = collaborationTaskMapper.getCollaborationTask(taskId);
        cTaskRespVo.setMsgSentTime(imCollaborationTask.getMsgSentTime());
        cTaskRespVo.setGroupName(imCollaborationTask.getGroupName());
        cTaskRespVo.setFromUserDepartmentName(imCollaborationTask.getFromUserDepartmentName());
        cTaskRespVo.setFromUserName(imCollaborationTask.getFromUserName());
        cTaskRespVo.setFromUserNick(imCollaborationTask.getFromUserNick());
        cTaskRespVo.setText(imCollaborationTask.getText());
        cTaskRespVo.setIcsMsgId(imCollaborationTask.getIcsMsgId());
        cTaskRespVo.setStatus(imCollaborationTask.getStatus());
        cTaskRespVo.setTaskId(imCollaborationTask.getId());
        cTaskRespVo.setReply(imCollaborationTask.getReply());
        //查找群是否已归档
        try{
            GroupExtends groupExtends = groupExtendsService.getByGroupId(imCollaborationTask.getGroupId());
            cTaskRespVo.setArchive(groupExtends.getArchived());
        }catch (Exception e){
            log.error(e.getMessage());
        }
        // 查问题处理的回复列表
        List<CollaborationTaskResponse> imTaskResponses =
            collaborationTaskResponseMapper.getCTaskResps(taskId, userId, postId);
        // 对于传送不规范的信息将'@all '替换成 ':@所有人]'供前端展示
        if (CollectionUtils.isNotEmpty(imTaskResponses)) {
            imTaskResponses.forEach(
                    it -> it.setContent(formatResponse(it.getContent()))
            );
        }
        cTaskRespVo.setResponses(imTaskResponses);
        cTaskRespVo.setText(removeSpacesInBrackets(cTaskRespVo.getText()));
        log.info("imTaskResponses:{}", imTaskResponses);
        return cTaskRespVo;
    }

    private String formatResponse(String text) {
        // 暂时匹配'@all '替换成 ':@所有人]'供前端展示,但如果真实数据就是'@all '文本，会有问题
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (text.matches(".*\\[.*@.*\\].*")) {
            return text;
        }
        Pattern pattern = Pattern.compile("@([^ ]+) ");
        Matcher matcher = pattern.matcher(text);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String content = matcher.group(1);
            String replacement = content.equals("all") ? "[@:@所有人]" : "[@:@" + content + "]";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return String.valueOf(result);
    }

    @Override
    public void updateCollaborationTask(Long taskId, Long fromExecutorId, Integer status, Integer reply) {
        // 更新状态
        CollaborationTask imCollaborationTask = collaborationTaskMapper.getCollaborationTask(taskId);
        if (imCollaborationTask != null) {
            imCollaborationTask.setStatus(status);
            imCollaborationTask.setReply(reply);
            imCollaborationTask.setGmtModified(new Date());
            log.info("updateCollaborationTaskReply:{}", imCollaborationTask);
            collaborationTaskMapper.updateById(imCollaborationTask);
            // 状态历史埋点：忽略(4)/跟踪(2)/办结(3) 写 tb_task_status_history，服务于 tb_static_task 统计
            // fromExecutorId 即实际操作人（回复触发时由 CollaborationTaskResponseServiceImpl 传入回复人）
            recordTaskStatusHistory(taskId, fromExecutorId, status);
            // 推送 定义1为里面外面都需要重新调用统计接口，2为单独调用外面的接口，3为单独调用里面的接口
            if (fromExecutorId != null) {
                List<Long> ids = new ArrayList<>();
                ids.add(fromExecutorId);
                var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(UPDATE_TASK).unicast()
                    .executorIds(ids).appKeys(Collections.singletonList("CDC-1000")).build()
                    .body(UPDATE_TASK, MSG_NOTIFY_TYPE, 3).build();
                streamBridge.send(MSG_TOPIC, cagentMqFrame);
                log.info("send cagent msg:{} done", cagentMqFrame);
            }
        }
    }

    @Override
    public void updateCollaborationTasks(List<CollaborationTaskCO> cTaskCos) {
        // 遍历
        for (CollaborationTaskCO cTaskCo : cTaskCos) {
            // 校验
            if (cTaskCo.getId() != null && cTaskCo.getStatus() != null) {
                CollaborationTask updateTask = new CollaborationTask();
                updateTask.setId(cTaskCo.getId());
                updateTask.setStatus(cTaskCo.getStatus());
                updateTask.setGmtModified(new Date());
                log.info("updateTask:{}", updateTask);
                // 更新
                collaborationTaskMapper.updateById(updateTask);
                // 状态历史埋点：忽略(4)/跟踪(2)/办结(3) 写 tb_task_status_history，服务于 tb_static_task 统计
                // cTaskCo.getFromExecutorId() 即实际操作人（UI 批量操作时由前端传入操作人）
                recordTaskStatusHistory(cTaskCo.getId(), cTaskCo.getFromExecutorId(), cTaskCo.getStatus());
                // 推送 定义1为里面外面都需要重新调用统计接口，2为单独调用外面的接口，3为单独调用里面的接口
                if (cTaskCo.getFromExecutorId() != null) {
                    List<Long> ids = new ArrayList<>();
                    ids.add(cTaskCo.getFromExecutorId());
                    var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(UPDATE_TASK).unicast()
                        .executorIds(ids).appKeys(Collections.singletonList("CDC-1000")).build()
                        .body(UPDATE_TASK, MSG_NOTIFY_TYPE, 3).build();
                    streamBridge.send(MSG_TOPIC, cagentMqFrame);

                    log.info("send cagent msg:{} done", cagentMqFrame);
                }
            }
        }
    }

    /**
     * 任务状态历史埋点：将忽略(4)/跟踪(2)/办结(3) 状态变更写入 tb_task_status_history。
     * <p>
     * 服务于 tb_static_task 统计表，统计表据此取 ignore_time/track_time/finish_time 及对应操作人。
     * <p>
     * 写入规则（与 spec.md 一致）：
     * <ul>
     *   <li>跟踪(2)：先查重（同 task_id 且 status=2），已存在则不写。保证每个 task 的跟踪记录永远只一条，
     *       取的是首次跟踪的操作人和时间。</li>
     *   <li>办结(3)/忽略(4)：终态天然唯一，按 (task_id, status) 查重，已存在则不写，
     *       避免重复回复导致重复埋点。</li>
     *   <li>其他状态(1/7/8 等)：不记录。</li>
     * </ul>
     * <p>
     * 并发控制：tb_task_status_history 表无唯一索引（业务限制），"先查后插"存在 TOCTOU 竞态。
     * 采用 Redis 分布式锁（按 taskId）+ double check 保证 (task_id, status) 唯一：
     * <ol>
     *   <li>无锁快速查重：命中直接返回，减少无谓加锁</li>
     *   <li>加锁（key=taskId，等待30s，持有1min）</li>
     *   <li>加锁后 double check 查重，防快速路径与加锁之间的窗口</li>
     * </ol>
     * 锁粒度=taskId，不同 task 不互斥；HTTP 查警信放锁内保证查重-查询-插入原子性。
     * <p>
     * user_id 取实际操作人（即方法入参 operatorUserId），不是 tb_task.user_id；
     * 若拿不到操作人则留空，统计表字段允许为空。userName/department 通过警信查询补全，
     * 查询失败不影响主流程（try-catch 兜底，含 Redis 故障/锁超时场景）。
     *
     * @param taskId         任务ID
     * @param operatorUserId 实际操作人ID（回复场景为回复人，UI 场景为前端传入的 fromExecutorId）
     * @param status         新状态
     */
    private void recordTaskStatusHistory(Long taskId, Long operatorUserId, Integer status) {
        // 仅记录忽略(4)/跟踪(2)/办结(3) 三种状态，其他状态直接返回
        if (status == null
            || (!Objects.equals(status, IGNORE_TASKS)
            && !Objects.equals(status, TRACK_TASKS)
            && !Objects.equals(status, FINISH_TASKS))) {
            return;
        }
        if (operatorUserId == null) {
            UserInfo user = SecurityUtils.getUser();
            if (user != null) {
                operatorUserId = user.getUserId();
            }
        }
        // 快速路径：先无锁查一次重，已存在直接返回，避免无谓加锁（大部分并发其实命中这里）
        if (existsTaskStatusHistory(taskId, status)) {
            log.info("recordTaskStatusHistory skip, taskId:{}, status:{} already exists", taskId, status);
            return;
        }
        // 按 taskId 加分布式锁，防止"先查后插"竞态导致并发重复写入
        // 锁粒度=taskId：不同 task 不互斥，仅同 task 的状态变更串行化
        // 不加 DB 唯一索引（业务限制），故靠锁保证 (task_id, status) 唯一
        String lockKey = String.format("cloudcmd:im-jingxin:task-status-history:%d", taskId);
        var redisLock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(1L));
        try {
            redisLock.tryLockWithException(30L, TimeUnit.SECONDS);
            // double check：加锁后再查一次重，防快速路径与加锁之间的窗口被另一线程抢先插入
            if (existsTaskStatusHistory(taskId, status)) {
                log.info("recordTaskStatusHistory skip after lock, taskId:{}, status:{}", taskId, status);
                return;
            }

            // 通过警信补全操作人姓名和主部门信息（HTTP 调用，失败不阻断主流程）
            // 放锁内：保证查重-查询-插入的原子性，HTTP 耗时不影响正确性，仅延长锁持有
            String userName = null;
            Long departmentId = null;
            String departmentName = null;
            if (operatorUserId != null) {
                ImUser imUser = imService.findUserInfo(String.valueOf(operatorUserId));
                if (imUser != null) {
                    userName = imUser.getName();
                    ImUser.UserDepartment primaryDept = imUser.getPrimaryDepartment();
                    if (primaryDept != null) {
                        departmentId = primaryDept.getId();
                        departmentName = primaryDept.getDepartmentName();
                    }
                } else {
                    log.warn("recordTaskStatusHistory user not found in im, taskId:{}, userId:{}",
                        taskId, operatorUserId);
                }
            }

            CollaborationTaskStatusHistory history = new CollaborationTaskStatusHistory()
                .setTaskId(taskId)
                .setStatus(status)
                .setUserId(operatorUserId)
                .setUserName(userName)
                .setDepartmentId(departmentId)
                .setDepartmentName(departmentName)
                .setIsDeleted(0)
                .setGmtCreated(new Date())
                .setGmtModified(new Date());
            collaborationTaskStatusHistoryMapper.insert(history);
            log.info("recordTaskStatusHistory done, taskId:{}, status:{}, userId:{}, userName:{}",
                taskId, status, operatorUserId, userName);
        } catch (Exception e) {
            // 埋点失败（含 Redis 故障/锁超时）不影响业务主流程（任务状态更新已成功）
            log.error("recordTaskStatusHistory error, taskId:{}, userId:{}, status:{}",
                taskId, operatorUserId, status, e);
        } finally {
            redisLock.unlock();
        }
    }

    /**
     * 查重：同 task_id 且同 status 是否已存在历史记录。
     * <p>
     * 用于埋点前的去重判断：跟踪(2)保证每个 task 永远只一条首次记录，
     * 办结(3)/忽略(4)终态天然唯一，重复回复不再埋点。
     */
    private boolean existsTaskStatusHistory(Long taskId, Integer status) {
        Long existCount = collaborationTaskStatusHistoryMapper.selectCount(
            Wrappers.lambdaQuery(CollaborationTaskStatusHistory.class)
                .eq(CollaborationTaskStatusHistory::getTaskId, taskId)
                .eq(CollaborationTaskStatusHistory::getStatus, status));
        return existCount != null && existCount > 0;
    }

    @Override
    public CTaskRespVo getCollaborationTaskDetailBySeqId(Long seqId, Long userId, Long postId) {
        QueryWrapper<CollaborationTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(CollaborationTask.ICS_MSG_ID, seqId);
        if (userId != null) {
            queryWrapper.eq(CollaborationTask.USER_ID, userId);
        }
        if (postId != null) {
            queryWrapper.eq(CollaborationTask.POST_ID, postId);
        }
        CollaborationTask collaborationTask = collaborationTaskMapper.selectOne(queryWrapper);
        log.info("collaborationTask:{}", collaborationTask);
        CTaskRespVo cTaskRespVo = new CTaskRespVo();
        if (collaborationTask != null) {
            cTaskRespVo = BeanCopyUtils.copyBean(collaborationTask, CTaskRespVo::new);
            cTaskRespVo.setTaskId(collaborationTask.getId());
            cTaskRespVo.setReply(collaborationTask.getReply());
            cTaskRespVo.setText(removeSpacesInBrackets(collaborationTask.getText()));
            log.info("result bean :{}", JsonUtil.toJsonStr(cTaskRespVo));
        }
        log.info("result bean :{}", JsonUtil.toJsonStr(cTaskRespVo));

        return cTaskRespVo;
    }

    @Override
    public Map<String, Integer> getTaskNum(Long userId, Long postId) {
        /*int minThreshold = getThreshold(0);
        int maxThreshold = getThreshold(1);*/

        Map<String, Integer> map = new HashMap<>();
        // 查找最新待办统计
        Integer todoNum = collaborationTaskMapper.queryNumByStatus(List.of(1), userId, postId, null, null, null);
        map.put("1", todoNum);
        // 查找未及时回复问题统计
        Integer trackingIssuesNum =
            collaborationTaskMapper.queryNumByStatus(List.of(7), userId, postId, null, null, null);
        map.put("2", trackingIssuesNum);
        // 查找已逾期问题统计
        Integer completedNum = collaborationTaskMapper.queryNumByStatus(List.of(8), userId, postId, null, null, null);
        map.put("3", completedNum);
        // 跟踪问题统计
        Integer ignoreNum = collaborationTaskMapper.queryNumByStatus(List.of(2), userId, postId, null, null, null);
        map.put("4", ignoreNum);
        return map;
    }

    @Override
    public void updateGroupNameByGroupId(Long groupId, String groupName) {
        collaborationTaskMapper.updateGroupName(groupId, groupName);
    }

    @Override
    public Page<CollaborationOverdueVO> getOverdueList(String departmentCode, String startTime, String endTime, Integer pageSize, Integer pageNum) {
        long currentTimeMillis = System.currentTimeMillis();
        log.info("getOverdueList start time: {}",currentTimeMillis );
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(departmentCode);
        log.info("getOverdueList query im department end time: {}ms", System.currentTimeMillis()-currentTimeMillis);
        List<String> collect = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        // 根据查找到的
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        log.info("getOverdueList query db start time: {}ms", System.currentTimeMillis()-currentTimeMillis);
        Page<CollaborationOverdueVO> overdueList = collaborationTaskMapper.getOverdueList(new Page<>(pageNum, pageSize),collect, startTime, endTime);        log.info("getOverdueList query db end time: {}", System.currentTimeMillis());
        for (CollaborationOverdueVO vo : overdueList.getRecords()) {
            vo.setQuestionContent(removeSpacesInBrackets(vo.getQuestionContent()));
            String relatedUserIds = vo.getRelatedUserIds();
            String userName = vo.getPostUserNames();
            String[] split = relatedUserIds.split(",");
            String[] names = userName.split(",");
            for (int i = 0; i < split.length; i++){
                if(split[i].equals(vo.getUserId()+"")){
                    vo.setPostUserNames(names[i]);
                    break;
                }
            }
        }
        log.info("getOverdueList end time: {}ms", System.currentTimeMillis()-currentTimeMillis);
        return overdueList;
    }

    @Override
    public List<CollaborationTask> findList(List<Integer> statusList, List<Long> postIdList, String startTime, String endTime) {
        LambdaQueryWrapper<CollaborationTask> queryWrapper = new LambdaQueryWrapper<CollaborationTask>()
                // 不返回待分配的任务
                .ne(CollaborationTask::getStatus, -1)
                .in(CollectionUtils.isNotEmpty(statusList), CollaborationTask::getStatus, statusList)
                .in(CollectionUtils.isNotEmpty(postIdList), CollaborationTask::getPostId, postIdList);
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge(CollaborationTask::getGmtModified, DateFormatUtil.parseDate(startTime + " 00:00:00", DateFormatUtil.YYYY_MM_DD_HH_MM_SS));
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le(CollaborationTask::getGmtModified, DateFormatUtil.parseDate(endTime + " 23:59:59", DateFormatUtil.YYYY_MM_DD_HH_MM_SS));
        }
        return list(queryWrapper);
    }

    @Override
    public List<CollaborationTask> findList(Long groupId, Long postId, List<Integer> statusList) {
        LambdaQueryWrapper<CollaborationTask> queryWrapper = new LambdaQueryWrapper<CollaborationTask>()
                // 不返回待分配的任务
                .ne(CollaborationTask::getStatus, -1)
                .eq(Objects.nonNull(groupId), CollaborationTask::getGroupId, groupId)
                .eq(Objects.nonNull(postId), CollaborationTask::getPostId, postId)
                .in(CollectionUtils.isNotEmpty(statusList), CollaborationTask::getStatus, statusList);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CollaborationTaskDelayDto> dealUnassignTask(Long postId, Long userId) {
        // 协同岗上岗，更新当前协同岗任务的userId为自己，状态为-2,此为中间态，用于后续查询数据
        int assignTaskNum = baseMapper.assignTask(postId, userId, -1, -2);
        if (assignTaskNum <= 0) {
            return Collections.emptyList();
        }
        // 更新数量大于0，代表成功
        LambdaQueryWrapper<CollaborationTask> wrapper = new LambdaQueryWrapper<CollaborationTask>()
            .eq(CollaborationTask::getPostId, postId)
            .eq(CollaborationTask::getUserId, userId)
            .eq(CollaborationTask::getStatus, -2);
         List<CollaborationTask> taskList = baseMapper.selectList(wrapper);
         // 有事务保证，这里一定能查到数据
        List<CollaborationTaskDelayDto> dealyDtoList = new ArrayList<>();
        taskList.forEach(task -> {
            CollaborationTaskCO co = new CollaborationTaskCO();
            co.setToExecutorId(task.getFromUserId());
            co.setStatus(1);
            dealyDtoList.add(new CollaborationTaskDelayDto(co, task));
        });
        // 更新到最终态
        baseMapper.assignTask(postId, userId, -2, 1);
        return dealyDtoList;
    }

    @Override
    public void pushToDealyQueue(List<CollaborationTaskDelayDto> delayDtos) {
        if (CollectionUtils.isEmpty(delayDtos)) {
            return;
        }
        delayDtos.forEach( dto -> dealDelayTask(dto.getCo(), dto.getTask()));
    }


    @Scheduled(fixedDelay = 30000) // 每分钟执行一次
    public void delayTasks() {
        // 获取时间
        int minThreshold = getThreshold(0);
        int maxThreshold = getThreshold(1);
        long now = System.currentTimeMillis();
        long minFollowTime = now - TimeUnit.MINUTES.toMillis(minThreshold);
        long maxFollowTime = now - TimeUnit.MINUTES.toMillis(maxThreshold);
        log.info("minFollowTime:{},maxFollowTime:{}", minFollowTime, maxFollowTime);

        // 获取过期任务
        Set<CollaborationTaskCO> frontTasks =
            redisUtil.zRangeByScore(TASK_DELAY_QUEUE_1, 0, minFollowTime, CollaborationTaskCO.class);
        Set<CollaborationTaskCO> backTasks =
            redisUtil.zRangeByScore(TASK_DELAY_QUEUE_2, 0, maxFollowTime, CollaborationTaskCO.class);
        log.info("delayTasks:{}", frontTasks);
        log.info("backTasks:{}", backTasks);

        // 获取当前用户
        if (!frontTasks.isEmpty()) {
            frontTasks.forEach(task -> {
                CollaborationTask imCollaborationTask = collaborationTaskMapper.getCollaborationTask(task.getId());
                if (imCollaborationTask == null || !Objects.equals(imCollaborationTask.getStatus(), TODO_TASKS)) {
                    return;
                }
                List<Long> ids = new ArrayList<>();
                ids.add(task.getToExecutorId());
                var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(UPDATE_TASK).unicast()
                    .executorIds(ids).appKeys(Collections.singletonList("CDC-1000")).build()
                    .body(UPDATE_TASK, MSG_NOTIFY_TYPE, 1).build();
                streamBridge.send(MSG_TOPIC, cagentMqFrame);
                CollaborationTask updateTask = new CollaborationTask();
                log.debug("id:{}", task.getId());
                updateTask.setId(task.getId());
                updateTask.setStatus(DELAY_TASKS);
                updateTask.setGmtModified(new Date());
                collaborationTaskMapper.updateById(updateTask);
                log.debug("send cagent frontTasks:{}", cagentMqFrame);
            });

            // 删除任务
            redisUtil.zRem(TASK_DELAY_QUEUE_1, frontTasks);
        }

        if (!backTasks.isEmpty()) {
            backTasks.forEach(task -> {
                CollaborationTask imCollaborationTask = collaborationTaskMapper.getCollaborationTask(task.getId());
                if (imCollaborationTask!=null&&(Objects.equals(imCollaborationTask.getStatus(),
                    TODO_TASKS) || Objects.equals(imCollaborationTask.getStatus(), DELAY_TASKS))) {
                    List<Long> ids = new ArrayList<>();
                    ids.add(task.getToExecutorId());
                    // 定义1为里面外面都需要重新调用统计接口，2为单独调用外面的接口，3为单独调用里面的接口
                    var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(UPDATE_TASK).unicast()
                        .executorIds(ids).appKeys(Collections.singletonList("CDC-1000")).build()
                        .body(UPDATE_TASK, MSG_NOTIFY_TYPE, 1).build();
                    CollaborationTask updateTask = new CollaborationTask();
                    log.debug("id:{}", task.getId());
                    updateTask.setId(task.getId());
                    updateTask.setStatus(BACK_TASKS);
                    updateTask.setGmtModified(new Date());
                    collaborationTaskMapper.updateById(updateTask);

                    // 逾期添加预警记录
                    saveTasksExpiredRecord(task.getId());
                    streamBridge.send(MSG_TOPIC, cagentMqFrame);
                    log.debug("send cagent backTasks:{}", cagentMqFrame);
                    // 发送预警消息
                    sendPreWarningMessageService.sendTaskexpiredMessage(task);
                }
            });
            // 删除任务
            redisUtil.zRem(TASK_DELAY_QUEUE_2, backTasks);
        }
        log.debug("delayTasks complete!");
    }

    private void saveTasksExpiredRecord(Long taskId) {
        TasksExpired record = new TasksExpired();
        record.setId(idWorker.nextId());
        record.setTaskId(taskId);
        record.setCreateTime(new Date());

        tasksExpiredService.save(record);
    }

    private int getThreshold(Integer value) {
        String taskExpirationTime = globalsService.getGlobalsValueByName("TASK_EXPIRATION_TIME");
        int[] timeValues = Arrays.stream(taskExpirationTime.split(",")).map(String::trim).limit(2).mapToInt(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                log.debug("无效的时间格式: {}", s);
                return 0;
            }
        }).toArray();
        // 0是min，1是max
        return timeValues[value];
    }

}