package com.tdtech.cloudcmd.icp.proxy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tdtech.cloudcmd.icp.proxy.client.IcpHttpClient;
import com.tdtech.cloudcmd.icp.proxy.client.entity.ResponseObject;
import com.tdtech.cloudcmd.icp.proxy.client.entity.UserResp;
import com.tdtech.cloudcmd.icp.proxy.conf.IcpProperties;
import com.tdtech.cloudcmd.icp.proxy.util.SyncUtil;
import com.tdtech.cloudcmd.icp.proxy.ws.event.HttpAuthSucceedEvent;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.PWICodeEnum;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthService {

    @Resource
    private IcpHttpClient icpHttpClient;
    @Resource
    private IcpProperties icpProperties;
    @Resource
    private ApplicationEventPublisher publisher;

    private volatile String session;

    private String isdn;

    private final Object sessionInitLock = new Object();
    private final Cache<String, UserResp> cache =
        CacheBuilder.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).build();

    private String sendLoginRequest() {
        try {
            // login
            var loginResp = icpHttpClient.unifiLogin();
            if (!PWICodeEnum.LOGIN_SUCCESS.getCode().equals(loginResp.getRsp())) {
                SyncUtil.setConnectStatus(SyncUtil.ERROR_CODE, "对接通信服务器失败，请检查相关配置！");
                throw new RuntimeException("fail" + loginResp);
            }
            // save session
            this.session = loginResp.getSession();
            this.isdn = icpProperties.getIcpAccount();
            log.info("resp :{},gpsisdn:{}", loginResp, isdn);
            // init websocket connection
            if (session == null || session.isBlank()) {
                SyncUtil.setConnectStatus(SyncUtil.ERROR_CODE, "对接通信服务器失败，请检查相关配置！");
                throw new RuntimeException("login session not found" + loginResp);
            }
            publisher.publishEvent(new HttpAuthSucceedEvent(session));
            SyncUtil.clearConnectStatus();
            return this.session;
        } catch (Exception e) {
            // 获取 -D Session 失败
            SyncUtil.setConnectStatus(SyncUtil.ERROR_CODE, "对接通信服务器失败，请检查相关配置！");
            throw new RuntimeException("login error", e);
        }
    }

    public void clearSession() {
        this.session = null;
        this.isdn = null;
        this.cache.cleanUp();
    }

    public String getSessionStr() {
        if (this.session == null || !Objects.equals(this.isdn, icpProperties.getIcpAccount())) {
            synchronized (sessionInitLock) {
                if (this.session == null || !Objects.equals(this.isdn, icpProperties.getIcpAccount())) {
                    return sendLoginRequest();
                }
            }
        }
        return this.session;
    }

    @SneakyThrows
    public UserResp getLoginUser() {
        return cache.get("any", () -> {
            var sessionStr = getSessionStr();
            var userByIsdn = icpHttpClient.getUserByIsdn(sessionStr, icpProperties.getIcpAccount());
            if (userByIsdn == null) {
                throw new RuntimeException(
                    "user by isdn not found:[" + icpProperties.getIcpAccount() + "] with session:[" + sessionStr + "]");
            }
            return userByIsdn;
        });
    }

    @Scheduled(initialDelay = 30000L, fixedDelay = 30000L)
    public void heartBeat() {
        if (session == null || session.isBlank()) {
            getSessionStr();
            return;
        }
        var respStr = icpHttpClient.heartbeat(session);
        var resp = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<Void>>() {
        });
        if (resp == null || resp.getCode() == null) {
            log.warn("unknown response code in payload:{}", respStr);
            clearSession();
            getSessionStr();
            return;
        }
        switch (resp.getCode()) {
            case SUCCESS: {
                // DO NOTHING
                break;
            }
            case AUTH_FAIL: {
                // clear session then trigger to login again
                clearSession();
                getSessionStr();
                break;
            }
            default: {
                throw new RuntimeException("heartbeat fail:" + respStr);
            }
        }
    }

}
