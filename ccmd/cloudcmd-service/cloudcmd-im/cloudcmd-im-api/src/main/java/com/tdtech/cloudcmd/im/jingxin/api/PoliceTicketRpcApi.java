package com.tdtech.cloudcmd.im.jingxin.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketVO;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface PoliceTicketRpcApi {
    Page<PoliceTicketVO> findPage(Integer current, Integer size,
                                  @NotNull PoliceTicketQO policeTicket) throws BusinessException;

    void bindGroup(@NotNull Long groupId, @NotNull List<Long> ticketIds)throws BusinessException;

    List<String> getTag(Long postId)throws BusinessException;

    List<Long> getPosts(String tag)throws BusinessException;
}
