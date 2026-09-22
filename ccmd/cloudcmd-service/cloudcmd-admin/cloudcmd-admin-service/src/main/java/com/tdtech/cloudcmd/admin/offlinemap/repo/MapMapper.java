package com.tdtech.cloudcmd.admin.offlinemap.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MapMapper extends BaseMapper<OfMap> {
    OfMap getOldestMap();
}
