package com.tdtech.cloudcmd.script.engine;

import com.tdtech.cloudcmd.script.repo.TicketClientRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

@Singleton
public class JsEngineInitializer {
    private static final Logger log = LoggerFactory.getLogger(JsEngineInitializer.class);
    @Inject
    JsEngine jsEngine;
    @Inject
    TicketClientRepository ticketClientRepository;

    @PostConstruct
    public void postConstruct() {
        log.info("init ScriptController");
        var configs = ticketClientRepository.findByStatus(0);
        if (configs == null || configs.isEmpty()) {
            return;
        }
        for (var config : configs) {
            try {
                jsEngine.loadJs(config.id() + "", config.script());
                log.info("load script:{} {}", config.id(), config.script());
            } catch (ScriptException e) {
                log.error("load script error:{}", config, e);
            }
        }
    }

}
