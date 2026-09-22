package com.tdtech.cloudcmd.im.jingxin.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;

@EnableAspectJAutoProxy
@EnableScheduling
@SpringBootApplication
public class JingXinApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(JingXinApplication.class, args);
    }
}
