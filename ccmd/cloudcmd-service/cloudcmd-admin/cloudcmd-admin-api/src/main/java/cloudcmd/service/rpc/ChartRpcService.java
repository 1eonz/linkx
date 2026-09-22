package cloudcmd.service.rpc;


import cloudcmd.dto.ChartCreateDto;
import cloudcmd.dto.ChartUpdateDto;
import cloudcmd.enums.DashboardCodeEnum;
import cloudcmd.rsp.ChartPageRsp;
import cloudcmd.rsp.ChartRsp;
import cloudcmd.vo.ChartPageVo;

import java.util.List;

/**
 * @program: back-new
 * @description: ChartRpcService
 * @author: yj
 * @date: 2024-05-17
 **/
public interface ChartRpcService {

    Long createChart(ChartCreateDto createDto);

    Long updateChart(ChartUpdateDto updateDto);

    ChartRsp getChart(Long id);

    DashboardCodeEnum removeChart(Long id, Long userId);

    ChartPageRsp<ChartRsp> listChartPage(ChartPageVo chartPageVo, Long userId);

    List<ChartRsp> listChart(String chartName, Long userId);

    boolean getChartInDashboardIsPublished(Long id);

}
