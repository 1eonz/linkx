package cloudcmd.service.rpc;
import cloudcmd.rsp.CdrMsgRsp;
import cloudcmd.rsp.CdrVideosRsp;
import cloudcmd.rsp.CdrVoiceRsp;
import cloudcmd.rsp.DashboardPageRsp;
import cloudcmd.vo.DashboardCommonVo;

public interface CdrRpcService {

    DashboardPageRsp<CdrVideosRsp> getCdrVideosPage(DashboardCommonVo param);

    DashboardPageRsp<CdrVideosRsp> getCdrMonitorsPage(DashboardCommonVo param);

    DashboardPageRsp<CdrVideosRsp> getCdrGroupCallsPage(DashboardCommonVo param);

    DashboardPageRsp<CdrMsgRsp> getCdrMsgPage(DashboardCommonVo param);

    DashboardPageRsp<CdrVideosRsp> getCdrPstnsPage(DashboardCommonVo param);

    DashboardPageRsp<CdrVideosRsp> getCdrConferencesPage(DashboardCommonVo param);

    DashboardPageRsp<CdrVideosRsp> getCdrEmergencysPage(DashboardCommonVo param);

    DashboardPageRsp<CdrVoiceRsp> getCdrVoicesPage(DashboardCommonVo commonVo);
}
