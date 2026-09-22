package com.tdtech.cloudcmd.im.jingxin.server.service;

import java.util.List;

public interface INotificationService {

    List<String> listModuleNames();

    int countUnread(String moduleName);

    int countRead(String moduleName);

    int markRead(Long notificationId);
}
