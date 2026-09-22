package cloudcmd.service.rpc;


import cloudcmd.rsp.*;
import cloudcmd.vo.DashboardCommonVo;

/**
 * @program: back-new-huawei
 * @description: UmpRpcService
 * @author: yj
 * @date: 2024-06-12
 **/

public interface UmpRpcService {
    
    DashboardPageRsp<UmpExecutorRsp> getUmpExecutorsPage(DashboardCommonVo commonVo);

    DashboardPageRsp<UmpOrganizationRsp> getUmpOrganiztionsPage(DashboardCommonVo commonVo);

    DashboardPageRsp<UmpEquipmentRsp> getUmpEquipmentsPage(DashboardCommonVo commonVo);

    DashboardPageRsp<UmpFacilityRsp> getUmpFacilitysPage(DashboardCommonVo commonVo);

}
