package com.tdtech.cloudcmd.cagent;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.tdtech.cloudcmd.cagent.conf.CagentProperties;
import com.tdtech.cloudcmd.cagent.remote.RemoteConfigurationProperties;
import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;

@EnableScheduling
@EnableConfigurationProperties({CagentProperties.class, RemoteConfigurationProperties.class})
@SpringBootApplication
public class CagentApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(CagentApplication.class, args);
    }

}
