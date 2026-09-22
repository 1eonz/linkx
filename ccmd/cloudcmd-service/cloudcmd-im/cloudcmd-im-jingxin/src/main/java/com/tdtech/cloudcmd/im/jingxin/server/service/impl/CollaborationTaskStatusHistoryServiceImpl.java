package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskStatusHistory;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskStatusHistoryCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskStatusHistoryService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationTaskStatusHistoryMapper;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Service
public class CollaborationTaskStatusHistoryServiceImpl extends ServiceImpl<CollaborationTaskStatusHistoryMapper, CollaborationTaskStatusHistory> implements ICollaborationTaskStatusHistoryService {
    @Resource
    private CollaborationTaskStatusHistoryMapper collaborationTaskStatusHistoryMapper;

    @Override
    public void save(CollaborationTaskStatusHistoryCO collaborationTaskStatusHistoryCO) {
        CollaborationTaskStatusHistory collaborationTaskStatusHistory = BeanCopyUtils.copyBean(collaborationTaskStatusHistoryCO, new CollaborationTaskStatusHistory());
        collaborationTaskStatusHistoryMapper.insert(collaborationTaskStatusHistory);
    }
}
