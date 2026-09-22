package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.RegionRange;

public interface RegionRangeMapper extends BaseMapper<RegionRange> {

    /**
     *  根据id获取自身及子集
     * @param parentId 主键
     * @return
     */
    List<RegionRange> getRegionByIdOrParentId(Long parentId);


    /**
     * 根据code查询辖区
     * @return
     */
    Long getRegionByCode(String code);

    /**
     * 根据组织id获取所对应的辖区信息
     * @param orgId 组织信息
     * @return
     */
    List<RegionRange> getRegionByOrgId(Long orgId);
}
