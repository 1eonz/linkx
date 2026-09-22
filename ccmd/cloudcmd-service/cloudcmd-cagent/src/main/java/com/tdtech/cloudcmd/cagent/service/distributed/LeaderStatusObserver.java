package com.tdtech.cloudcmd.cagent.service.distributed;

public interface LeaderStatusObserver {

    void onInit();

    void onLeader();

    void onFollower(String leaderHost);

}
