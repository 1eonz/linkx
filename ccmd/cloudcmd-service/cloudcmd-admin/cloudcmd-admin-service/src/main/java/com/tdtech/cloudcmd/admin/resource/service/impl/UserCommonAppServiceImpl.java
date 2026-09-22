package com.tdtech.cloudcmd.admin.resource.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tdtech.cloudcmd.admin.resource.entity.UserCommonAppDO;
import com.tdtech.cloudcmd.admin.resource.entity.dto.AppInfoDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoRespVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.UserCommonAppRespVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.UserCommonAppSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.mapper.UserCommonAppMapper;
import com.tdtech.cloudcmd.admin.resource.service.IAppInfoService;
import com.tdtech.cloudcmd.admin.resource.service.UserCommonAppService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.ListUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;

/**
 * @author lsc
 * @date 2025/7/17
 **/
@Service("userCommonAppService")
@Validated
@Slf4j
public class UserCommonAppServiceImpl implements UserCommonAppService {

    @Resource
    private UserCommonAppMapper mapper;
    @Resource
    @Lazy
    private IAppInfoService appInfoService;

    @Override
    public List<Long> save(UserCommonAppSaveReqVO createReqVO) {
        log.info("UserCommonApp save:{}", createReqVO);
        Integer terminalType = createReqVO.getTerminalType();
        if(terminalType == null){
            terminalType = 1;
        }
        List<Long> appIds = new ArrayList<>();
        String userId = createReqVO.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录或登录过期！");
        }
        //第一次进入后，用户初始化数据
        List<UserCommonAppDO> userCommonAppDOS = mapper.listByUserId(userId, terminalType);
        String initStatus = mapper.getInitStatus(userId, terminalType);
        if (ListUtils.isBlankList(userCommonAppDOS) && initStatus == null) {
            mapper.saveInitStatus(userId, terminalType);
        }
        deleteAllByUserId(userId, terminalType);
        if (createReqVO.getApps() != null && !createReqVO.getApps().isEmpty()) {
            for (UserCommonAppSaveReqVO.AppVO app : createReqVO.getApps()) {
                if (!exists(app.getId(), userId,terminalType)) {
                    if (appInfoService.exists(app.getId())) {
                        // 插入
                        if (CollectionUtils.isEmpty(appIds) || !appIds.contains(app.getId())) {
                            UserCommonAppDO data = new UserCommonAppDO();
                            data.setAppId(app.getId());
                            data.setUserId(userId);
                            data.setSort(app.getSort());
                            data.setTerminalType(terminalType);
                            mapper.insert(data);
                            appIds.add(data.getId());
                        }
                    }
                }
            }
        }
        // 返回
        return appIds;
    }

    @Override
    public Boolean getInitStatus(String userId, Integer terminalType) {
        if(terminalType == null){
            terminalType = 1;
        }
        String initStatus = mapper.getInitStatus(userId, terminalType);
        if (initStatus != null) {
            return true;
        }
        return false;
    }

    @Override
    public void delete(Long id, Integer terminalType) {
        if(terminalType == null){
            terminalType = 1;
        }
        // 校验存在
        validateExists(id);
        Long userId = SecurityUtils.getUser().getUserId();
        // 删除
        mapper.delete(new LambdaUpdateWrapper<UserCommonAppDO>().eq(UserCommonAppDO::getId, id)
                .eq(UserCommonAppDO::getUserId, userId)
                .eq(UserCommonAppDO::getTerminalType, terminalType));
    }

    @Override
    public void deleteAllByAppId(Long id) {
        // 删除
        mapper.delete(new LambdaUpdateWrapper<UserCommonAppDO>().eq(UserCommonAppDO::getAppId, id));
    }

    @Override
    public void deleteAllByUserId(String userId,Integer terminalType) {
        // 删除
        mapper.delete(new LambdaUpdateWrapper<UserCommonAppDO>().eq(UserCommonAppDO::getUserId, userId).eq(UserCommonAppDO::getTerminalType, terminalType));
    }

    @Override
    public List<UserCommonAppRespVO> getMy(String userId, Integer terminalType,Integer scope) {
        if(terminalType == null){
            terminalType = 1;
        }
        List<UserCommonAppDO> list = mapper.listByUserId(userId, terminalType);
        List<UserCommonAppRespVO> listVO = BeanCopyUtils.copyList(list, UserCommonAppRespVO::new);
        if (ListUtils.isNotBlankList(listVO)) {
            List<AppInfoDO> apps = appInfoService.selectList(scope);
            for (UserCommonAppRespVO userCommonAppRespVO : listVO) {
                for (AppInfoDO appInfoDO : apps) {
                    if (appInfoDO.getId().equals(userCommonAppRespVO.getAppId())) {
                        userCommonAppRespVO.setApp(BeanCopyUtils.copyBean(appInfoDO, AppInfoRespVO::new));
                        break;
                    }
                }
            }
        }
        return listVO;
    }

    private void validateExists(Long id) {
        if (mapper.selectById(id) == null) {
            throw new RuntimeException("用户常用应用不存在");
        }
    }

    private boolean exists(Long appId, String userId,Integer terminalType) {
        var count = mapper.selectCount(new LambdaQueryWrapper<UserCommonAppDO>()
                .eq(UserCommonAppDO::getAppId, appId)
                .eq(UserCommonAppDO::getUserId, userId)
                .eq(UserCommonAppDO::getTerminalType, terminalType));
        return count > 0;
    }

}
