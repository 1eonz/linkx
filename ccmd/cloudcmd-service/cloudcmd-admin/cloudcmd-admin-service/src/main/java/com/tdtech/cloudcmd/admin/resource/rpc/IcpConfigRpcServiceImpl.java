package com.tdtech.cloudcmd.admin.resource.rpc;

import cloudcmd.dto.IcpConfigDto;
import cloudcmd.service.rpc.IcpConfigRpcService;
import com.tdtech.cloudcmd.admin.resource.entity.IcpConfig;
import com.tdtech.cloudcmd.admin.resource.service.IIcpConfigService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;

@DubboService
public class IcpConfigRpcServiceImpl implements IcpConfigRpcService {
    @Resource
    private IIcpConfigService icpConfigService;

    @Override
    public IcpConfigDto getIcpConfig() {
        IcpConfig icpConfig = icpConfigService.getIcpConfig();
        IcpConfigDto icpConfigDto = new IcpConfigDto();
        BeanUtils.copyProperties(icpConfig, icpConfigDto);
        return icpConfigDto;
    }
}
