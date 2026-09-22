package com.tdtech.cloudcmd.admin.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.admin.resource.entity.PerWarningRalation;
import com.tdtech.cloudcmd.admin.resource.entity.dto.PerWarningRalationDO;

import com.tdtech.cloudcmd.admin.resource.entity.vo.PerWarningRalationPageReqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author sf
 * @date 2026/1/13
 **/
@Mapper
public interface PerWarningRalationMapper extends BaseMapper<PerWarningRalation> {

    /**
     *
     * @param page
     * @param reqVO
     * @return
     */
    Page<PerWarningRalationDO> selectPerWarningRalationPage(Page<PerWarningRalation> page, @Param("reqVO") PerWarningRalationPageReqVO reqVO);

    /**
     *
     * @param businessId
     * @return List<PerWarningRalationDO>
     */
    List<PerWarningRalationDO> selectByBusinessId(Integer businessId);

    void batchInsert(List<PerWarningRalation> perWarningRalationList);
}
