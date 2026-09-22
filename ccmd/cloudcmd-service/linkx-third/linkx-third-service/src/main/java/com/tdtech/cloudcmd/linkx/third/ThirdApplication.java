package com.tdtech.cloudcmd.linkx.third;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class ThirdApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(ThirdApplication.class, args);
    }
}