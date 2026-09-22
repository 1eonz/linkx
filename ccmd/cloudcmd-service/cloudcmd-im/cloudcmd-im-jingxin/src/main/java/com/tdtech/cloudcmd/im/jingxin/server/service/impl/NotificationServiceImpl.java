package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.server.service.INotificationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.NotificationMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.NotificationTargetMapper;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class NotificationServiceImpl implements INotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Resource
    private NotificationTargetMapper notificationTargetMapper;

    @Override
    public List<String> listModuleNames() {
        Long userId = currentUserId();
        return notificationMapper.selectModuleNamesByUserId(userId);
    }

    @Override
    public int countUnread(String moduleName) {
        Long userId = currentUserId();
        return notificationTargetMapper.countUnreadByUserIdAndModuleName(userId, moduleName);
    }

    @Override
    public int countRead(String moduleName) {
        Long userId = currentUserId();
        return notificationTargetMapper.countReadByUserIdAndModuleName(userId, moduleName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markRead(Long notificationId) {
        Long userId = currentUserId();
        return notificationTargetMapper.markReadByUserIdAndNotificationId(userId, notificationId);
    }

    private Long currentUserId() {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        return user.getUserId();
    }
}
