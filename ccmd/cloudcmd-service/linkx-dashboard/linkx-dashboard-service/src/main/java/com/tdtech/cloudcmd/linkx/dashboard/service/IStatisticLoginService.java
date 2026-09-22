package com.tdtech.cloudcmd.linkx.dashboard.service;

import dto.StatisticLoginDTO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * LINKX增加日活数据统计
 */
public interface IStatisticLoginService {

    void statisticLogin(StatisticLoginDTO... statisticLogins);

    void export(String startTime, String endTime, HttpServletResponse response) throws IOException;
}
