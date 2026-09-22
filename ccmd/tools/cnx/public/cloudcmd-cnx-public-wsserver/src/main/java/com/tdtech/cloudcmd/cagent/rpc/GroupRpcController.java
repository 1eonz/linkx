package com.tdtech.cloudcmd.cagent.rpc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.cagent.conf.SystemProperties;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;
import com.tdtech.cloudcmd.cagent.service.GroupChannelTable;
import com.tdtech.cloudcmd.cagent.service.MessageService;
import com.tdtech.cloudcmd.cagent.service.entity.LocalChannel;
import com.tdtech.cloudcmd.cagent.service.entity.ShadowedChannel;
import com.tdtech.cloudcmd.cagent.service.outbound.RedisCachedMessageWriter;

@RestController
public class GroupRpcController implements GroupRpcClient {
    @Resource
    private GroupChannelTable channelTable;
    @Resource
    private RedisCachedMessageWriter redisCachedMessageWriter;
    @Resource
    private SystemProperties systemProperties;
    @Resource
    private MessageService messageService;

    // ************* ENABLE AS FOLLOWER *****************//
    @Override
    @GetMapping("channel/all")
    public List<ChannelUpdateDTO> getAllLocalChannel() {
        Collection<LocalChannel> allLocal = channelTable.getAllLocal();
        if (allLocal != null && !allLocal.isEmpty()) {
            return allLocal.stream()
                .map(a -> new ShadowedChannel(a.getUserInfo(), a.getId(), systemProperties.getIp(),
                    systemProperties.getPort()))
                .map(a -> new ChannelUpdateDTO(a, ChannelUpdateActionEnum.SAVE)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    @PostMapping("task/distribute")
    public void onDistributeTask(@RequestBody TaskDTO task) {
        Objects.requireNonNull(task.getTarget());
        Objects.requireNonNull(task.getCdcFrame());
        redisCachedMessageWriter.sendByDataStrategy(task.getTarget(), task.getCdcFrame());
    }

    // ***************** ENABLE AS LEADER *********************//
    @Override
    @PostMapping("channel/report")
    public void reportChannelUpdate(@RequestBody List<ChannelUpdateDTO> channelList) {
        channelTable.followerChannelUpdate(channelList);
    }

    @Override
    @PostMapping("task/report")
    public void reportTask(@RequestBody CdcFrame data) {
        messageService.distribute(data);
    }

}
