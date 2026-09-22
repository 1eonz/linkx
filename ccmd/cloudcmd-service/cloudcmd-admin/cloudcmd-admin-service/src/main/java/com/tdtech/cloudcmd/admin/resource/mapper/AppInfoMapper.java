package com.tdtech.cloudcmd.admin.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.admin.resource.entity.AppInfo;
import com.tdtech.cloudcmd.admin.resource.entity.dto.AppInfoDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AppInfoPageReqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Mapper
public interface AppInfoMapper extends BaseMapper<AppInfo> {

    Page<AppInfoDO> selectPage(Page<AppInfo> page, @Param("reqVO") AppInfoPageReqVO reqVO);

    AppInfo selectInfoById(Long id);

    List<AppInfoDO> selectPrerequisitePage(@Param("reqVO") AppInfoPageReqVO pageReqVO);

    Page<AppInfoDO> selectPrerequisitePage(
            Page<AppInfo> page, @Param("reqVO") AppInfoPageReqVO pageReqVO);

    Page<AppInfoDO> selectAppPage(Page<AppInfo> page, @Param("reqVO") AppInfoPageReqVO pageReqVO);

    void deleteAllPrerequisite(@Param("id") Long id);
}
