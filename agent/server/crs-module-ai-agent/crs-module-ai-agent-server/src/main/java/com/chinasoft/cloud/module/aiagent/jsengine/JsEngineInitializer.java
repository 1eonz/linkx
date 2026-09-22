package com.chinasoft.cloud.module.aiagent.jsengine;

import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentConfigMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptException;

@Slf4j
@Component
public class JsEngineInitializer {
    @Resource
    JsEngine jsEngine;
    @Resource
    AgentConfigMapper agentConfigMapper;

    @PostConstruct
    public void postConstruct() {
        log.info("init ScriptController");
        var agentConfigs = agentConfigMapper.selectList();
        if (agentConfigs == null || agentConfigs.isEmpty()) {
            return;
        }
        for (var conf : agentConfigs) {
            if (conf.getParamScript() != null && !conf.getParamScript().isBlank()) {
                loadscript(conf.getId() + JsEngine.PARAM_SUFFIX, conf.getParamScript());
                log.info("load param script {} {}", conf.getId(), conf.getName());
            }
            if (conf.getRespScript() != null && !conf.getRespScript().isBlank()) {
                loadscript(conf.getId() + JsEngine.RESPONSE_SUFFIX, conf.getRespScript());
                log.info("load response script {} {}", conf.getId(), conf.getName());
            }
        }
    }

    private void loadscript(String id, String script) {
        try {
            jsEngine.loadJs(id, script);
        } catch (ScriptException e) {
            log.error("load script error:{} {}", id, script, e);
        }
    }

}
