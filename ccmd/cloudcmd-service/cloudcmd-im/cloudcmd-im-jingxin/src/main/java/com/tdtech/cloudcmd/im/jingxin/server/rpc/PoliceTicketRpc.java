package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.PoliceTicketRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketService;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketTypeService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DubboService
public class PoliceTicketRpc implements PoliceTicketRpcApi {
    @Resource
    private PoliceTicketService policeTicketService;
    @Resource
    private PoliceTicketTypeService policeTicketTypeService;

    @Override
    public Page<PoliceTicketVO> findPage(Integer current, Integer size,
                                         @NotNull PoliceTicketQO policeTicket){
        Page<PoliceTicketVO> page = new Page<>(current, size);
        var r = policeTicketService.findPage(page, policeTicket);
        Optional.ofNullable(r.getRecords()).stream().flatMap(Collection::stream).forEach(record -> {
            record.setOrigin(null);
        });
        return r;
    }

    @Override
    public void bindGroup(@NotNull Long groupId, @NotNull List<Long> ticketIds){
        policeTicketService.bindGroup(groupId, ticketIds);
    }

    @Override
    public List<String> getTag(Long postId) {
        return policeTicketTypeService.getTag(postId);
    }

    @Override
    public List<Long> getPosts(String tag) {
        return policeTicketTypeService.getPosts(tag);
    }
}
