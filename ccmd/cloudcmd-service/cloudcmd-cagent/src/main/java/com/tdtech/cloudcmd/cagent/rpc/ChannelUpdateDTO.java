package com.tdtech.cloudcmd.cagent.rpc;

import com.tdtech.cloudcmd.cagent.service.entity.ShadowedChannel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChannelUpdateDTO {

    private ShadowedChannel channel;
    private ChannelUpdateActionEnum action;
}
