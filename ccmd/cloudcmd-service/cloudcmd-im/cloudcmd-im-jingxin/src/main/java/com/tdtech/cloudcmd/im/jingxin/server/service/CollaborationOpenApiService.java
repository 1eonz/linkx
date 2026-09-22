package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserDeptNodeInfoVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationClientVO;

/**
 * @author lsc
 * @date 2025/8/12
 **/
public interface CollaborationOpenApiService {

    Long createClient(CollaborationClientVO collaborationClientVO);

    Page<CollaborationClient> getClient(CollaborationClientVO collaborationClientVO);

    void deleteClient(Long id);

    void updateClient(CollaborationClientVO collaborationClientVO);

    CollaborationClient clientDetail(Long id);

}
