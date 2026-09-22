package com.tdtech.cloudcmd.cagent.actuator;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Resource;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.service.GroupChannelTable;
import com.tdtech.cloudcmd.cagent.service.distributed.LeaderStatus;
import com.tdtech.cloudcmd.cagent.service.entity.LocalChannel;
import com.tdtech.cloudcmd.cagent.service.entity.ShadowedChannel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component("tablechannel")
@WebEndpoint(id = "tablechannel")
public class ActuatorConfiguration {
    @Resource
    private GroupChannelTable channelTable;

    @ReadOperation
    public ChannelTableEndPoint getCustomData() {
        LeaderStatus leaderStatus = channelTable.leaderStatus();
        Collection<LocalChannel> allLocal = channelTable.getAllLocal();
        if (leaderStatus == LeaderStatus.LEADER) {
            return new ChannelTableEndPoint(channelTable.getAllRemote(), allLocal, leaderStatus);
        }
        return new ChannelTableEndPoint(Collections.emptyList(), allLocal, leaderStatus);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelTableEndPoint {
        private Collection<ShadowedChannel> allRemote;
        private Collection<LocalChannel> allLocal;
        private LeaderStatus leaderStatus;
    }
}
