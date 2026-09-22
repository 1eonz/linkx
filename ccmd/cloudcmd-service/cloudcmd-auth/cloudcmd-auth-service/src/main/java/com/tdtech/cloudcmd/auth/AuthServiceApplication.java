package com.tdtech.cloudcmd.auth;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@RefreshScope
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(AuthServiceApplication.class, args);
    }

}
