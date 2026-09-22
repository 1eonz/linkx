package com.tdtech.cloudcmd.linkx.dashboard;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class DashboardApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(DashboardApplication.class, args);
    }
}
