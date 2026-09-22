package com.tdtech.cloudcmd.cagent.rpc;

import java.util.List;

import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;

public interface GroupRpcClient {
    List<ChannelUpdateDTO> getAllLocalChannel();

    void onDistributeTask(TaskDTO task);

    void reportChannelUpdate(List<ChannelUpdateDTO> channelList);

    void reportTask(CdcFrame data);

}
