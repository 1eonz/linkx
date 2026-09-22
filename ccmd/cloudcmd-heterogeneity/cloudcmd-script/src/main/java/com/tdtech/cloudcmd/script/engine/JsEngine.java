package com.tdtech.cloudcmd.script.engine;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class JsEngine {

    private static final Logger log = LoggerFactory.getLogger(JsEngine.class);

    private final ScriptEngineManager manager;
    private final ScriptEngine scriptEngine;

    private final Map<String, CompiledScript> scriptMap = new LinkedHashMap<>();

    private final Object runScriptLock = new Object();

    public JsEngine() {
        manager = new ScriptEngineManager();
        scriptEngine = this.manager.getEngineByName("js");
    }

    public void loadJs(String id, String script) throws ScriptException {
        synchronized (runScriptLock) {
            if (script == null) {
                scriptMap.remove(id);
            } else {
                var compilable = ((Compilable)scriptEngine);
                var compiledScript = compilable.compile(script);
                scriptMap.put(id, compiledScript);
            }
        }
    }

    public boolean checkScript(String script) {
        synchronized (runScriptLock) {
            try {
                var compilable = ((Compilable)scriptEngine);
                compilable.compile(script);
                return true;
            } catch (ScriptException e) {
                log.warn("check failed:{}", e.getMessage());
                return false;
            }
        }
    }

    // 并发场景需要并发创建context
    public Object runJs(String id, String params, String config) throws ScriptException {
        synchronized (runScriptLock) {
            if (scriptMap.containsKey(id)) {
                scriptEngine.put("Params", params);
                scriptEngine.put("Config", config);
                scriptMap.get(id).eval(scriptEngine.getContext());
                return scriptEngine.get("Result");
            } else {
                log.error("script named:{} not found", id);
                return null;
            }
        }
    }

    public Object tryJs(String script, String conf, String params) throws ScriptException {
        synchronized (runScriptLock) {
            CompiledScript compiledScript = ((Compilable)scriptEngine).compile(script);
            scriptEngine.put("Params", params);
            scriptEngine.put("Config", conf);
            compiledScript.eval(scriptEngine.getContext());
            return scriptEngine.get("Result");
        }
    }
}
