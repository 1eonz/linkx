package com.tdtech.linkx.node.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.linkx.node.entity.PeerNodeGrantPermission;
import com.tdtech.linkx.node.mapper.PeerNodeGrantPermissionMapper;
import com.tdtech.linkx.node.service.IPeerNodeGrantPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Service
@Slf4j
public class PeerNodeGrantPermissionServiceImpl
        extends ServiceImpl<PeerNodeGrantPermissionMapper, PeerNodeGrantPermission>
        implements IPeerNodeGrantPermissionService {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public List<PeerNodeGrantPermission> listAll() {
        return list();
    }

    @Override
    public String matchPermission(String originUri) {
        if (originUri == null || originUri.isEmpty()) {
            return null;
        }
        List<PeerNodeGrantPermission> permissions = listAll();
        for (PeerNodeGrantPermission p : permissions) {
            if (p.getUris() == null || p.getUris().isEmpty()) {
                continue;
            }
            for (String pattern : p.getUris().split(",")) {
                String trimmed = pattern.trim();
                if (!trimmed.isEmpty() && pathMatcher.match(trimmed, originUri)) {
                    return p.getName();
                }
            }
        }
        return null;
    }
}
