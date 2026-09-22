package com.tdtech.cloudcmd.admin.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.admin.resource.entity.AppInfo;
import com.tdtech.cloudcmd.admin.resource.entity.dto.AppInfoDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoUpdateStatusReqVO;
import com.tdtech.cloudcmd.admin.resource.mapper.AppInfoMapper;
import com.tdtech.cloudcmd.admin.resource.service.IAppInfoService;
import com.tdtech.cloudcmd.admin.resource.service.UserCommonAppService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Slf4j
@Service
public class AppInfoServiceImpl implements IAppInfoService {

    @Resource
    private AppInfoMapper infoMapper;
    @Resource
    @Lazy
    private UserCommonAppService userCommonAppService;

    @Override
    public Long createInfo(AppInfoSaveReqVO createReqVO) {
        // 插入
        AppInfo info = BeanCopyUtils.copyBean(createReqVO, AppInfo::new);
        List<Integer> scope = createReqVO.getScope();
        info.setScope(convertScope(scope));
        normalizeAppLocator(info);
        LocalDateTime current = LocalDateTime.now();
        // 创建时间为空，则以当前时间为插入时间
        if (Objects.isNull(info.getCreateTime())) {
            info.setCreateTime(current);
        }
        validateInfoExists(null, createReqVO.getName());
        infoMapper.insert(info);
        // 返回
        return info.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInfo(AppInfoSaveReqVO updateReqVO) {
        // 校验存在
        if (updateReqVO.getId() == null) {
            throw new RuntimeException("应用编号不能为空");
        }
        AppInfo old = infoMapper.selectInfoById(updateReqVO.getId());
        if (Objects.isNull(old)) {
            throw new RuntimeException("应用不存在");
        }
        validateInfoExists(updateReqVO.getId(), updateReqVO.getName());

        // 前置应用更新为非前置应用时，删除所有绑定到该应用的绑定关系
        if (old.getType().equals(3) && !updateReqVO.getType().equals(3)) {
            infoMapper.deleteAllPrerequisite(updateReqVO.getId());
        }
        // 更新
        AppInfo updateObj = BeanCopyUtils.copyBean(updateReqVO, AppInfo::new);
        List<Integer> scope = updateReqVO.getScope();
        updateObj.setScope(convertScope(scope));
        normalizeAppLocator(updateObj);
        infoMapper.updateById(updateObj);
    }

    private void normalizeAppLocator(AppInfo appInfo) {
        if (Objects.isNull(appInfo) || Objects.isNull(appInfo.getType())) {
            return;
        }
        if (appInfo.getType().equals(1) || appInfo.getType().equals(3)) {
            if (StringUtils.isBlank(appInfo.getPackageAndroid()) && StringUtils.isNotBlank(appInfo.getUrl())) {
                appInfo.setPackageAndroid(appInfo.getUrl());
            }
            appInfo.setUrl("");
            return;
        }
        if (appInfo.getType().equals(4)) {
            if (StringUtils.isBlank(appInfo.getPackageHm()) && StringUtils.isNotBlank(appInfo.getUrl())) {
                appInfo.setPackageAndroid(appInfo.getUrl());
            }
            appInfo.setUrl("");
        }
    }

    @Override
    @LogReport(type = OperationTypeEnum.APPLICATION_SHELVES)
    public void shelvesAppInfo(@LogReportParam AppInfoUpdateStatusReqVO updateStatusVO) {
        AppInfo appInfo = infoMapper.selectInfoById(updateStatusVO.getId());
        if (Objects.isNull(appInfo)) {
            throw new RuntimeException("应用不存在");
        }
        appInfo.setStatus(updateStatusVO.getStatus());
        infoMapper.updateById(appInfo);
    }

    @Override
    @LogReport(type = OperationTypeEnum.APPLICATION_DOWN_SHELF)
    public void downShelfAppInfo(@LogReportParam AppInfoUpdateStatusReqVO updateStatusVO) {
        AppInfo appInfo = infoMapper.selectInfoById(updateStatusVO.getId());
        if (Objects.isNull(appInfo)) {
            throw new RuntimeException("应用不存在");
        }
        appInfo.setStatus(updateStatusVO.getStatus());
        infoMapper.updateById(appInfo);
    }

    private void validateInfoExists(Long id, String name) {
        if (id != null && infoMapper.selectInfoById(id) == null) {
            throw new RuntimeException("应用不存在");
        }
        if (StringUtils.isNotBlank(name)){
            QueryWrapper<AppInfo> appInfoQueryWrapper = new QueryWrapper<>();
            appInfoQueryWrapper.eq(AppInfo.NAME, name);
            appInfoQueryWrapper.eq(AppInfo.IS_DELETED, 0);
            if(id != null){
                 appInfoQueryWrapper.ne(AppInfo.ID, id);
            }
            if (infoMapper.selectCount(appInfoQueryWrapper) > 0) {
                throw new RuntimeException("应用名称已存在");
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInfo(Long id) {
        if (id == null) {
            throw new RuntimeException("应用编号不能为空");
        }
        AppInfo appInfo = infoMapper.selectInfoById(id);
        if (Objects.isNull(appInfo)) {
            throw new RuntimeException("应用不存在");
        }
        // 逻辑删除
        infoMapper.deleteAllPrerequisite(id);
        appInfo.setIsDeleted(1);
        infoMapper.updateById(appInfo);
        userCommonAppService.deleteAllByAppId(id);
    }

    @Override
    @LogReport(type = OperationTypeEnum.APPLICATION_DELETE)
    public void deleteInfo(@LogReportParam AppInfo appInfo) {
        deleteInfo(appInfo.getId());
    }

    @Override
    public boolean exists(Long id) {
        return infoMapper.selectInfoById(id) != null;
    }

    @Override
    public AppInfoDO getInfo(Long id) {
        AppInfo appInfo = infoMapper.selectInfoById(id);
        Integer scope = appInfo.getScope();
        List<Integer> scopeList = convertToScopeList(scope);
        AppInfoDO appInfoDO = BeanCopyUtils.copyBean(appInfo, AppInfoDO::new);
        appInfoDO.setScopeList(scopeList);
        return appInfoDO;
    }

    @Override
    public AppInfo findById(Long id) {
        return infoMapper.selectInfoById(id);
    }

    @Override
    public PageResult<AppInfoDO> getInfoPage(AppInfoPageReqVO pageReqVO) {
        var page = new Page<AppInfo>(pageReqVO.getPageNum(), pageReqVO.getPageSize());
        Page<AppInfoDO> appInfoDOPage = infoMapper.selectPage(page, pageReqVO);
        log.info("pageResult:{}", appInfoDOPage);
        var pageResult = new PageResult<AppInfoDO>();
        pageResult.setPages(appInfoDOPage.getPages());
        pageResult.setSize(appInfoDOPage.getSize());
        pageResult.setTotal(appInfoDOPage.getTotal());
        pageResult.setCurrent(appInfoDOPage.getCurrent());
        List<AppInfoDO> records = appInfoDOPage.getRecords();
        records.forEach(appInfoDO -> {
            Integer scope = appInfoDO.getScope();
            List<Integer> scopeList = convertToScopeList(scope);
            appInfoDO.setScopeList(scopeList);
        });
        pageResult.setRecords(appInfoDOPage.getRecords());
        return pageResult;
    }

    @Override
    public List<AppInfoDO> selectList() {
        return selectList(null);
    }

    @Override
    public List<AppInfoDO> selectList(Integer queryScope) {
        QueryWrapper<AppInfo> wrapper = new QueryWrapper<>();
        wrapper.eq(AppInfo.STATUS, 0);
        wrapper.eq(AppInfo.IS_DELETED, 0);
        // app展示时仅展示非前置应用
        wrapper.ne(AppInfo.TYPE, 3);
        // 位运算查询：queryScope & scope = queryScope
        if (queryScope != null) {
            wrapper.apply("{0} & scope = {0}", queryScope);
        }
        List<AppInfo> appInfos = infoMapper.selectList(wrapper);
        return BeanCopyUtils.copyList(appInfos, AppInfoDO::new);
    }

    @Override
    public PageResult<AppInfoDO> getPrerequisitePage(AppInfoPageReqVO pageReqVO) {
        if (pageReqVO.getPageNum() != null && pageReqVO.getPageSize() != null) {
            var page = new Page<AppInfo>(pageReqVO.getPageNum(), pageReqVO.getPageSize());
            Page<AppInfoDO> appInfoDOPage = infoMapper.selectPrerequisitePage(page, pageReqVO);
            log.info("prerequisitePageResult Page:{}", appInfoDOPage);
            var pageResult = new PageResult<AppInfoDO>();
            pageResult.setTotal(appInfoDOPage.getTotal());
            pageResult.setCurrent(appInfoDOPage.getCurrent());
            pageResult.setRecords(appInfoDOPage.getRecords());
            pageResult.setPages(appInfoDOPage.getPages());
            pageResult.setSize(appInfoDOPage.getSize());
            return pageResult;
        } else {
            List<AppInfoDO> appInfoDOPage = infoMapper.selectPrerequisitePage(pageReqVO);
            log.info("prerequisitePageResult List:{}", appInfoDOPage);
            var pageResult = new PageResult<AppInfoDO>();
            pageResult.setTotal((long) appInfoDOPage.size());
            pageResult.setCurrent(1L);
            pageResult.setRecords(appInfoDOPage);
            pageResult.setPages(1L);
            pageResult.setSize((long) appInfoDOPage.size());
            return pageResult;
        }
    }

    @Override
    public PageResult<AppInfoDO> getAppPage(AppInfoPageReqVO pageReqVO) {
        var page = new Page<AppInfo>(pageReqVO.getPageNum(), pageReqVO.getPageSize());
        Page<AppInfoDO> appInfoDOPage = infoMapper.selectAppPage(page, pageReqVO);
        log.info("appPageResult:{}", appInfoDOPage);
        var pageResult = new PageResult<AppInfoDO>();
        pageResult.setCurrent(appInfoDOPage.getCurrent());
        pageResult.setRecords(appInfoDOPage.getRecords());
        pageResult.setTotal(appInfoDOPage.getTotal());
        pageResult.setPages(appInfoDOPage.getPages());
        pageResult.setSize(appInfoDOPage.getSize());
        return pageResult;
    }

    /**
     * 将scopeList转换为位运算的scope值
     * scopeList: [1, 2, 4] -> scope: 7 (1|2|4)
     *
     * @param scopeList 展示范围列表
     * @return 位运算后的scope值，如果为空则返回null
     */
    private Integer convertScope(List<Integer> scopeList) {
        if (scopeList == null || scopeList.isEmpty()) {
            return null;
        }
        int scope = 0;
        for (Integer item : scopeList) {
            if (item != null) {
                scope |= item;
            }
        }
        return scope;
    }

    /**
     * 将scope位运算值转换为scopeList
     * scope: 7 -> scopeList: [1, 2, 4]
     * scope: 3 -> scopeList: [1, 2]
     * scope: 5 -> scopeList: [1, 4]
     *
     * @param scope 位运算值
     * @return 展示范围列表，如果为空则返回空列表
     */
    private List<Integer> convertToScopeList(Integer scope) {
        List<Integer> scopeList = new ArrayList<>();
        if (scope == null || scope <= 0) {
            return scopeList;
        }
        // scope定义：1-鸿蒙移动端，2-安卓移动端，4-PC浏览器，8-PC桌面端
        int[] scopeValues = {1, 2, 4, 8};
        for (int value : scopeValues) {
            if ((scope & value) == value) {
                scopeList.add(value);
            }
        }
        return scopeList;
    }
}