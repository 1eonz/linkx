package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CooperationUnattended;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CooperationUnattendedVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CooperationUnattendedMapper extends BaseMapper<CooperationUnattended> {
    Page<CooperationUnattendedVO> findPage(
            Page<CooperationUnattendedVO> page,
            @Param("keywords") String keywords,
            @Param("orgCodeList") List<String> orgCodeList,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

}
