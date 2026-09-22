package com.tdtech.cloudcmd.admin.resource.rpc;

import cloudcmd.dto.SystemConfigDto;
import cloudcmd.service.rpc.SystemConfigRpcService;
import com.tdtech.cloudcmd.admin.resource.entity.SystemConfig;
import com.tdtech.cloudcmd.admin.resource.service.ISystemConfigService;
import com.tdtech.cloudcmd.util.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

@DubboService
public class SystemConfigRpcServiceImpl implements SystemConfigRpcService {
    @Resource
    private ISystemConfigService systemConfigService;

    @Override
    public String getValueByKey(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return systemConfigService.getConfigValue(key);
    }

    @Override
    public void saveOrUpdate(SystemConfigDto configDto) {
        SystemConfig config = systemConfigService.getSystemConfigByKey(configDto.getKey());
        if (config != null) {
            config.setValue(configDto.getValue());
            systemConfigService.updateSystemConfig(config);
            return;
        }
        config = new SystemConfig();
        config.setKey(configDto.getKey());
        config.setValue(configDto.getValue());
        systemConfigService.createSystemConfig(config);
    }
}
