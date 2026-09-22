package com.tdtech.cloudcmd.linkx.dashboard.service.impl;

import com.tdtech.cloudcmd.im.jingxin.api.GroupRatingRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatQueryQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatRespVO;
import com.tdtech.cloudcmd.linkx.dashboard.service.IGroupRatingStatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GroupRatingStatServiceImpl implements IGroupRatingStatService {

    @DubboReference
    private GroupRatingRpcApi groupRatingRpcApi;

    @Override
    public GroupRatingStatRespVO getGroupTagRating(Integer type, String departmentCode, String startTime, String endTime) {
        GroupRatingStatQueryQO query = GroupRatingStatQueryQO.builder()
            .type(type)
            .departmentCode(departmentCode)
            .startTime(startTime)
            .endTime(endTime)
            .build();

        return groupRatingRpcApi.getGroupTagRating(query);
    }

    @Override
    public GroupRatingStatRespVO getCoopUserRating(Integer type, String departmentCode, String startTime, String endTime) {
        GroupRatingStatQueryQO query = GroupRatingStatQueryQO.builder()
            .type(type)
            .departmentCode(departmentCode)
            .startTime(startTime)
            .endTime(endTime)
            .build();

        return groupRatingRpcApi.getCoopUserRating(query);
    }
}
