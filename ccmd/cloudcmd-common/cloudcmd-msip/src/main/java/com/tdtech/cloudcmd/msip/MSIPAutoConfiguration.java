package com.tdtech.cloudcmd.msip;

import com.tdtech.cloudcmd.msip.aop.KeyedSecondCounter;
import com.tdtech.cloudcmd.msip.aop.LicensedAspect;
import com.tdtech.cloudcmd.msip.aop.LogReportAspect;
import com.tdtech.cloudcmd.msip.aop.RequestLimitAspect;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MSIPAutoConfiguration {
    @Bean
    public ReportUtil reportUtil() {
        return new ReportUtil();
    }

    @Bean
    public LicenseUtil licenseUtil() {
        return new LicenseUtil();
    }

    @Bean
    public LogReportAspect logReportAspect() {
        return new LogReportAspect();
    }

    @Bean
    public RequestLimitAspect requestLimitAspect() {
        return new RequestLimitAspect();
    }

    @Bean
    public LicensedAspect licensedAspect() {
        return new LicensedAspect();
    }

    @Bean
    public KeyedSecondCounter keyedSecondCounter() {
        return new KeyedSecondCounter();
    }
}
