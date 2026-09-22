package cloudcmd.service.rpc;

import cloudcmd.dto.PerWarningRalationDto;

import java.util.List;

/**
 * @author: S063874
 * @date: 2026-01-15 19:25
 */
public interface PerWarningRalationRpcService {

    List<PerWarningRalationDto> getPerWarningRalationByPostId(Long postId);

    List<PerWarningRalationDto> getPerWarningRalationByDeptId(List<Long> deptIds);

    int deleteGroupWarningRalation(Long groupId);

    void updateTargetName(Long targetId, String newName);
}