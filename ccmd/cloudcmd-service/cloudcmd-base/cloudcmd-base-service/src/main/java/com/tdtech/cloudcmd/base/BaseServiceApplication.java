package com.tdtech.cloudcmd.base;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@RefreshScope
@EnableScheduling
public class BaseServiceApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(BaseServiceApplication.class, args);
    }

}
