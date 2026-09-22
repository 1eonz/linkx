package com.tdtech.cloudcmd.admin.scheduler;

import com.tdtech.cloudcmd.msip.entity.LiveAlarmsResp;
import com.tdtech.cloudcmd.msip.entity.QueryLiveAlarmsReq;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
/**
 * MSIP告警清除-看护任务
 */
public class AlarmClearScheduler {
    @Autowired
    private ReportUtil reportUtil;

    /**
     * 查询告警未消除的数据，放入redis中
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void scheduled() {
        // 当天最多仅处理10000条，超过10000条的不处理
        long page = 1L;
        long pageSize = 100L;
        QueryLiveAlarmsReq req = new QueryLiveAlarmsReq();
        req.setPageNum(page);
        req.setPageSize(pageSize);
        int loopTimes = 0;
        // 仅处理本系统的告警信息
        List<String> alarmIds = Arrays.stream(AlarmTemplateZhEnum.values()).map(AlarmTemplateZhEnum::getAlarmId)
                .collect(Collectors.toList());
        while (true) {
            if (loopTimes > 100) {
                log.warn("清除告警信息任务执行，循环次数超过100次，跳过执行");
                break;
            }
            List<LiveAlarmsResp.AlarmsInfo> alarmsInfos = reportUtil.queryActiveAlarms(req);
            // 仅处理本系统的
            List<String> alarmIdToDeal = alarmsInfos.stream().filter(Objects::nonNull).filter(i ->
                            Objects.nonNull(i.getAlertDefine()) && alarmIds.contains(i.getAlertDefine().getAlertDefineId()))
                    .map(a -> a.getAlertDefine().getAlertDefineId())
                    .collect(Collectors.toList());
            // 放入redis中，由业务自行触发消除告警
            reportUtil.saveAlertActiveRecord2Redis(alarmIdToDeal);
            // 无数据或者数据小于页面尺寸，不进行下次循环
            if (CollectionUtils.isEmpty(alarmsInfos) || alarmsInfos.size() < pageSize) {
                break;
            }
            loopTimes++;
            page++;
        }
    }
}
