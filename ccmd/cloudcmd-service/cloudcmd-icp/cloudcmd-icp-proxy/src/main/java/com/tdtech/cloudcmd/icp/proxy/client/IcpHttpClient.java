package com.tdtech.cloudcmd.icp.proxy.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.icp.proxy.client.entity.CameraLevelResp;
import com.tdtech.cloudcmd.icp.proxy.client.entity.CameraResp;
import com.tdtech.cloudcmd.icp.proxy.client.entity.DepartmentResp;
import com.tdtech.cloudcmd.icp.proxy.client.entity.GisSubscriptionReq;
import com.tdtech.cloudcmd.icp.proxy.client.entity.LoginReq;
import com.tdtech.cloudcmd.icp.proxy.client.entity.LoginResp;
import com.tdtech.cloudcmd.icp.proxy.client.entity.ResourceSubscriptionReq;
import com.tdtech.cloudcmd.icp.proxy.client.entity.ResponseObject;
import com.tdtech.cloudcmd.icp.proxy.client.entity.UserResp;
import com.tdtech.cloudcmd.icp.proxy.conf.IcpProperties;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.PWICodeEnum;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.ResourceTypeEnum;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.net.http.HttpRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class IcpHttpClient {

    private final static String EXPIRED_TIME = "-1";

    private final IcpProperties icpProperties;
    private final HttpClient httpClient;

    public LoginResp unifiLogin() {
        var loginCO = new LoginReq().setLocalIp("127.0.0.1").setPassword(icpProperties.getIcpPwd());
        var unifiLoginURI = icpProperties.getUnifiLoginURI();
        var loginRO = httpClient.putJson(unifiLoginURI, new HashMap<>(), loginCO, LoginResp.class);
        log.info("uri:{} login:{} resp:{}", unifiLoginURI, loginCO, loginRO);
        return loginRO;
    }

    public String heartbeat(@NotNull String session) {
        var heartbeatURI = icpProperties.getHeartbeatURI();
        //ICP 这个接口 content-type header 大小写敏感 只有单独处理
        var respStr =
            httpClient.post(heartbeatURI, Map.of("session", session), HttpRequest.BodyPublishers.noBody(), null,
                String::new);
        log.info("uri:{} session:{} resp:{}", heartbeatURI, session, respStr);
        return respStr;
    }

    public void gisSub(@NotNull String session, @NotNull Collection<String> isdns) {
        GisSubscriptionReq resourceSubscriptionCO = new GisSubscriptionReq().setExpiredTime(EXPIRED_TIME)
            .setUeList(isdns.stream().map(GisSubscriptionReq.Isdn::new).collect(Collectors.toList()));
        var gisSubURI = icpProperties.getGisSubURI();
        var respStr = httpClient.postJson(gisSubURI, Map.of("session", session), resourceSubscriptionCO, String.class);
        log.info("gis sub:{} {} {} {}", session, gisSubURI, resourceSubscriptionCO, respStr);
    }

    public void statusSub(@NotNull String session, @NotNull Collection<String> isdns) {
        ResourceSubscriptionReq resourceSubscriptionCO = new ResourceSubscriptionReq().setType(ResourceTypeEnum.SUB)
            .setResList(isdns.stream().map(ResourceSubscriptionReq.Isdn::new).collect(Collectors.toList()));
        var statusSubURI = icpProperties.getOnlineStatusSubURI();
        var respStr =
            httpClient.postJson(statusSubURI, Map.of("session", session), resourceSubscriptionCO, String.class);
        log.info("status sub:{} {} {} {}", session, statusSubURI, resourceSubscriptionCO, respStr);
    }

    public List<DepartmentResp> getAllDepartment(@NotNull String session) {
        var departmentURI = icpProperties.getDepartmentURI();
        var respStr = httpClient.getJson(departmentURI, Map.of("session", session), String.class);
        log.info("list depatment:{} {} {}", session, departmentURI, respStr);
        if (respStr == null || respStr.isBlank()) {
            throw new BusinessException("查询ICP组织错误");
        }
        var responseObject = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<DepartmentResp>>() {
        });
        checkResponse(responseObject, "查询ICP组织错误");
        return responseObject.getList();
    }

    public List<CameraLevelResp> getAllCameraLevel(@NotNull String session) {
        var departmentURI = icpProperties.getCameraLevelURI();
        var respStr = httpClient.getJson(departmentURI, Map.of("session", session), String.class);
        log.info("list camera level:{} {}", session, departmentURI);
        if (respStr == null || respStr.isBlank()) {
            throw new BusinessException("查询ICP层级错误");
        }
        var responseObject = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<CameraLevelResp>>() {
        });
        checkResponse(responseObject, "查询ICP层级错误");
        return responseObject.getList();
    }

    public CameraResp getCameraByIsdn(@NotNull String session, @NotNull String isdn) {
        var cameraByIsdnURI = icpProperties.getCameraByIsdnURI(isdn);
        var respStr = httpClient.getJson(cameraByIsdnURI, Map.of("session", session), String.class);
        log.info("get camera:{} {}", session, cameraByIsdnURI);
        if (respStr == null || respStr.isBlank()) {
            throw new BusinessException("查询ICP摄像头错误");
        }
        var responseObject = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<CameraResp>>() {
        });
        checkResponse(responseObject, "查询ICP摄像头错误");
        return responseObject.getValue();
    }

    public ResponseObject<CameraResp> getCamera(@NotNull String session, @NotNull Integer offset, @NotNull Integer limit) {
        var cameraURI = icpProperties.getCameraURI(offset, limit);
        var respStr = httpClient.getJson(cameraURI, Map.of("session", session), String.class);
        log.info("list camera:{} {}", session, cameraURI);
        if (respStr == null || respStr.isBlank()) {
            throw new BusinessException("查询ICP摄像头错误");
        }
        var responseObject = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<CameraResp>>() {
        });
        checkResponse(responseObject, "查询ICP摄像头错误");
        return responseObject;
    }

    public List<CameraResp> getCameraByLevel(@NotNull String session, @NotNull String levelNumber,
        @NotNull Integer offset, @NotNull Integer limit) {
        var cameraByLevelURI = icpProperties.getCameraByLevelURI();
        Map<String, Object> req = Map.of("levelnumber", levelNumber, "offset", offset + "", "limit", limit + "");
        var respStr = httpClient.postJson(cameraByLevelURI, Map.of("session", session), req, String.class);
        log.info("list camera:{} {} {} {} {} {}", session, cameraByLevelURI, req, levelNumber, offset, limit);
        if (respStr == null || respStr.isBlank()) {
            throw new BusinessException("查询ICP摄像头错误");
        }
        var responseObject = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<CameraResp>>() {
        });
        checkResponse(responseObject, "查询ICP摄像头错误");
        return responseObject.getList();
    }

    public ResponseObject<UserResp> getUserByDepartment(@NotNull String session, String departmentId, @NotNull Integer offset,
        @NotNull Integer limit, String category) {
        var userByDepartmentURI = icpProperties.getUserByDepartmentURI(offset, limit, departmentId, category);
        var respStr = httpClient.getJson(userByDepartmentURI, Map.of("session", session), String.class);
        log.info("list user:{} {} {} {}", session, userByDepartmentURI, offset, limit);
        if (respStr == null || respStr.isBlank()) {
            throw new BusinessException("查询ICP用户错误");
        }
        var responseObject = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<UserResp>>() {
        });
        checkResponse(responseObject, "查询ICP用户错误");
        return responseObject;
    }

    public UserResp getUserByIsdn(@NotNull String session, @NotNull String isdn) {
        var userByIsdnURI = icpProperties.getUserByIsdnURI(isdn);
        var respStr = httpClient.getJson(userByIsdnURI, Map.of("session", session), String.class);
        log.info("get user:{} {}", session, userByIsdnURI);
        if (respStr == null || respStr.isBlank()) {
            throw new BusinessException("查询ICP用户错误");
        }
        var responseObject = JsonUtil.parseJson(respStr, new TypeReference<ResponseObject<UserResp>>() {
        });
        checkResponse(responseObject, "查询ICP用户错误");
        return responseObject.getValue();
    }

    private void checkResponse(ResponseObject<?> responseObject, String msg) {
        if (responseObject == null) {
            throw new BusinessException(msg);
        }
        if (responseObject.getCode() == null || responseObject.getCode() != PWICodeEnum.SUCCESS) {
            throw new BusinessException(msg);
        }
    }
}
