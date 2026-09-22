package com.tdtech.cloudcmd.linkx.dashboard.rpc;

import com.tdtech.cloudcmd.linkx.dashboard.service.IStatisticLoginService;
import com.tdtech.cloudcmd.service.rpc.StatisticLoginRpcService;
import dto.StatisticLoginDTO;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * LINKX增加日活数据统计
 */
@DubboService
public class StatisticLoginRpcServiceImpl implements StatisticLoginRpcService {
    @Resource
    private IStatisticLoginService statisticLoginService;

    @Override
    public void statisticLogin(StatisticLoginDTO... statisticLogins) {
        statisticLoginService.statisticLogin(statisticLogins);
    }
}
