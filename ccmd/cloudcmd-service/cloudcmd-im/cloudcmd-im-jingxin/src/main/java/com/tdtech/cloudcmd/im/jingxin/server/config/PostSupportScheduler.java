package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImMessageRequest;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.TxtMsgVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserListVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutySchedule;
import com.tdtech.cloudcmd.im.jingxin.server.entity.SystemMsg;
import com.tdtech.cloudcmd.im.jingxin.server.enums.NotifyTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.SwitchTypeEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationAttendanceSwitchMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostGroupMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class PostSupportScheduler {



    @Resource
    private ImService imService;
    @Resource
    private  CollaborationPostGroupMapper collaborationPostGroupMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledSyncPost() {
        // 从im查询所有协同岗
        // 更新协同岗支撑人员
        log.info("同步协同岗支撑人员开始");
        long l = System.currentTimeMillis();
        List<UserListVo> userListVos = imService.queryCollaborationPost();
        int allcount = 0;
        log.info("查询到{}个协同岗", userListVos.size());
        for (UserListVo userListVo : userListVos) {
            List<UserListVo.BindingGroup> groups = userListVo.getGroups();
            if (CollectionUtils.isNotEmpty(groups)) {
                String postId = userListVo.getId();
                for (UserListVo.BindingGroup group : groups) {
                    if (group.hasSupportUser()) {
                        int count = collaborationPostGroupMapper.updateByPostIdAndGroupId(Long.parseLong(postId), group.getSupportUserId(),group.getGroupId());
                        allcount += count;
                    }
                }
            }
        }
        log.info("同步协同岗支撑人员结束，共更新{}条,耗时{}ms", allcount, System.currentTimeMillis() - l);
    }
}
