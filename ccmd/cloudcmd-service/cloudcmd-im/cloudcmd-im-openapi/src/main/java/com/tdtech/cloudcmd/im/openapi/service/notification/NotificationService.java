package com.tdtech.cloudcmd.im.openapi.service.notification;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.tdtech.cloudcmd.im.openapi.repo.notification.Notification;
import com.tdtech.cloudcmd.im.openapi.repo.notification.NotificationMapper;
import com.tdtech.cloudcmd.im.openapi.repo.notification.NotificationTarget;
import com.tdtech.cloudcmd.im.openapi.repo.notification.NotificationTargetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private NotificationTargetMapper notificationTargetMapper;

    @DSTransactional
    public void saveNotification(String appId, String moduleName, String content,
                                 Integer collaborativeMsg, String url,
                                 Integer showInNotification,
                                 Long notificationId,
                                 List<Long> targetUserIds) {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setAppId(appId);
        notification.setModuleName(moduleName);
        notification.setContent(content);
        notification.setCollaborativeMsg(collaborativeMsg);
        notification.setUserIds(targetUserIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        notification.setUrl(url);
        notification.setShowInNotification(showInNotification);
        notification.setGmtCreated(new Date());
        notificationMapper.insert(notification);

        if (collaborativeMsg != null && collaborativeMsg == 1
                && targetUserIds != null && !targetUserIds.isEmpty()) {
            for (Long userId : targetUserIds) {
                NotificationTarget target = new NotificationTarget();
                target.setNotificationId(notification.getId());
                target.setUserId(userId);
                target.setRead(0);
                target.setGmtCreated(new Date());
                notificationTargetMapper.insert(target);
            }
        }

    }


}
