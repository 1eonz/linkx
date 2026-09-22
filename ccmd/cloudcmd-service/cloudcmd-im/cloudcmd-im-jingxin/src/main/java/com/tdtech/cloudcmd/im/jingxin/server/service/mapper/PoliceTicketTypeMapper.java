package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.NotNull;

@Mapper
public interface PoliceTicketTypeMapper extends BaseMapper<PoliceTicketType> {
    PoliceTicketType selectByTag(@Param("tag") @NotNull String tag);
}
