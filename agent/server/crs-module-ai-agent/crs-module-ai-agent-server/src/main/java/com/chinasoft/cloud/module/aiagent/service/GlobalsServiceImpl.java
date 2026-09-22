package com.chinasoft.cloud.module.aiagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinasoft.cloud.framework.common.util.object.BeanUtils;
import com.chinasoft.cloud.framework.utils.IdWorker;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.GlobalCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertAiSettingsCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiSettingsVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.GlobalVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Globals;
import com.chinasoft.cloud.module.aiagent.dal.mysql.GlobalsMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@Slf4j
@Service
@Validated
public class GlobalsServiceImpl implements GlobalsService {
    @Resource
    private GlobalsMapper globalsMapper;
    @Resource
    private IdWorker idWorker;


    @Override
    public List<Globals> findByIdList(List<Long> idList) {
        return globalsMapper.selectByIds(idList);
    }

    @Override
    public String findByName(String name) {
        LambdaQueryWrapper<Globals> queryWrapper = new LambdaQueryWrapper<Globals>()
                .eq(Globals::getName, name);
        Globals globals = globalsMapper.selectOne(queryWrapper);
        return Objects.nonNull(globals) ? globals.getValue() : "";
    }

    @Override
    public AiSettingsVO getAiSettings() {
        Map<String, String> settingsMap = getSettingsMap();
        AiSettingsVO settingsVO = new AiSettingsVO();
        settingsVO.setApprovalEnabled(parseInteger(settingsMap.get(AiAgentGlobalConstants.APPROVAL_ENABLED), 0));
        settingsVO.setApprovalSubMode(settingsMap.get(AiAgentGlobalConstants.APPROVAL_SUB_MODE));
        settingsVO.setApprovalSystemUrl(settingsMap.get(AiAgentGlobalConstants.APPROVAL_SYSTEM_URL));
        return settingsVO;
    }

    @Override
    public void updateAiSettings(UpsertAiSettingsCO settingsCO) {
        Map<String, String> newValues = new LinkedHashMap<>();
        newValues.put(AiAgentGlobalConstants.APPROVAL_ENABLED, stringify(settingsCO.getApprovalEnabled()));
        newValues.put(AiAgentGlobalConstants.APPROVAL_SUB_MODE, settingsCO.getApprovalSubMode());
        newValues.put(AiAgentGlobalConstants.APPROVAL_SYSTEM_URL, settingsCO.getApprovalSystemUrl());

        List<Globals> existedList = globalsMapper.selectList(Wrappers.lambdaQuery(Globals.class)
            .in(Globals::getName, newValues.keySet()));
        Map<String, Globals> existedMap = new LinkedHashMap<>();
        existedList.forEach(item -> existedMap.put(item.getName(), item));

        newValues.forEach((name, value) -> {
            Globals existed = existedMap.get(name);
            if (existed == null) {
                Globals globals = new Globals();
                globals.setId(idWorker.nextId());
                globals.setName(name);
                globals.setValue(value);
                globalsMapper.insert(globals);
                return;
            }
            existed.setValue(value);
            globalsMapper.updateById(existed);
        });
    }

    @Override
    public List<GlobalVO> getGlobalSettings(String name) {
        var wrapper = Wrappers.lambdaQuery(Globals.class);
        if (StringUtils.isNotBlank(name)) {
            wrapper.eq(Globals::getName, name);
        }
        List<Globals> settings = globalsMapper.selectList(wrapper);
        return BeanUtils.toBean(settings, GlobalVO.class);
    }

    @Override
    public Boolean updateGlobalSettings(GlobalCO globalCO) {
        var data = globalsMapper.selectById(globalCO.getId());
        if (data == null) {
            return false;
        }
        boolean needUpdate = false;
        if (!data.getValue().equals(globalCO.getValue())) {
            data.setValue(globalCO.getValue());
            needUpdate = true;
        }
        if (!data.getStatus().equals(globalCO.getStatus())) {
            data.setStatus(globalCO.getStatus());
            needUpdate = true;
        }
        if (needUpdate) {
            data.setGmtModified(new Date());
            globalsMapper.updateById(data);
        }

        return true;
    }

    private Map<String, String> getSettingsMap() {
        List<Globals> settings = globalsMapper.selectList(Wrappers.lambdaQuery(Globals.class)
            .in(Globals::getName, Arrays.asList(
                AiAgentGlobalConstants.APPROVAL_ENABLED,
                AiAgentGlobalConstants.APPROVAL_SUB_MODE,
                AiAgentGlobalConstants.APPROVAL_SYSTEM_URL
            )));
        Map<String, String> settingsMap = new LinkedHashMap<>();
        settings.forEach(item -> settingsMap.put(item.getName(), item.getValue()));
        return settingsMap;
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            log.warn("parse global setting failed, value:{}", value, ex);
            return defaultValue;
        }
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

}
