package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import cloudcmd.service.rpc.AppInfoRpcService;
import cloudcmd.vo.UserCommonAppResp4RpcVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile.UserProfileCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.userProfile.UserProfileVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserProfile;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.UserProfileMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.UserProfileService;
import com.tdtech.cloudcmd.linkx.third.api.Vo.AppUsedRankingVo;
import com.tdtech.cloudcmd.linkx.third.api.rpc.AppGroupRpcService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.DateUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 用户偏好信息表 服务实现类
 *
 * @author wb
 * @since 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {

    @Autowired
    private IdWorker idWorker;

//    @DubboReference(url = "172.24.64.95:18080")
    @DubboReference
    private AppInfoRpcService appInfoRpcService;
//    @DubboReference(url = "172.24.64.95:18081")
    @DubboReference
    private AppGroupRpcService appGroupRpcService;

    @Override
    public void createOrUpdate(UserProfileCreateReq createReq) {
        UserProfile existing = getByUserId(createReq.getUserId());
        UserProfile userProfile = new UserProfile();
        userProfile.setAppSortType(createReq.getType());
        userProfile.setUserId(createReq.getUserId());
        if (existing != null) {
            userProfile.setId(existing.getId());
            updateById(userProfile);
            return;
        }
        userProfile.setId(idWorker.nextId());
        userProfile.setGmtCreated(DateUtils.of(new Date()));
        save(userProfile);
    }

    @Override
    public UserProfileVo getProfile(Long userId, Integer terminalType,Integer scope) {
        UserProfile profile = getByUserId(userId);
        UserProfileVo userProfileVo = new UserProfileVo();
        List<UserCommonAppResp4RpcVo> defaultApps = appInfoRpcService.getMy(String.valueOf(userId), terminalType,scope);
        List<JSONObject> defaultAppsJson = JSONObject.parseArray(JSONObject.toJSONString(defaultApps), JSONObject.class);
        // 默认自定义排序
        if (Objects.isNull(profile)) {
            userProfileVo.setUserId(userId);
            userProfileVo.setAppCustomSort(defaultAppsJson);
            userProfileVo.setAppSortType(2);
            return userProfileVo;
        }
        BeanCopyUtils.copyBean(profile, userProfileVo);
        if (Objects.equals(1, profile.getAppSortType())) {
            List<AppUsedRankingVo> appUsedRanking = appGroupRpcService.getAppUsedRanking(userId, terminalType, scope);
            List<JSONObject> appUsedRankingJson = JSONObject.parseArray(JSONObject.toJSONString(appUsedRanking), JSONObject.class);
            userProfileVo.setAppCustomSort(appUsedRankingJson);
        } else {
            userProfileVo.setAppCustomSort(defaultAppsJson);
        }
        return userProfileVo;
    }

    @Override
    public Integer getSortedType(Long userId) {
        UserProfile profile = getByUserId(userId);
        if (profile == null) {
            return null;
        }
        return profile.getAppSortType();
    }

    @Override
    public UserProfile getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public UserProfile getByUserId(Long userId) {
        return lambdaQuery()
                .eq(UserProfile::getUserId, userId)
                .one();
    }

    @Override
    public void updateAppSortType(Long userId, Integer appSortType) {
        lambdaUpdate()
                .eq(UserProfile::getUserId, userId)
                .set(UserProfile::getAppSortType, appSortType)
                .update();
    }

    @Override
    public void updateAppCustomSort(Long userId, List<Long> appCustomSort) {
        lambdaUpdate()
                .eq(UserProfile::getUserId, userId)
                .set(UserProfile::getAppCustomSort, appCustomSort)
                .update();
    }
}
