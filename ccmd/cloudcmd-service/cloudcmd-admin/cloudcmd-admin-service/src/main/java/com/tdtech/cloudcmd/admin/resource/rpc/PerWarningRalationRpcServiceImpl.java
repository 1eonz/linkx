package com.tdtech.cloudcmd.admin.resource.rpc;

import cloudcmd.dto.PerWarningRalationDto;
import cloudcmd.service.rpc.PerWarningRalationRpcService;
import com.tdtech.cloudcmd.admin.resource.entity.PerWarningRalation;
import com.tdtech.cloudcmd.admin.resource.service.IPerWarningRalationService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: S063874
 * @date: 2026-01-15 19:34
 */


@DubboService
public class PerWarningRalationRpcServiceImpl implements PerWarningRalationRpcService {


    @Resource
    private IPerWarningRalationService perWarningRalationService;

    @Override
    public List<PerWarningRalationDto> getPerWarningRalationByPostId(Long postId) {
        // 根据岗位id查询预警关系
        List<PerWarningRalation> perWarningRalationList = perWarningRalationService.getPerWarningRalationByPostId(postId);
        return convertPerWarningRalationDto(perWarningRalationList);
    }

    @Override
    public List<PerWarningRalationDto> getPerWarningRalationByDeptId(List<Long> deptIds) {
        // 根据部门id查询预警关系
        List<PerWarningRalation> perWarningRalationList = perWarningRalationService.getPerWarningRalationByDeptId(deptIds);
        return convertPerWarningRalationDto(perWarningRalationList);
    }

    @Override
    public int deleteGroupWarningRalation(Long groupId) {
        return perWarningRalationService.deleteGroupWarningRalation(groupId);
    }

    @Override
    public void updateTargetName(Long targetId, String newName) {
        perWarningRalationService.updateTargetName(targetId, newName);
    }

    private List<PerWarningRalationDto> convertPerWarningRalationDto(List<PerWarningRalation> perWarningRalations) {
        return BeanCopyUtils.copyList(perWarningRalations, PerWarningRalationDto::new);
    }
}