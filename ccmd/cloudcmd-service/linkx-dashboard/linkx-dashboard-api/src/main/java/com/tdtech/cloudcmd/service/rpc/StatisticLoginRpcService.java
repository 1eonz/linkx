package com.tdtech.cloudcmd.service.rpc;

import dto.StatisticLoginDTO;

/**
 * LINKX增加日活数据统计
 */
public interface StatisticLoginRpcService {

    void statisticLogin(StatisticLoginDTO... statisticLogins);
}
