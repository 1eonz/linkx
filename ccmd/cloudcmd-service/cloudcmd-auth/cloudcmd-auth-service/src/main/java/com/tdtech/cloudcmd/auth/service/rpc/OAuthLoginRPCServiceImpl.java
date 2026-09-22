package com.tdtech.cloudcmd.auth.service.rpc;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginAPIDto;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginAPIInfo;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginDto;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginInfo;
import com.tdtech.cloudcmd.auth.entity.Application;
import com.tdtech.cloudcmd.auth.service.IApplicationService;
import com.tdtech.cloudcmd.auth.service.IOAuthLoginService;
import com.tdtech.cloudcmd.auth.service.OAuthLoginRPCService;

@Service
public class OAuthLoginRPCServiceImpl implements OAuthLoginRPCService {
    @Autowired
    private IApplicationService applicationService;
    @Autowired
    private IOAuthLoginService oAuthLoginService;

    @Override
    public OAuthLoginAPIDto oAuthLogin(OAuthLoginAPIInfo loginInfo, String appKey) {
        Application application = applicationService.getApplicationById(appKey);
        OAuthLoginInfo oAuthLoginInfo = new OAuthLoginInfo();
        BeanUtils.copyProperties(loginInfo, oAuthLoginInfo);
        OAuthLoginDto authLoginDto = oAuthLoginService.oAuthLogin(oAuthLoginInfo, application);
        OAuthLoginAPIDto oAuthLoginAPIDto = new OAuthLoginAPIDto();
        BeanUtils.copyProperties(authLoginDto, oAuthLoginAPIDto);
        return oAuthLoginAPIDto;
    }
}
