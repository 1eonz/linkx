package com.tdtech.cloudcmd.admin.resource.service;

import com.tdtech.cloudcmd.admin.resource.entity.AppInfo;
import com.tdtech.cloudcmd.admin.resource.entity.dto.AppInfoDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoUpdateStatusReqVO;
import com.tdtech.cloudcmd.bean.PageResult;

import javax.validation.Valid;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 **/
public interface IAppInfoService {

    /**
     * 创建应用管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInfo(@Valid AppInfoSaveReqVO createReqVO);

    /**
     * 更新应用管理
     *
     * @param updateReqVO 更新信息
     */
    void updateInfo(@Valid AppInfoSaveReqVO updateReqVO);

    /**
     * 删除应用管理
     *
     * @param id 编号
     */
    void deleteInfo(Long id);

    void deleteInfo(AppInfo appInfo);

    boolean exists(Long id);

    /**
     * 获得应用管理
     *
     * @param id 编号
     * @return 应用管理
     */
    AppInfoDO getInfo(Long id);

    AppInfo findById(Long id);

    /**
     * 获得应用管理分页
     *
     * @param pageReqVO 分页查询
     * @return 应用管理分页
     */
    PageResult<AppInfoDO> getInfoPage(AppInfoPageReqVO pageReqVO);

    /**
     * 上架应用
     *
     * @param updateStatusVO
     */
    void shelvesAppInfo(AppInfoUpdateStatusReqVO updateStatusVO);

    /**
     * 下架应用
     *
     * @param updateStatusVO
     */
    void downShelfAppInfo(AppInfoUpdateStatusReqVO updateStatusVO);

    List<AppInfoDO> selectList();

    List<AppInfoDO> selectList(Integer queryScope);

    PageResult<AppInfoDO> getPrerequisitePage(@Valid AppInfoPageReqVO pageReqVO);

    PageResult<AppInfoDO> getAppPage(@Valid AppInfoPageReqVO pageReqVO);
}