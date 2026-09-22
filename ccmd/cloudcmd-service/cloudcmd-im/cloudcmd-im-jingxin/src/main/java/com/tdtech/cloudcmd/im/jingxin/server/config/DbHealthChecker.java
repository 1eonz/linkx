package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Slf4j
public class DbHealthChecker {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReportUtil reportUtil;

    public boolean ping() {
        try (Connection conn = dataSource.getConnection()) {
            // 校验并返回
            return conn.isValid(3);
        } catch (SQLException e) {
            // 记录日志后认为不可用
            log.error("ping db error: {}", e.getMessage());
            return false;
        }
    }

    @Scheduled(initialDelay = 10L * 1000L, fixedDelay = 10L * 1000L)
    public void dbTask() {
        boolean ping = ping();
        if (ping) {
            reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.DATABASE_ERROR);
        } else {
            log.error("数据库连接异常");
            reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.DATABASE_ERROR);
        }
    }
}
