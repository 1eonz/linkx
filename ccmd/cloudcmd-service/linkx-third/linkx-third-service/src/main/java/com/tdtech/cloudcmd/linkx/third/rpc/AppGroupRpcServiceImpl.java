package com.tdtech.cloudcmd.linkx.third.rpc;

import com.tdtech.cloudcmd.linkx.third.api.Vo.AppUsedRankingVo;
import com.tdtech.cloudcmd.linkx.third.api.rpc.AppGroupRpcService;
import com.tdtech.cloudcmd.linkx.third.service.AppGroupService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@DubboService
public class AppGroupRpcServiceImpl implements AppGroupRpcService {
    @Autowired
    private AppGroupService appGroupService;
    @Override
    public List<AppUsedRankingVo> getAppUsedRanking(Long userId, Integer terminalType,Integer scope) {
        return appGroupService.getAppUsedRanking(userId, terminalType, scope);
    }
}
