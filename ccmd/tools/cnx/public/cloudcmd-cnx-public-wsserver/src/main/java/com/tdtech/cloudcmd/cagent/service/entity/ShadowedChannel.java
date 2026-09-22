package com.tdtech.cloudcmd.cagent.service.entity;

import com.tdtech.cloudcmd.cagent.service.UserInfo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShadowedChannel extends BaseChannel {

    private String ip;
    private Integer port;
    private LocalChannel localChannel;

    public ShadowedChannel(UserInfo userInfo, Long id, String ip, Integer port) {
        super(userInfo, id);
        this.ip = ip;
        this.port = port;
    }

    public ShadowedChannel(LocalChannel localChannel) {
        super(localChannel.getUserInfo(), localChannel.getId());
        this.localChannel = localChannel;
    }

    public boolean isLocal() {
        return localChannel != null;
    }

    @Override
    public String toString() {
        return "[" + super.getId() + "]" + (localChannel == null ? "(" + ip + ":" + port + ")" : "(LOCAL)") + "-"
            + super.getUserInfo().toString();
    }
}
