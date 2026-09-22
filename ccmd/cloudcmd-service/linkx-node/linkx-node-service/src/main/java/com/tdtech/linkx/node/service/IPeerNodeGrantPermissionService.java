package com.tdtech.linkx.node.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.linkx.node.entity.PeerNodeGrantPermission;

import java.util.List;

public interface IPeerNodeGrantPermissionService extends IService<PeerNodeGrantPermission> {

    /**
     * 查询所有权限路由配置。
     */
    List<PeerNodeGrantPermission> listAll();

    /**
     * 根据 originURI 匹配权限项。
     * 使用 AntPathMatcher 对 uris（逗号分隔）逐一匹配。
     *
     * @param originUri 原始业务 URI，如 /collaboration/post/queryDepartment
     * @return 匹配到的权限项名称（org/dashboard/coopuser），未匹配返回 null
     */
    String matchPermission(String originUri);
}
