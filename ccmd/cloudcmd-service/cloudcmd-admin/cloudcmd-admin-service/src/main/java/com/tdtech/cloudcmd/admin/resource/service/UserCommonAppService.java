package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import javax.validation.Valid;

import com.tdtech.cloudcmd.admin.resource.entity.vo.UserCommonAppRespVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.UserCommonAppSaveReqVO;

/**
 * @author lsc
 * @date 2025/7/17
 **/
public interface UserCommonAppService {

    /**
     * 创建常用应用
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    List<Long> save(@Valid UserCommonAppSaveReqVO createReqVO);

    Boolean getInitStatus(String userId,Integer terminalType);


    /**
     * 删除用户常用应用
     *
     * @param id 编号
     */
    void delete(Long id,Integer terminalType);

    /**
     * 删除所有指定应用AppId
     *
     * @param id 编号
     */
    void deleteAllByAppId(Long id);

    void deleteAllByUserId(String userId,Integer terminalType);

    /**
     * 查询当前用户常用应用列表
     */
    List<UserCommonAppRespVO> getMy(String userId, Integer terminalType, Integer scope);
}