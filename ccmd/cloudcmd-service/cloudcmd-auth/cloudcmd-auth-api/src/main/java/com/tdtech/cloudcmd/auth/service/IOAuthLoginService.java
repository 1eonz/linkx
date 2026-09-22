package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.dto.*;
import com.tdtech.cloudcmd.bean.R;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mWX556161
 * @date 2020/11/18 16:00
 */
public interface IOAuthLoginService {

    /**
     * 登录
     *  @param loginInfo
     * @param applicationId
     * @param request
     */
    OAuthLoginDto oAuthLogin(OAuthLoginInfo loginInfo, Long applicationId, HttpServletRequest request);

    /**
     * 登出
     * 
     * @param accessToken
     */
    void oAuthLogout(String accessToken,String ip);

    /**
     * token刷新
     * 
     * @param authRefreshDto
     * @return
     */
    OAuthRefreshDto oAuthRefreshToken(OAuthRefreshInfo authRefreshDto);

    /**
     *
     * @param oAuthDemsTokenRefreshInfo
     * @return
     */
    OAuthDemsTokenRefreshDto oAuthDemsRefreshToken(OAuthDemsTokenRefreshInfo oAuthDemsTokenRefreshInfo);

    /**
     * 检测所有token有效性
     */
    void validTokens();

    /**
     * 检测用户心跳
     */
    R oauthKeepalive(String accessToken);

    /**
     * 修改用户密码
     * 
     * @param pwdDto
     */
    void changePassword(OAuthPwdDto pwdDto, String ip,String appKey);

    void kickOutUserForRPC(String userId);


}
