package cloudcmd.service.rpc;

/**
 * 应用信息RPC接口
 */

import cloudcmd.vo.AppInfoResp4RpcVO;
import cloudcmd.vo.UserCommonAppResp4RpcVo;

import java.util.List;

public interface AppInfoRpcService {
    List<AppInfoResp4RpcVO> getAppInfoByIds(List<Long> appIds ,Integer scope);

    List<AppInfoResp4RpcVO> getAllAppInfoByIds(Integer scope);

    List<UserCommonAppResp4RpcVo> getMy(String userId, Integer terminalType,Integer scope);
    /**
     * 获取H5应用数量
     *
     * @return H5应用数量
     */
    Long getH5AppCount();
}
