package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicket;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TrPoliceTicketGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PoliceTicketMapper extends MPJBaseMapper<PoliceTicket> {
    List<TrPoliceTicketGroupVO> findListByOrgCodeList(@Param("orgCodeList") List<String> orgCodeList);
}
