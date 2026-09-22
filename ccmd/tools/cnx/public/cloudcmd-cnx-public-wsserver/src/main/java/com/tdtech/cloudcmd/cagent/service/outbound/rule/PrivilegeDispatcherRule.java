package com.tdtech.cloudcmd.cagent.service.outbound.rule;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.service.OrgTreeCache;
import com.tdtech.cloudcmd.cagent.service.UserInfo;
import com.tdtech.cloudcmd.cagent.service.entity.BaseChannel;

@Component
public class PrivilegeDispatcherRule {
    @Resource
    private OrgTreeCache orgTreeCache;

    public <T extends BaseChannel> Stream<T> pick(Long priv, Stream<T> source) {
        return source.filter(a -> hasPriv(a.getUserInfo(), priv));
    }

    private boolean hasPriv(UserInfo userInfo, Long priv) {
        List<Long> pl;
        if (Objects.equals(userInfo.getHasChildOrgPriv(), 0)) {
            pl = orgTreeCache.getTree(userInfo.getOrganizationId());
        } else {
            pl = Collections.singletonList(userInfo.getOrganizationId());
        }
        return pl != null && pl.contains(priv);
    }
}
