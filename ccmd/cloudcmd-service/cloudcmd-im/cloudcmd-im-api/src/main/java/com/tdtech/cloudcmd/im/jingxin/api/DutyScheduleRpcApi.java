package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.im.jingxin.api.entity.duty.DutyScheduleUserVO;

import java.time.LocalDateTime;
import java.util.List;

public interface DutyScheduleRpcApi {

    /**
     * Query duty schedules by IM user ids.
     *
     * @param userIdList IM user ids
     * @param dutyStartDate duty date range start
     * @param dutyEndDate duty date range end
     * @param dutyType duty type
     * @return duty schedule users
     */
    List<DutyScheduleUserVO> listDutyScheduleUsers(List<Long> userIdList, LocalDateTime dutyStartDate,
                                                   LocalDateTime dutyEndDate, Long dutyType);
}
