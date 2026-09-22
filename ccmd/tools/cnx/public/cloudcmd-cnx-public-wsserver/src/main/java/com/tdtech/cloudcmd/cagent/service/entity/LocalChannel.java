package com.tdtech.cloudcmd.cagent.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalChannel extends BaseChannel {

    @JsonIgnore
    private Channel channel;

}
