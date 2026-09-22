package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualCreateReq;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualRespVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualUpdateReq;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.ImUserVirtual;

import java.util.List;

public interface ImUserVirtualService extends IService<ImUserVirtual> {

    /**
     * 创建虚拟用户
     */
    Long createVirtualUser(ImUserVirtualCreateReq req);

    /**
     * 更新虚拟用户
     */
    void updateVirtualUser(Long userId, ImUserVirtualUpdateReq req);

    /**
     * 查询虚拟用户列表
     */
    List<ImUserVirtualRespVO> listVirtualUsers(String userName);

    /**
     * 删除虚拟用户
     */
    void deleteVirtualUser(Long userId);

    int updateByPrimaryKey(ImUserVirtual imUserVirtual);

    int batchInsert(List<ImUserVirtual> list);

    int deleteByPrimaryKeyIn(List<Long> list);

    void deleteVirtualUserById(ImUserVirtual imUserVirtual);

    int deleteVirtualUserBatch(List<ImUserVirtual> imUserVirtuals);
}
