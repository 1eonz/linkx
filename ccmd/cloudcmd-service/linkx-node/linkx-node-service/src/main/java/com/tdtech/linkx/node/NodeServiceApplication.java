package com.tdtech.linkx.node;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@DubboComponentScan("com.tdtech.linkx.node.rpc")
@RefreshScope
@EnableScheduling
public class NodeServiceApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(NodeServiceApplication.class, args);
    }
}
