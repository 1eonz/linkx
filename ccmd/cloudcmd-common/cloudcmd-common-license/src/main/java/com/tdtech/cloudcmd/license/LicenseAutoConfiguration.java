package com.tdtech.cloudcmd.license;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LicenseAutoConfiguration {

//    @Bean
//    public LicenseClient licenseClient() {
//        return new BufferedLicenseClient();
//    }

    @Bean
    public LicensePredicate licensePredicate() {
        return new LicensePredicate();
    }

}
