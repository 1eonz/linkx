package com.tdtech.cloudcmd.icp.proxy;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class IcpProxyApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(IcpProxyApplication.class, args);
    }
}
