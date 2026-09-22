package com.tdtech.cloudcmd.im.jingxin.server.chatmessagehandler;

import com.tdtech.cloudcmd.im.jingxin.client.CachedImConfig;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsDepartmentEvents;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsDepartmentNoticeMessage;
import com.tdtech.cloudcmd.im.jingxin.client.entity.WsResponse;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Organization;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class IMDepartmentNoticeMessageHandler {

    @Resource
    private IOrganizationService organizationService;

    @Resource
    private CachedImConfig cachedImConfig;


    @Bean("departmentNoticeMessage")
    public Consumer<WsResponse> processWsMessage() {
        return wsResponse -> {
            try {
                if (!Objects.equals(wsResponse.getModule(), "addressbook")
                        || !Objects.equals(wsResponse.getNotifyType(), "departmentNotice")) {
                    return;
                }
                log.info("departmentNoticeMessage on message: {}", wsResponse);
                String signFlag = cachedImConfig.getConfig("DEPARTMENT_SYNC_SIGN");
                boolean isClosed = StringUtils.isBlank(signFlag) || (!"true".equals(signFlag));
                if (isClosed) {
                    log.info("departmentNoticeMessage isClosed");
                    return;
                }
                var message = JsonUtil.convert(wsResponse.getData(), WsDepartmentNoticeMessage.class);
                processDepartmentNoticeMsg(message);
            } catch (Exception e) {
                log.error("departmentNoticeMessage consume error", e);
            }
        };
    }

    private void processDepartmentNoticeMsg(WsDepartmentNoticeMessage message) {
        List<WsDepartmentEvents> departmentEvents = message.getDepartmentEvents();
        if (CollectionUtils.isEmpty(departmentEvents)) {
            return;
        }
        List<Organization> saveOrUpdateList = departmentEvents.stream()
                .filter(dept -> Arrays.asList(1, 2).contains(dept.getOperateType()))
                .map(dept -> new Organization(dept.getDepartment()))
                .collect(Collectors.toList());
        List<Long> deleteIdList = departmentEvents.stream()
                .filter(dept -> 3 == dept.getOperateType())
                .map(WsDepartmentEvents::getDepartmentId)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(saveOrUpdateList)) {
            organizationService.saveOrUpdateBatch(saveOrUpdateList);
        }
        if (CollectionUtils.isNotEmpty(deleteIdList)) {
            organizationService.removeByIds(deleteIdList);
        }
        organizationService.deleteCache();
    }

}
