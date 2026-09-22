package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.module.aiagent.controller.admin.co.GlobalCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertAiSettingsCO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.AiSettingsVO;
import com.chinasoft.cloud.module.aiagent.controller.admin.vo.GlobalVO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Globals;

import java.util.List;

public interface GlobalsService {
    List<Globals> findByIdList(List<Long> idList);

    String findByName(String name);

    AiSettingsVO getAiSettings();

    void updateAiSettings(UpsertAiSettingsCO settingsCO);

    List<GlobalVO> getGlobalSettings(String name);

    Boolean updateGlobalSettings(GlobalCO globalCO);
}
