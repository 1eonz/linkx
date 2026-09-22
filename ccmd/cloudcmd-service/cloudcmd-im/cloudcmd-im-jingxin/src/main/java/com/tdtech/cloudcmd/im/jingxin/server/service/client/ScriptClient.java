package com.tdtech.cloudcmd.im.jingxin.server.service.client;

import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicket;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketService;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ScriptClient {

    private static final String HOST = "http://cloudcmd-script:8080";
    private static final String loadScriptURL = "/script/load";
    private static final String tryURL = "/script/try";
    private static final String checkScript = "/script/check";

    private final HttpClient httpClient;
    private final PoliceTicketService policeTicketService;

    @SneakyThrows
    public Boolean checkScript(String script) {
        return httpClient.postJson(new URI(HOST + checkScript), null, script, Boolean.class);
    }

    @SneakyThrows
    public void loadScript(Long configId) {
        httpClient.postJson(new URI(HOST + loadScriptURL), null, configId, Void.class);
    }

    @SneakyThrows
    public Object tryScript(String script, String payload) {
        return httpClient.postJson(new URI(HOST + tryURL), null, Map.of("script", script, "params", payload), Object.class);
    }

    @Bean("policeTicketResult")
    public Consumer<String> consumeProcessResult() {
        return result -> {
            log.info("policeTicketResult:{}", result);
            var policeTicket = JsonUtil.parseJson(result, PoliceTicket.class);
            policeTicketService.processPoliceTicket(policeTicket);
        };
    }
}
