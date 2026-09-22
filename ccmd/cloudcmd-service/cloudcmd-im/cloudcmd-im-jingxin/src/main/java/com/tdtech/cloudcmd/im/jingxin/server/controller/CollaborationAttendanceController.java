package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceUserDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationAttendanceService;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author syf
 * @date 2025/7/16
 **/
@Tag(name = "协同岗勤务")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/attendance")
@RequiredArgsConstructor
@Validated
public class CollaborationAttendanceController {

    private static final String EXPORT_FILE_NAME = "协同岗人员上下岗记录";
    @Resource
    private CollaborationAttendanceService collaborationAttendanceService;
    @Resource
    private CollaborationPostService collaborationPostService;

    @Resource
    private ReportUtil reportUtil;

    @GetMapping("/heartbeat/{userId}")
    public R<Void> heartbeat(@PathVariable("userId") Long userId) {
        collaborationAttendanceService.heartbeat(userId);
        return R.success();
    }

    @GetMapping("/page")
    public Page<CollaborationAttendance> getPage(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String postName,
        @RequestParam(required = false) String orgName, @RequestParam(required = false) Long orgId,
        @RequestParam(required = false) String personName, @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime) {
        return collaborationAttendanceService.getPage(pageNum, pageSize, postName, orgName, orgId, personName,
            startTime, endTime);
    }

    @GetMapping("/export")
    public void exportToExcel(@RequestParam(required = false) String postName,
        @RequestParam(required = false) String orgName, @RequestParam(required = false) String orgId,
        @RequestParam(required = false) String personName, @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime, HttpServletResponse response) throws IOException {
        collaborationAttendanceService.exportToExcel(EXPORT_FILE_NAME, postName, orgName, orgId, personName, startTime,
            endTime, response);
    }

    /**
     * 切换上下岗状态
     * 
     * @return 切换成功与否
     */
    @PostMapping("/switchStatus")
    public R<Boolean> switchStatus(@Valid @RequestBody CollaborationAttendanceUserDTO user) {
        log.info("switchStatus receive Time:{}", System.currentTimeMillis());
        List<CollaborationPost> collaborationPosts =
            collaborationPostService.listByUserId(String.valueOf(user.getUserId()));
        if (CollectionUtils.isEmpty(collaborationPosts)) {
            return R.failure("当前用户未绑定协同岗，请检查数据是否异常");
        }
        collaborationAttendanceService.saveAttendance(collaborationPosts, user);
        return R.success(true);
    }

    /**
     * 检测用户是否绑定协同岗
     * 
     * @return 该用户是否绑定协同岗
     */
    @GetMapping("/getSwitchStatus")
    public R<Map<String, Object>> getSwitchStatus(@NotNull(message = "用户id不能为空") Long userId) {
        Map<String, Object> resultMap = new HashMap<>();
        List<CollaborationPost> collaborationPosts = collaborationPostService.listByUserId(String.valueOf(userId));
        boolean hasBonded = false;
        boolean isSwitchOpen = false;
        if (CollectionUtils.isNotEmpty(collaborationPosts)) {
            hasBonded = true;
        }
        resultMap.put("bondedStatus", hasBonded);
        CollaborationAttendanceSwitch attendanceSwitch = collaborationAttendanceService.getSwitchStatusByPerson(userId);
        if (attendanceSwitch != null) {
            isSwitchOpen = attendanceSwitch.getSwitchStatus() == 0;
        }
        resultMap.put("switchStatus", isSwitchOpen);
        return R.success(resultMap);
    }

    /**
     * 统计该协同岗当前剩余人数
     * 
     * @return 该协同岗剩余人数
     */
    @GetMapping("/getLastNum")
    public R<Map<String, Object>> getLastNum(@NotNull(message = "用户id不能为空") Long userId) {
        List<CollaborationPost> collaborationPosts = collaborationPostService.listByUserId(String.valueOf(userId));
        if (CollectionUtils.isEmpty(collaborationPosts)) {
            return R.failure("当前用户未绑定协同岗，请检查数据是否异常");
        }
        // 优先从普通协同岗获取人数
        collaborationPosts = collaborationPosts.stream().sorted(Comparator.comparing(CollaborationPost::getType)).collect(Collectors.toList());
        log.info("getLastNum collaborationPosts: {}", collaborationPosts);
        return R.success(collaborationAttendanceService.getLastNumByPostId(collaborationPosts.get(0).getId(), userId));
    }
    /**
     * 统计该协同岗当前剩余人数
     *
     * @return 该协同岗剩余人数
     */
    @GetMapping("/getOnline")
    public R<List<ImUserDto>> getOnlineByPostId(@RequestParam("postId") Long postId) {
        return R.success(collaborationAttendanceService.getOnlineByPostId(postId));
    }

    @PostMapping("/admin/offline")
    public R<Boolean> offline(@Valid @RequestBody CollaborationAttendanceUserDTO user) {
        log.info("switchStatus receive Time:{}", System.currentTimeMillis());
        List<CollaborationPost> collaborationPosts =
                collaborationPostService.listByUserId(String.valueOf(user.getUserId()));
        if (CollectionUtils.isEmpty(collaborationPosts)) {
            return R.failure("当前用户未绑定协同岗，请检查数据是否异常");
        }
        if(collaborationPosts.size() == 1){
            // 如果一个人只支持一个协同岗，则直接按照之前的逻辑下岗即可
            collaborationAttendanceService.saveAttendance(collaborationPosts, user);
        }else{
            collaborationAttendanceService.adminOffline(collaborationPosts, user);
        }
        // 记录misp日志
        try {
            OperationLog log = new OperationLog(OperationTypeEnum.COLLABORATION_POST_DELETE);
            log.setOperation("操作了" + user.getPostName() + "下岗");
            UserInfo userInfo = SecurityUtils.getUser();
            if (Objects.nonNull(userInfo)) {
                log.setOperator(userInfo.getUserName());
            }
            reportUtil.saveOperationLog(log);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return R.success(true);
    }
}
