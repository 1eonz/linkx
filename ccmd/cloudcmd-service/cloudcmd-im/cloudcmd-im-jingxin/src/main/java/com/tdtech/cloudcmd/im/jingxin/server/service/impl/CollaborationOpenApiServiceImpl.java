package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import java.security.SecureRandom;
import java.util.List;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserDeptInfoVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserDeptNodeInfoVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserGetVo;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationClientVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationOpenApiService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationClientMapper;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;

/**
 * @author lsc
 * @date 2025/8/12
 **/
@Slf4j
@Service
public class CollaborationOpenApiServiceImpl implements CollaborationOpenApiService {

    private static final SecureRandom numberGenerator = new SecureRandom();
    @Resource
    private CollaborationClientMapper collaborationClientMapper;
    @Resource
    private IdWorker idWorker;
    @Autowired
    private RedisUtil redisUtil;
    @Resource
    private ImHttpClient  imHttpClient;

    @Override
    public Long createClient(CollaborationClientVO collaborationClientVO) {
        CollaborationClient collaborationClient =
            BeanCopyUtils.copyBean(collaborationClientVO, CollaborationClient::new);
        collaborationClient.setId(idWorker.nextId());
        String clientType = collaborationClientVO.getClientType();
        if(StringUtils.isBlank(clientType)){
            // 默认给0
            collaborationClientVO.setClientType("0");
        }
        // 校验应用名称和应用id不可重复
        if (collaborationClientMapper.selectCount(
            new QueryWrapper<CollaborationClient>().eq("client_name", collaborationClient.getClientName())) > 0) {
            throw new RuntimeException("应用名称不可重复");
        }
        if (collaborationClientMapper.selectCount(
            new QueryWrapper<CollaborationClient>().eq("client_id", collaborationClient.getClientId())) > 0) {
            throw new RuntimeException("应用id不可重复");
        }
        collaborationClientMapper.insert(collaborationClient);
        return collaborationClient.getId();
    }

    @Override
    public Page<CollaborationClient> getClient(CollaborationClientVO collaborationClientVO) {
        return collaborationClientMapper.selectClientPage(
            new Page<CollaborationClient>(collaborationClientVO.getPageNum(), collaborationClientVO.getPageSize()),
            collaborationClientVO);
    }

    @Override
    public void deleteClient(Long id) {
        collaborationClientMapper.deleteById(id);
    }

    @Override
    public void updateClient(CollaborationClientVO collaborationClientVO) {
        CollaborationClient collaborationClient =
            BeanCopyUtils.copyBean(collaborationClientVO, CollaborationClient::new);
        // 校验应用名称和应用id不可重复
        if (collaborationClientMapper.selectCount(new QueryWrapper<CollaborationClient>()
            .eq("client_name", collaborationClient.getClientName()).ne("id", collaborationClient.getId())) > 0) {
            throw new RuntimeException("应用名称不可重复");
        }
        if (collaborationClientMapper.selectCount(new QueryWrapper<CollaborationClient>()
            .eq("client_id", collaborationClient.getClientId()).ne("id", collaborationClient.getId())) > 0) {
            throw new RuntimeException("应用id不可重复");
        }
        collaborationClientMapper.updateById(collaborationClient);
    }

    @Override
    public CollaborationClient clientDetail(Long id) {
        return collaborationClientMapper.selectById(id);
    }

}