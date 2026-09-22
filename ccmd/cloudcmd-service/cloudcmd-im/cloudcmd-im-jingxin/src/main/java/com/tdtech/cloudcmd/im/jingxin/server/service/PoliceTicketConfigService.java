package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketClientQO;
import java.util.List;

public interface PoliceTicketConfigService {

    boolean createPoliceTicket(PoliceTicketClient policeTicketClient);

    boolean updatePoliceTicket(PoliceTicketClient policeTicketClient);

    void enable(Long id, Integer status);

    boolean deletePoliceTicket(Long id);

    boolean deletePoliceTicket(PoliceTicketClient policeTicketClient);

    PoliceTicketClient getPoliceTicketById(Long id);

    List<PoliceTicketClient> listAllPoliceTickets();

    Page<PoliceTicketClient> pagePoliceTicketClients(PoliceTicketClientQO qo, Long current,
        Long size);
}
