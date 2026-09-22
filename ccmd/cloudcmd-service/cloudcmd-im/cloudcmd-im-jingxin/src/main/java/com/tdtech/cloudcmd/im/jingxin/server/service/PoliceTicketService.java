package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiCreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiCreateGroupCOV2;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiCreateGroupCOV3;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicket;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketStatisticsVO;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.List;
import java.util.Map;

public interface PoliceTicketService {
    int create(PoliceTicket policeTicket);

    PoliceTicket findById(Long id);

    int update(PoliceTicket policeTicket);

    int deleteById(Long id);

    Page<PoliceTicketVO> findPage(Page<PoliceTicketVO> page, PoliceTicketQO policeTicket);

    void bindGroup(@NotNull Long groupId, @NotNull List<Long> ticketIds);

    void deleteBinding(@NotNull Long groupId, @Null Long ticketId);

    List<Long> groupIds(@NotNull Long ticketId);

    Map<Long,Integer> countBinding(@NotNull List<Long> groupId);

    void processPoliceTicket(@Validated @NotNull PoliceTicket policeTicket);

    Long createGroup(OpenApiCreateGroupCO createGroupVO, String ownerId);

    List<PoliceTicketStatisticsVO> statistics(List<Long> orgIds);

    Long createGroupV2(OpenApiCreateGroupCOV2 openApiCreateGroupCOV2);

    R<Long> createGroupByIdCardsAndUserIds(OpenApiCreateGroupCOV3 openApiCreateGroupCOV3, String userId, String idCard);
}
