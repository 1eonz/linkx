package com.tdtech.cloudcmd.linkx.third.rpc;

import com.tdtech.cloudcmd.linkx.third.api.dto.LocationShareExitDto;
import com.tdtech.cloudcmd.linkx.third.api.rpc.LocationShareRpcService;
import com.tdtech.cloudcmd.linkx.third.service.LocationShareService;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareExitVo;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService
public class LocationShareRpcServiceImpl implements LocationShareRpcService {

    @Autowired
    private LocationShareService locationShareService;

    @Override
    public void exitLocationShare(LocationShareExitDto dto) {
        LocationShareExitVo vo = new LocationShareExitVo();
        vo.setUserId(dto.getUserId());
        vo.setExitType(dto.getExitType());
        vo.setExitDesc(dto.getExitDesc());
        locationShareService.exitLocationShare(dto.getShareId(), vo);
    }
}
