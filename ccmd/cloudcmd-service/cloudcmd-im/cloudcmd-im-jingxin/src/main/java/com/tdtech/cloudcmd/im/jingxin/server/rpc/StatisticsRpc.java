package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.im.jingxin.api.StatisticsRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCoopDutySwitchDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCreateGroupDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticCursorResult;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticTaskDTO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.statistics.StaticTaskResponseDTO;
import com.tdtech.cloudcmd.im.jingxin.server.service.IStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统计明细增量拉取 RPC 实现（Dubbo）。
 * <p>
 * 供 linkx-dashboard 定时任务调用，委托 {@link IStatisticsService} 做源表聚合。
 */
@Slf4j
@DubboService
public class StatisticsRpc implements StatisticsRpcApi {

    @Resource
    private IStatisticsService statisticsService;

    @Override
    public StaticCursorResult<StaticTaskDTO> listStaticTaskByCursor(Date timeAfter, Long idAfter, int pageSize) {
        return statisticsService.listStaticTaskByCursor(timeAfter, idAfter, pageSize);
    }

    @Override
    public StaticCursorResult<StaticCreateGroupDTO> listStaticCreateGroupByCursor(Date timeAfter, Long idAfter, int pageSize) {
        return statisticsService.listStaticCreateGroupByCursor(timeAfter, idAfter, pageSize);
    }

    @Override
    public StaticCursorResult<StaticTaskResponseDTO> listStaticTaskResponseByCursor(Date timeAfter, Long idAfter, int pageSize) {
        return statisticsService.listStaticTaskResponseByCursor(timeAfter, idAfter, pageSize);
    }

    @Override
    public StaticCursorResult<StaticCoopDutySwitchDTO> listStaticCoopDutySwitchByCursor(Date timeAfter, Long idAfter, int pageSize) {
        return statisticsService.listStaticCoopDutySwitchByCursor(timeAfter, idAfter, pageSize);
    }

    @Override
    public List<CollaborationPostVO> listCoopPostsByType(Integer type) {
        return statisticsService.listCoopPostsByType(type);
    }

    @Override
    public Map<String, Long> findUserIdsByIdCards(List<String> idCards) {
        return statisticsService.findUserIdsByIdCards(idCards);
    }
}