package com.tdtech.cloudcmd.linkx.third.api.Vo;

import cloudcmd.vo.UserCommonAppResp4RpcVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppUsedRankingVo extends UserCommonAppResp4RpcVo {
    /*
    应用使用次数
     */
    private Integer count = 0;
}
