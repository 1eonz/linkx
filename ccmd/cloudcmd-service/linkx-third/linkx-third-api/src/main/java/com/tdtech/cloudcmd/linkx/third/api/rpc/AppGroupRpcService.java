package com.tdtech.cloudcmd.linkx.third.api.rpc;


import com.tdtech.cloudcmd.linkx.third.api.Vo.AppUsedRankingVo;

import java.util.List;

public interface AppGroupRpcService {

    List<AppUsedRankingVo> getAppUsedRanking(Long userId, Integer terminalType,Integer scope);
}
