package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevel;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CoopLevelMapper extends BaseMapper<CoopLevelDO> {

    int selectByNameCount(@Param("name") String name);

    CoopLevelDO selectByParentId(@Param("parentId") Long parentId);

    int selectChildrenCount(@Param("parentId") Long parentId);

    List<CoopLevel> selectChildren(@Param("parentId") Long parentId);

    CoopLevelDO selectInfoById(@Param("id") String levelId);

    List<CoopLevel> selectAllChildrenCount(@Param("ids") List<Long> ids);
}
