package com.chinasoft.cloud.module.aiagent.jsengine;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class JsEngine {

    public static final String PARAM_SUFFIX = "_param";
    public static final String RESPONSE_SUFFIX = "_resp";

    private final ScriptEngineManager manager;
    @Getter
    private final ScriptEngine scriptEngine;

    private final Map<String, CompiledScript> scriptMap = new LinkedHashMap<>();

    private final Object runScriptLock = new Object();

    public JsEngine() {
        this.manager = new ScriptEngineManager();
        this.scriptEngine = this.manager.getEngineByName("js");
    }

    public static void main(String[] args) throws ScriptException {
        var jsEngine = new JsEngine();
        jsEngine.loadJs("1", """
            Result = function(p,c){
                if(!p.startsWith("data:")){
                    return null
                }
                p=p.substring(5)
                var pj=JSON.parse(p)
                if(pj.event=="text_chunk"){
                    return {
                        "event": "message",
                        "answer": pj.data.text
                    }
                }else if(pj.event=="workflow_finished"){
                    return {
                        "event": "message_end",
                        "status": pj.data.status,
                        "error": pj.data.error
                    }
                }else{
                    return null;
                }
            }(Params,Config)
            """);
        var o = jsEngine.runJs("1", """
            data:{
            "event": "workflow_finished",
            "data": {
            "status": "succeeded",
            "error": "error"
            }
            }
            """, """
            {
                "id": 1234,
                "name": "name"
            }
            """);
        System.out.println(o);
    }

    public void loadJs(String id, String script) throws ScriptException {
        synchronized (runScriptLock) {
            if (script == null) {
                scriptMap.remove(id);
            } else {
                var compilable = ((Compilable)getScriptEngine());
                var compiledScript = compilable.compile(script);
                scriptMap.put(id, compiledScript);
            }
        }
    }

    public boolean checkScript(String script) {
        synchronized (runScriptLock) {
            try {
                var compilable = ((Compilable)getScriptEngine());
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
                var scriptEngineEntity = getScriptEngine();
                scriptEngineEntity.put("Params", params);
                scriptEngineEntity.put("Config", config);
                scriptMap.get(id).eval(scriptEngineEntity.getContext());
                return scriptEngineEntity.get("Result");
            } else {
                log.error("script named:{} not found", id);
                return null;
            }
        }
    }

    public Object tryJs(String script, String conf, String params) throws ScriptException {
        synchronized (runScriptLock) {
            var scriptEngineEntity = getScriptEngine();
            CompiledScript compiledScript = ((Compilable)scriptEngineEntity).compile(script);
            scriptEngineEntity.put("Params", params);
            scriptEngineEntity.put("Config", conf);
            compiledScript.eval(scriptEngineEntity.getContext());
            return scriptEngineEntity.get("Result");
        }
    }
}
