package com.tdtech.cloudcmd.cagent.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.remote.RemoteClient;
import com.tdtech.cloudcmd.cagent.remote.TreeUtil;

@Component
public class OrgTreeCache {
    private final Object treeLock = new Object();
    @Resource
    private RemoteClient remoteClient;

    private volatile Map<Long, List<Long>> orgTree;
    private long cacheTime = 0;

    public List<Long> getTree(Long id) {
        var now = System.currentTimeMillis();
        if (cacheTime + 60000L < now) {
            synchronized (treeLock) {
                // double check lock
                if (cacheTime + 60000L < now) {
                    init();
                    cacheTime = now;
                }
            }
        }
        return orgTree.get(id);
    }

    private void init() {
        var organizations = remoteClient.allOrgs();
        var treeNodes = TreeUtil.buildTree(organizations, (c, p) -> Objects.equals(p.getId(), c.getParentId()));
        orgTree = new HashMap<>();
        TreeUtil.depthFirstTraverse(treeNodes, tn -> {
            var org = tn.getData();
            var id = org.getId();
            orgTree.put(id, new LinkedList<>());
            orgTree.get(id).add(id);
            TreeUtil.depthFirstTraverse(tn.getChildren(), tnc -> orgTree.get(id).add(tnc.getData().getId()));
        });
    }

}
