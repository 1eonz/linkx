package com.tdtech.cloudcmd.admin.resource.service;

import com.tdtech.cloudcmd.admin.resource.entity.PerWarningRalation;
import com.tdtech.cloudcmd.admin.resource.entity.dto.CarouselDO;
import com.tdtech.cloudcmd.admin.resource.entity.dto.PerWarningRalationDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.PerWarningRalationPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.PerWarningRalationSaveReqVO;
import com.tdtech.cloudcmd.bean.PageResult;

import javax.validation.Valid;
import java.util.List;

/**
 * @author: S063874
 * @date: 2026-01-13 14:42
 */
public interface IPerWarningRalationService {

    /**
     * 创建预警推送关系
     *
     * @param perWarningRalationSaveReqVO
     * @return 返回创建的条数
     */
    int createPerWaringRalation(@Valid PerWarningRalationSaveReqVO perWarningRalationSaveReqVO);

    /**
     * 更新预警推送关系
     *
     * @param perWarningRalationSaveReqVO
     */
    boolean updatePerWaringRalation(@Valid PerWarningRalationSaveReqVO perWarningRalationSaveReqVO);

    /**
     * 删除预警推送关系
     *
     * @param id
     */
    void deletePerWaringRalation(Long id);

    /**
     * 批量删除预警推送关系
     *
     * @param ids 编号
     */
    void deletePerWaringRalationListByIds(List<Long> ids);

    /**
     * 获得预警关系分页
     *
     * @param pageReqVO 分页查询
     * @return 预警关系分页
     */
    PageResult<PerWarningRalationDO> getPerWarningRalationPage(PerWarningRalationPageReqVO pageReqVO);

    /**
     *
     * @param postId
     * @return
     */
    List<PerWarningRalation> getPerWarningRalationByPostId(Long postId);

    /**
     *
     * @param deptIds
     * @return
     */
    List<PerWarningRalation> getPerWarningRalationByDeptId(List<Long> deptIds);

    /**
     * 删除指定群组的预警通知关系
     *
     * @param groupId 群组ID
     * @return 删除条数
     */
    int deleteGroupWarningRalation(Long groupId);

    void updateTargetName(Long targetId, String newName);
}