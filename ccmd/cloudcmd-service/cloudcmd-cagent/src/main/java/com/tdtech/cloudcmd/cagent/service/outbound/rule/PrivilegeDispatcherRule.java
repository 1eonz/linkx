package com.tdtech.cloudcmd.cagent.service.outbound.rule;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.service.UserInfo;
import com.tdtech.cloudcmd.cagent.service.entity.BaseChannel;

@Component
public class PrivilegeDispatcherRule {

    public <T extends BaseChannel> Stream<T> pick(Long priv, Stream<T> source) {
        return source.filter(a -> hasPriv(a.getUserInfo(), priv));
    }

    private boolean hasPriv(UserInfo userInfo, Long priv) {
//        return userInfo.getImOrgPrivs()!=null&&userInfo.getImOrgPrivs().contains(priv+"");
        //TODO delete for now
        return true;
    }
}
