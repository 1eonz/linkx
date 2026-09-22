package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.im.jingxin.api.GroupRatingRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatQueryQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupRatingStatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

@Slf4j
@DubboService
public class GroupRatingRpc implements GroupRatingRpcApi {

    @Resource
    private GroupRatingStatService groupRatingStatService;

    @Override
    public GroupRatingStatRespVO getGroupTagRating(GroupRatingStatQueryQO query) {
        return groupRatingStatService.getGroupTagRating(query);
    }

    @Override
    public GroupRatingStatRespVO getCoopUserRating(GroupRatingStatQueryQO query) {
        return groupRatingStatService.getCoopUserRating(query);
    }
}
