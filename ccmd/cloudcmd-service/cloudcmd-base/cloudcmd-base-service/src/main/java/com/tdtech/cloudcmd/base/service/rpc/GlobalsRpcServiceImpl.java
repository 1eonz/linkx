package com.tdtech.cloudcmd.base.service.rpc;

import java.util.ArrayList;
import java.util.List;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.tdtech.cloudcmd.base.api.param.GlobalsDto;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.base.config.DisplayTarget;
import com.tdtech.cloudcmd.base.entity.Globals;
import com.tdtech.cloudcmd.base.service.ICacheService;
import com.tdtech.cloudcmd.base.service.IGlobalsService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author mWX556161
 * @date 2020/6/16 14:35
 */
@DubboService
@Slf4j
public class GlobalsRpcServiceImpl implements GlobalsRpcService {

    @Autowired
    private ICacheService cacheService;
    @Autowired
    private IGlobalsService globalsService;
    @Autowired
    private DisplayTarget displayTarget;

    @Override
    public String getGlobalsValueByName(String name) {
        return cacheService.getGlobalsValue(name);
    }

    @Override
    public List<GlobalsDto> getGlobalsList() {
        List<Globals> globalsList = globalsService.getGlobalsList();
        List<GlobalsDto> globalsDtos = new ArrayList<>();
        for (Globals globals : globalsList) {
            GlobalsDto globalsDto = new GlobalsDto();
            globalsDto.setName(globals.getName());
            globalsDto.setValue(globals.getValue());
            globalsDtos.add(globalsDto);
        }
        return globalsDtos;
    }

    @Override
    public Integer getDisplayTarget() {
        return displayTarget.displayTarget==null?0:displayTarget.displayTarget;
    }

}
