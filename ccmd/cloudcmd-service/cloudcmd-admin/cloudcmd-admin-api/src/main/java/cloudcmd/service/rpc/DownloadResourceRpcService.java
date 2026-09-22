package cloudcmd.service.rpc;


import cloudcmd.dto.CommonDownloadDto;
import cloudcmd.dto.DownloadParam;
import cloudcmd.dto.ResourceDto;

import java.util.List;

/**
 * @program: back-new-huawei
 * @description: DownloadResourceRpcService
 * @author: yj
 * @date: 2024-08-19
 **/
public interface DownloadResourceRpcService {

    List<CommonDownloadDto> getDownloadResource(String tableName, DownloadParam param);

    List<ResourceDto> getAllResourceInfo();
}
