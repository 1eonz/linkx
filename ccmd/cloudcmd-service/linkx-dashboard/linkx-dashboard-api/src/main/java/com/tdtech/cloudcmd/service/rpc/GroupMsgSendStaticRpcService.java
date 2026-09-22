package com.tdtech.cloudcmd.service.rpc;

import dto.GroupMsgSendStaticDto;

import java.util.List;

/**
 * @author: S063874
 * @date: 2026-03-17 18:30
 */
public interface GroupMsgSendStaticRpcService {

    void batchCreateGroupMsgSendStaticWithFilter(List<GroupMsgSendStaticDto> groupMsgSendStaticList);

    GroupMsgSendStaticDto getLatestByUserId(Long userId);
}
