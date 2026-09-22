package com.tdtech.cloudcmd.cagent.server;

import com.tdtech.cloudcmd.cagent.service.UserInfo;

import io.netty.channel.Channel;

/**
 * Channel Status Interface, triggered when channel status change happens in random order chain.
 * 
 * None Exception will be caught, so when an Exception got throw, the trigger chain got broken.
 *
 * When not expected to break the chain, catching All Exception is Required.
 */
public interface ChannelStatusHandler {

    /**
     * triggered when auth succeed, when an Exception thrown, channel will be closed.
     */
    void onChannelAuth(Channel channel, UserInfo userInfo);

    void onChannelInactive(Channel channel, UserInfo userInfo);

    void onChannelHeartBeat(Channel channel, UserInfo userInfo);
}