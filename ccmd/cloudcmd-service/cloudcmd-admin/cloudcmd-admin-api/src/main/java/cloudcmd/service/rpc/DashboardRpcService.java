package cloudcmd.service.rpc;


import cloudcmd.dto.DashboardCreateDto;
import cloudcmd.dto.DashboardUpdateDto;
import cloudcmd.rsp.DashboardChartListRsp;
import cloudcmd.rsp.DashboardChartRsp;
import cloudcmd.rsp.DashboardPageRsp;
import cloudcmd.rsp.DashboardRsp;
import cloudcmd.vo.DashboardCommonVo;
import cloudcmd.vo.DashboardPageVo;

import java.util.List;

/**
 * @program: back-new
 * @description: DashboardRpcService
 * @author: yj
 * @date: 2024-05-17
 **/
public interface DashboardRpcService {

    Long createDashboard(DashboardCreateDto createDto);

    DashboardChartRsp getDashboard(Long id, Long orgId);

    Long updateDashboard(DashboardUpdateDto dashboardUpdateRsp);

    boolean removeDashboard(Long id, Long orgId, Long userId);

    DashboardPageRsp<DashboardRsp> listDashboardPage(DashboardPageVo dashboardPageVo, Long userId);

    List<DashboardRsp> listDashboardList(Integer status, Long userId);

    DashboardPageRsp<DashboardChartListRsp> getDashboardChartsPage(DashboardCommonVo commonVo);

    DashboardRsp queryDashboard(Long id);
}
