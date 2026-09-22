package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.dto.OAuthLoginAPIDto;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginAPIInfo;

public interface OAuthLoginRPCService {
    OAuthLoginAPIDto oAuthLogin(OAuthLoginAPIInfo loginInfo, String appKey);
}
