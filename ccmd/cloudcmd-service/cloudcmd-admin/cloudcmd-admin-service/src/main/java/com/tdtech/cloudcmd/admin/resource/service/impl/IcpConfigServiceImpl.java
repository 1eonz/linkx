package com.tdtech.cloudcmd.admin.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tdtech.cloudcmd.admin.message.MqPublisher;
import com.tdtech.cloudcmd.admin.resource.entity.IcpConfig;
import com.tdtech.cloudcmd.admin.resource.entity.vo.IcpConfigVO;
import com.tdtech.cloudcmd.admin.resource.mapper.IcpConfigMapper;
import com.tdtech.cloudcmd.admin.resource.service.IIcpConfigService;
import com.tdtech.cloudcmd.admin.util.enums.ProtocolType;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class IcpConfigServiceImpl implements IIcpConfigService {
    @Resource
    private IcpConfigMapper icpConfigMapper;
    @Autowired
    private MqPublisher mqPublisher;
    @Resource
    private ReportUtil reportUtil;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIcpConfig(IcpConfigVO icpConfigVO) {
        List<IcpConfig> oldIcpConfigs = icpConfigMapper.selectList(new LambdaQueryWrapper<IcpConfig>()
                .eq(IcpConfig::getIp, icpConfigVO.getIp()));
        IcpConfig icpConfig = new IcpConfig();
        BeanUtils.copyProperties(icpConfigVO, icpConfig);
        Date now = new Date();
        icpConfig.setGmtCreated(now);
        icpConfig.setGmtLastModified(now);

        OperationLog operationLog = null;
        if (oldIcpConfigs == null || oldIcpConfigs.isEmpty()) {
            // 如果对接ip发生变化，则删除原有配置，重新插入新配置，并重置部门和层级信息
            icpConfigMapper.delete(new LambdaQueryWrapper<>());
            icpConfig.setCameraLevelId(null);
            icpConfig.setDepartmentId(null);
            icpConfig.setCameraLevelName(null);
            icpConfig.setDepartmentName(null);
            icpConfigMapper.insert(icpConfig);
            operationLog = new OperationLog(OperationTypeEnum.ICP_CONFIG_INSERT);
            operationLog.setOperation(String.format(
                    operationLog.getOperation(),
                    ProtocolType.codeOf(icpConfig.getProtocol()).getName(),
                    icpConfig.getIp(),
                    icpConfig.getPort(),
                    icpConfig.getWssUrl(),
                    icpConfig.getUsername()
            ));
        } else {
            IcpConfig oldConfig = oldIcpConfigs.get(0);
            icpConfig.setId(oldConfig.getId());
            icpConfig.setGmtLastModified(now);
            LambdaQueryWrapper<IcpConfig> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(IcpConfig::getId, icpConfig.getId());
            icpConfigMapper.update(icpConfig, lambdaQueryWrapper);

            List<String> changes = buildLogContents(oldConfig, icpConfig);
            operationLog = new OperationLog(OperationTypeEnum.ICP_CONFIG_UPDATE);
            operationLog.setOperation(String.format(operationLog.getOperation(), String.join("，", changes)));
        }

        mqPublisher.sendIcpConfigMessage(icpConfig);
        UserInfo user = SecurityUtils.getUser();
        if (Objects.nonNull(user)) {
            operationLog.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(operationLog);
    }

    @Override
    public IcpConfig getIcpConfig() {
        IcpConfig icpConfig = null;
        List<IcpConfig> icpConfigs = icpConfigMapper.selectList(new QueryWrapper<>());
        if (CollectionUtils.isNotEmpty(icpConfigs)) {
            icpConfig = icpConfigs.get(0);
        }
        return icpConfig;
    }

    private List<String> buildLogContents(IcpConfig oldConfig, IcpConfig icpConfig) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(oldConfig.getProtocol(), icpConfig.getProtocol())) {
            String oldProtocolName = ProtocolType.codeOf(oldConfig.getProtocol()).getName();
            String newProtocolName = ProtocolType.codeOf(icpConfig.getProtocol()).getName();
            changes.add(formatChangeLog("ICP_CONFIG_PROTOCOL", oldProtocolName, newProtocolName));
        }
        if (!Objects.equals(oldConfig.getIp(), icpConfig.getIp())) {
            changes.add(formatChangeLog("ICP_CONFIG_IP", oldConfig.getIp(), icpConfig.getIp()));
        }
        if (!Objects.equals(oldConfig.getPort(), icpConfig.getPort())) {
            changes.add(formatChangeLog("ICP_CONFIG_PORT", oldConfig.getPort(), icpConfig.getPort()));
        }
        if (!Objects.equals(oldConfig.getWssUrl(), icpConfig.getWssUrl())) {
            changes.add(formatChangeLog("ICP_CONFIG_WSS_URL", oldConfig.getWssUrl(), icpConfig.getWssUrl()));
        }
        if (!Objects.equals(oldConfig.getUsername(), icpConfig.getUsername())) {
            changes.add(formatChangeLog("ICP_CONFIG_USERNAME", oldConfig.getUsername(), icpConfig.getUsername()));
        }
        if (!Objects.equals(oldConfig.getPassword(), icpConfig.getPassword())) {
            changes.add(formatChangeLog("ICP_CONFIG_PASSWORD", "******", "******"));
        }
        if (!Objects.equals(oldConfig.getDepartmentName(), icpConfig.getDepartmentName())) {
            changes.add(formatChangeLog("ICP_CONFIG_DEPARTMENT", oldConfig.getDepartmentName(), icpConfig.getDepartmentName()));
        }
        if (!Objects.equals(oldConfig.getCameraLevelName(), icpConfig.getCameraLevelName())) {
            changes.add(formatChangeLog("ICP_CONFIG_CAMERA_LEVEL", oldConfig.getCameraLevelName(), icpConfig.getCameraLevelName()));
        }
        return changes;
    }

    private String formatChangeLog(String fieldKey, Object oldValue, Object newValue) {
        String fieldName = I18nUtil.get(fieldKey);
        String emptyText = I18nUtil.get("ENG_SYSTEM_CONFIG_EMPTY");
        String oldValueStr = (oldValue == null || oldValue.toString().isEmpty()) ? emptyText : oldValue.toString();
        String newValueStr = (newValue == null || newValue.toString().isEmpty()) ? emptyText : newValue.toString();
        return fieldName + "：{" + oldValueStr + "->" + newValueStr + "}";
    }

}