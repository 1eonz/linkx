package com.tdtech.cloudcmd.im.jingxin.server.aiclient.group;

import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;

import java.util.List;

public interface GroupAiWsClientInterface {

    void addGroupAi(AiVirtualUser user);

    void addGroupAi(List<AiVirtualUser> users);

    void removeGroupAi(String clientId);

    void removeGroupAi(List<String> clientIds);
}
