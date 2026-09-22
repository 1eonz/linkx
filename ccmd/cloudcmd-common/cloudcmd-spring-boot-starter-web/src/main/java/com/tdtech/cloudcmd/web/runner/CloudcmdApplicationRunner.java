package com.tdtech.cloudcmd.web.runner;

import java.util.ServiceLoader;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CloudcmdApplicationRunner {

    private CloudcmdApplicationRunner() {
        throw new UnsupportedOperationException();
    }

    public static ConfigurableApplicationContext run(Class<?> rootClass, String[] args) {
        var springApplicationBuilder =
            new SpringApplicationBuilder(rootClass).bannerMode(Banner.Mode.OFF).allowCircularReferences(true);
        var load = ServiceLoader.load(PropertiesCustomizer.class);
        for (var pc : load) {
            var properties = pc.properties();
            if(properties!=null){
                springApplicationBuilder = springApplicationBuilder.properties(properties);
            }
        }
        return springApplicationBuilder.run(args);
    }

}
