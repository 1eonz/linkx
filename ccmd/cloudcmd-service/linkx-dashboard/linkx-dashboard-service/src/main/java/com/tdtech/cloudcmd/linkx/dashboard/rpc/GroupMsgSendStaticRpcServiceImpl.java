package com.tdtech.cloudcmd.linkx.dashboard.rpc;

import com.tdtech.cloudcmd.linkx.dashboard.entity.GroupMsgSendStatic;
import com.tdtech.cloudcmd.linkx.dashboard.service.IGroupMsgSendStaticService;
import com.tdtech.cloudcmd.service.rpc.GroupMsgSendStaticRpcService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import dto.GroupMsgSendStaticDto;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: S063874
 * @date: 2026-03-17 18:35
 */
@DubboService
public class GroupMsgSendStaticRpcServiceImpl implements GroupMsgSendStaticRpcService {

    @Resource
    private IGroupMsgSendStaticService groupMsgSendStaticService;

    @Override
    public void batchCreateGroupMsgSendStaticWithFilter(List<GroupMsgSendStaticDto> groupMsgSendStaticList) {
        List<GroupMsgSendStatic> groupMsgSendStatics = convertGroupMsgSendStatic(groupMsgSendStaticList);
        groupMsgSendStaticService.batchCreateGroupMsgSendStaticWithFilter(groupMsgSendStatics);
    }

    @Override
    public GroupMsgSendStaticDto getLatestByUserId(Long userId) {
        GroupMsgSendStatic groupMsgSendStatic = groupMsgSendStaticService.getLatestByUserId(userId);
        return BeanCopyUtils.copyBean(groupMsgSendStatic, GroupMsgSendStaticDto::new);
    }

    private List<GroupMsgSendStatic> convertGroupMsgSendStatic(List<GroupMsgSendStaticDto> groupMsgSendStaticDtoList) {
        return BeanCopyUtils.copyList(groupMsgSendStaticDtoList, GroupMsgSendStatic::new);
    }
}
