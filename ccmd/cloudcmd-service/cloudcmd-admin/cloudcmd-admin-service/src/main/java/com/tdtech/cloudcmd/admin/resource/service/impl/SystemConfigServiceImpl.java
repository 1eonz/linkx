package com.tdtech.cloudcmd.admin.resource.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.admin.resource.entity.Globals;
import com.tdtech.cloudcmd.admin.resource.mapper.GlobalsMapper;
import com.tdtech.cloudcmd.base.api.service.CacheRpcService;
import com.tdtech.cloudcmd.admin.util.constant.Constants;
import com.tdtech.cloudcmd.admin.util.enums.CreateGroupType;
import com.tdtech.cloudcmd.admin.util.enums.SystemConfigType;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tdtech.cloudcmd.admin.resource.entity.SystemConfig;
import com.tdtech.cloudcmd.admin.resource.mapper.SystemConfigMapper;
import com.tdtech.cloudcmd.admin.resource.service.ISystemConfigService;
import com.tdtech.cloudcmd.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 系统配置 Service 实现类
 *
 * @author: S063874
 * @date: 2026-03-10 14:05
 */
@Slf4j
@Service
public class SystemConfigServiceImpl implements ISystemConfigService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private IdWorker idWorker;

    @Autowired
    private GlobalsMapper globalsMapper;

    @DubboReference
    private CacheRpcService cacheRpcService;

    @Resource
    private ReportUtil reportUtil;

    @Override
    public Long createSystemConfig(SystemConfig systemConfig) {
        // 检查是否已存在相同key的配置
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getKey, systemConfig.getKey());
        SystemConfig existingConfig = systemConfigMapper.selectOne(queryWrapper);
        if (existingConfig != null) {
            throw new RuntimeException("系统配置已存在");
        }

        systemConfig.setId(idWorker.nextId());
        Date now = new Date();
        systemConfig.setGmtCreateTime(now);
        systemConfig.setGmtLastModified(now);

        systemConfigMapper.insert(systemConfig);
        return systemConfig.getId();
    }

    @Override
    public void updateSystemConfig(SystemConfig systemConfig) {
        // 校验存在
        validateSystemConfigExists(systemConfig.getId());
        Long id = systemConfig.getId();
        String newValue = systemConfig.getValue();
        SystemConfig oldConfig = getSystemConfigById(id);
        String oldValue = oldConfig != null ? oldConfig.getValue() : null;
        
        // 更新
        systemConfig.setGmtLastModified(new Date());
        systemConfigMapper.updateById(systemConfig);
        if(2031323063807125505L == id && StringUtils.isNotBlank(newValue)){
            // 同步更新全局参数的标题
            JSONObject jsonObject = JSON.parseObject(newValue);
            String title = (String) jsonObject.get("value");
            if(StringUtils.isNotBlank(title)){
                Globals globals = new Globals();
                globals.setId(1457528867697822379L);
                globals.setValue(title);
                globalsMapper.updateById(globals);
                cacheRpcService.initGlobals();
            }
        }
        reportOperationLog(id, oldValue, newValue);
    }

    private void reportOperationLog(Long id, String oldValue, String newValue) {
        if(id != 2031324046037624837L){
            if (SystemConfigType.CREAT_GROUP_CONFIG.getId().equals(id)) {
                reportCreateGroupConfigChange(oldValue, newValue);
            } else if (SystemConfigType.PC_NAV_CUSTOM.getId().equals(id)) {
                reportPcNavCustomChange(oldValue, newValue);
            } else {
                reportOtherChange(oldValue, newValue);
            }
        }
    }

    private void reportOtherChange(String oldValue, String newValue) {
        if (StringUtils.isBlank(oldValue) || StringUtils.isBlank(newValue)) {
            return;
        }
        OperationLog operationLog = new OperationLog(OperationTypeEnum.GROUP_SYSTEM_UPDATE);

        JSONObject oldJO;
        JSONObject newJO;
        try {
            oldJO = JSONObject.parseObject(oldValue);
            newJO = JSONObject.parseObject(newValue);
        } catch (Exception e) {
            // 纯值配置（如加密开关 true/false）非JSON结构，无变更明细可记录，跳过日志
            return;
        }

        String oldV = oldJO.getString("value");
        String newV = newJO.getString("value");
        String newName = newJO.getString("name");

        String logContext =  String.format("%s: %s -> %s", newName, oldV, newV);
        operationLog.setOperation(String.format(operationLog.getOperation(), logContext));
        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }
        reportUtil.saveOperationLog(operationLog);
    }

    private void reportCreateGroupConfigChange(String oldValue, String newValue) {
        Map<Integer, JSONObject> oldDataMap = parseGroupConfigToMap(oldValue);
        Map<Integer, JSONObject> newDataMap = parseGroupConfigToMap(newValue);
        
        StringBuilder allChanges = new StringBuilder();
        for (CreateGroupType groupType : CreateGroupType.values()) {
            Integer type = groupType.getCode();
            JSONObject oldItem = oldDataMap.get(type);
            JSONObject newItem = newDataMap.get(type);

            String changeLog = buildChangeLog(groupType, oldItem, newItem);
            if (StringUtils.isNotBlank(changeLog)) {
                if (allChanges.length() > 0) {
                    allChanges.append(Constants.SEMICOLON_ZH);
                }
                allChanges.append(changeLog);
            }
        }
        
        if (allChanges.length() > 0) {
            saveOperationLog(allChanges.toString());
        }
    }

    private Map<Integer, JSONObject> parseGroupConfigToMap(String value) {
        Map<Integer, JSONObject> result = new HashMap<>();
        if (StringUtils.isBlank(value)) {
            return result;
        }
        try {
            List<JSONObject> list = JSON.parseArray(value, JSONObject.class);
            for (JSONObject item : list) {
                Integer type = item.getInteger("type");
                if (type != null) {
                    result.put(type, item);
                }
            }
        } catch (Exception e) {
            log.error("解析建群配置JSON失败: {}", value, e);
        }
        return result;
    }

    private String buildChangeLog(CreateGroupType groupType, JSONObject oldItem, JSONObject newItem) {
        String typeName = I18nUtil.get(groupType.getName());
        StringBuilder logBuilder = new StringBuilder();

        String oldName = oldItem != null ? oldItem.getString("name") : null;
        String newName = newItem != null ? newItem.getString("name") : null;
        String oldEnable = oldItem != null ? oldItem.getString("enable") : null;
        String newEnable = newItem != null ? newItem.getString("enable") : null;

        String emptyText = I18nUtil.get("ENG_SYSTEM_CONFIG_EMPTY");
        String semicolon = I18nUtil.get("ENG_SYSTEM_CONFIG_SEMICOLON");
        String nameLabel = I18nUtil.get("ENG_SYSTEM_CONFIG_NAME_LABEL");
        String enableStatus = I18nUtil.get("ENG_SYSTEM_CONFIG_ENABLE_STATUS");

        // 比对name变化
        if (!StringUtils.equals(oldName, newName)) {
            if (logBuilder.length() > 0) {
                logBuilder.append(semicolon);
            }
            logBuilder.append(typeName).append(nameLabel)
                    .append(oldName == null ? emptyText : oldName)
                    .append("->")
                    .append(newName == null ? emptyText : newName);
        }

        // 比对enable变化
        if (!StringUtils.equals(oldEnable, newEnable)) {
            if (logBuilder.length() > 0) {
                logBuilder.append(semicolon);
            }
            logBuilder.append(typeName).append(enableStatus)
                    .append(oldEnable == null ? emptyText : oldEnable)
                    .append("->")
                    .append(newEnable == null ? emptyText : newEnable);
        }

        return logBuilder.toString();
    }

    private void saveOperationLog(String operation) {
        OperationLog operationLog = new OperationLog(OperationTypeEnum.GROUP_SYSTEM_UPDATE);
        operationLog.setOperation(String.format(operationLog.getOperation(), operation));

        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }

        reportUtil.saveOperationLog(operationLog);
    }

    private void reportPcNavCustomChange(String oldValue, String newValue) {

        Map<String, JSONObject> oldDataMap = parseNavConfigToMap(oldValue);
        Map<String, JSONObject> newDataMap = parseNavConfigToMap(newValue);

        List<JSONObject> addedItems = new ArrayList<>();
        List<JSONObject> removedItems = new ArrayList<>();

        for (Map.Entry<String, JSONObject> entry : newDataMap.entrySet()) {
            if (!oldDataMap.containsKey(entry.getKey())) {
                addedItems.add(entry.getValue());
            }
        }

        for (Map.Entry<String, JSONObject> entry : oldDataMap.entrySet()) {
            if (!newDataMap.containsKey(entry.getKey())) {
                removedItems.add(entry.getValue());
            }
        }


        if (addedItems.isEmpty() && removedItems.isEmpty()) {
            savePcNavOperationLog(newValue);
            return;
        }

        for (JSONObject item : addedItems) {
            String log = buildNavAddLog(item);
            savePcNavOperationLog(log);
        }

        for (JSONObject item : removedItems) {
            String log = buildNavRemoveLog(item);
            savePcNavOperationLog(log);
        }
    }

    private Map<String, JSONObject> parseNavConfigToMap(String value) {
        Map<String, JSONObject> result = new HashMap<>();
        if (StringUtils.isBlank(value)) {
            return result;
        }
        try {
            List<JSONObject> list = JSON.parseArray(value, JSONObject.class);
            for (JSONObject item : list) {
                String name = item.getString("name");
                if (StringUtils.isNotBlank(name)) {
                    result.put(name, item);
                }
            }
        } catch (Exception e) {
            log.error("解析导航配置JSON失败: {}", value, e);
        }
        return result;
    }

    private String buildNavAddLog(JSONObject item) {
        String name = item.getString("name");
        String url = item.getString("url");
        Integer order = item.getInteger("order");
        String emptyText = I18nUtil.get("ENG_SYSTEM_CONFIG_EMPTY");
        return String.format(I18nUtil.get("ENG_SYSTEM_CONFIG_NAV_ADD"),
                name == null ? emptyText : name,
                url == null ? emptyText : url,
                order == null ? emptyText : order);
    }

    private String buildNavRemoveLog(JSONObject item) {
        String name = item.getString("name");
        String url = item.getString("url");
        Integer order = item.getInteger("order");
        String emptyText = I18nUtil.get("ENG_SYSTEM_CONFIG_EMPTY");
        return String.format(I18nUtil.get("ENG_SYSTEM_CONFIG_NAV_REMOVE"),
                name == null ? emptyText : name,
                url == null ? emptyText : url,
                order == null ? emptyText : order);
    }

    private void savePcNavOperationLog(String operation) {
        OperationLog operationLog = new OperationLog(OperationTypeEnum.PC_CONFIG_UPDATE);
        operationLog.setOperation(String.format(operationLog.getOperation(), operation));

        UserInfo user = SecurityUtils.getUser();
        if (user != null) {
            operationLog.setOperator(user.getUserName());
        }

        reportUtil.saveOperationLog(operationLog);
    }

    @Override
    public void deleteSystemConfig(Long id) {
        // 校验存在
        validateSystemConfigExists(id);
        // 删除
        systemConfigMapper.deleteById(id);
    }

    @Override
    public void deleteSystemConfigByIds(List<Long> ids) {
        // 校验存在
        validateSystemConfigExists(ids);
        // 删除
        systemConfigMapper.deleteBatchIds(ids);
    }

    @Override
    public SystemConfig getSystemConfigById(Long id) {
        return systemConfigMapper.selectById(id);
    }

    @Override
    public SystemConfig getSystemConfigByKey(String key) {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getKey, key);
        return systemConfigMapper.selectOne(queryWrapper);
    }

    @Override
    public List<SystemConfig> listSystemConfig() {
        return systemConfigMapper.selectList(null);
    }

    @Override
    public String getConfigValue(String key) {
        SystemConfig systemConfig = getSystemConfigByKey(key);
        return systemConfig != null ? systemConfig.getValue() : null;
    }

    /**
     * 校验系统配置是否存在
     *
     * @param id 编号
     */
    private void validateSystemConfigExists(Long id) {
        if (systemConfigMapper.selectById(id) == null) {
            throw new RuntimeException("系统配置不存在");
        }
    }

    /**
     * 校验系统配置是否存在
     *
     * @param ids 编号列表
     */
    private void validateSystemConfigExists(List<Long> ids) {
        List<SystemConfig> list = systemConfigMapper.selectBatchIds(ids);
        if (CollectionUtils.isEmpty(list) || list.size() != ids.size()) {
            throw new RuntimeException("系统配置不存在");
        }
    }
}
