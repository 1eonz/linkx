package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.dto.OAuthLoginDto;
import com.tdtech.cloudcmd.auth.dto.OAuthLoginInfo;
import com.tdtech.cloudcmd.auth.dto.OAuthPwdDto;
import com.tdtech.cloudcmd.auth.dto.OAuthRefreshDto;
import com.tdtech.cloudcmd.auth.dto.OAuthRefreshInfo;
import com.tdtech.cloudcmd.auth.entity.Application;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;

/**
 * @author mWX556161
 * @date 2020/11/18 16:00
 */
public interface IOAuthLoginService {

    /**
     * 登录
     * 
     * @param loginInfo
     * @param applicationId
     */
    OAuthLoginDto oAuthLogin(OAuthLoginInfo loginInfo, Application applicationId);

    /**
     * 登出
     * 
     * @param accessToken
     */
    void oAuthLogout(UserInfo user, String accessToken, String ip);

    /**
     * token刷新
     * 
     * @param authRefreshDto
     * @return
     */
    OAuthRefreshDto oAuthRefreshToken(OAuthRefreshInfo authRefreshDto);

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
    void changePassword(OAuthPwdDto pwdDto, String ip);

    OAuthLoginDto tokenLogin(OAuthLoginInfo loginInfo, Application application);

    OAuthLoginDto h5Login(OAuthLoginInfo loginInfo, Application application);

    void checkLimitUserCount(Integer ready2SaveCount);

    void kickOutUserForRPC(String userId);


    String getLoginMessage();
}
