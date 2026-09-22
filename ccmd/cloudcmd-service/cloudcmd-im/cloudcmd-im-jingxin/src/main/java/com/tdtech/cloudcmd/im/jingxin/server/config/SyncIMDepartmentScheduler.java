package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

@Slf4j
@Configuration
public class SyncIMDepartmentScheduler {

    @Resource
    private IOrganizationService organizationService;

    @Scheduled(initialDelay = 10000L, fixedDelay = 10L * 60L * 1000L)
    public void scheduled() {
        organizationService.syncFromIM();
    }

}
