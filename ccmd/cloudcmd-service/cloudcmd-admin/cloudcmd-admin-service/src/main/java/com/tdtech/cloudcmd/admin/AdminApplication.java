package com.tdtech.cloudcmd.admin;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.tdtech.cloudcmd.web.runner.CloudcmdApplicationRunner;

/**
 * @author : mWX556161
 * @date : 2020-05-23 10:06
 */
@SpringBootApplication
@RefreshScope
@EnableScheduling
public class AdminApplication {

    public static void main(String[] args) {
        CloudcmdApplicationRunner.run(AdminApplication.class, args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
