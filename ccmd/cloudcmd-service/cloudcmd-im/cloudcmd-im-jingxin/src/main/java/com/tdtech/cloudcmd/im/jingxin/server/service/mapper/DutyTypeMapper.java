package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DutyTypeMapper extends BaseMapper<DutyType> {

    IPage<DutyType> selectPageByKeywords(Page<DutyType> page, @Param("name") String name);

    int countByName(@Param("name") String name, @Param("excludeType") Long excludeType);

    int countCurrentOrFutureScheduleByType(@Param("type") Long type);

    List<DutyType> selectAllList();
}
