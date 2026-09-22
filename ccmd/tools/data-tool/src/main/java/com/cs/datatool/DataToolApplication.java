package com.cs.datatool;

import com.cs.datatool.utils.HttpClientConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({HttpClientConfigurationProperties.class})
@SpringBootApplication
public class DataToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataToolApplication.class, args);
    }

}
