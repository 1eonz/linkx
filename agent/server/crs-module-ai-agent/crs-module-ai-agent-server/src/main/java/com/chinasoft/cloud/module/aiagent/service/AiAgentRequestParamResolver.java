package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.module.aiagent.enums.BodyTypeEnum;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chinasoft.cloud.framework.common.util.json.JsonUtils;
import com.chinasoft.cloud.module.aiagent.controller.app.co.AskAgentCO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;
import com.chinasoft.cloud.module.aiagent.jsengine.JsEngine;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiAgentRequestParamResolver {

    private static final Pattern PARAM_REF_PATTERN =
        Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}|\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final JsEngine jsEngine;

    @Setter
    private AiAgentFileService aiAgentFileService;

    public AiAgentRequestParamResolver(JsEngine jsEngine) {
        this.jsEngine = jsEngine;
    }

    public AskRequestDefinition resolve(AgentConfig conf, RequestInput input) throws ScriptException {
        AskRequestDefinition definition = new AskRequestDefinition();
        definition.setHttpMethod(StringUtils.defaultIfBlank(conf.getHttpMethod(), "POST"));
        definition.setHeaders(parseHeaders(conf, input));
        definition.setQuery(parseQuery(conf, input));
        if (input.getImExtra() != null && !input.getImExtra().isEmpty()) {
            definition.setBody(JsonUtils.toJsonString(input.getImExtra()));
        } else {
            definition.setBody(parseBody(conf, input));
        }
        definition.setBodyType(conf.getBodyType() != null ? conf.getBodyType() : BodyTypeEnum.RAW_JSON.getType());
        definition.setEndFlag(conf.getEndFlag() != null ? conf.getEndFlag() : 1);
        if (BodyTypeEnum.FORM_DATA.getType().equals(definition.getBodyType())) {
            definition.setFormDataParts(parseFormDataParts(conf, input));
        }
        return definition;
    }

    public JSONObject buildParamContext(AgentConfig conf, RequestInput input) {
        JSONObject config = buildConfigJson(conf);
        JSONObject inputJson = input.toJson();
        JSONObject result = new JSONObject(true);
        result.putAll(config);
        result.putAll(inputJson);
        result.put("config", config);
        result.put("input", inputJson);

        // 新增：添加文件相关变量（从缓存获取）
        if (StringUtils.isNotBlank(input.getSessionId()) && aiAgentFileService != null) {
            AiFileInfo fileInfo = aiAgentFileService.getFileInfo(input.getSessionId());
            if (fileInfo != null) {
                result.put("fileId", fileInfo.getFileId());
                result.put("fileName", fileInfo.getFileName());       // 原始文件名
                result.put("filePath", fileInfo.getFilePath());       // 本地路径
                result.put("fileUrl", fileInfo.getFileUrl());         // 访问URL
                result.put("fileCategory", fileInfo.getFileCategory()); // 文件类别
                result.put("file", new JSONObject(true) {{
                    put("id", fileInfo.getFileId());
                    put("name", fileInfo.getFileName());
                    put("path", fileInfo.getFilePath());
                    put("url", fileInfo.getFileUrl());
                    put("category", fileInfo.getFileCategory());
                }});
            }
        }

        if (StringUtils.isNotBlank(input.getAttachement())) {
            result.put("attachment", input.getAttachement());
        }

        if (input.getImExtra() != null && !input.getImExtra().isEmpty()) {
            result.putAll(input.getImExtra());
            result.put("imExtra", input.getImExtra());
        }

        return result;
    }

    public void mergeScriptResult(AskRequestDefinition definition, Object data) {
        if (!(data instanceof Map<?, ?> scriptMap)) {
            definition.setBody(JsonUtils.toJsonString(data));
            return;
        }
        JSONObject valueJson = toJsonObject(scriptMap);
        if (valueJson.containsKey("header")) {
            definition.setHeaders(normalizeStringMap(valueJson.get("header")));
        }
        if (valueJson.containsKey("query")) {
            definition.setQuery(normalizeObjectMap(valueJson.get("query")));
        }
        if (valueJson.containsKey("body")) {
            definition.setBody(toRequestBody(valueJson.get("body")));
        } else if (!containsAnyRequestPart(valueJson)) {
            definition.setBody(JsonUtils.toJsonString(valueJson));
        }
        Object httpMethod = valueJson.get("httpMethod");
        if (httpMethod == null) {
            httpMethod = valueJson.get("method");
        }
        if (httpMethod != null) {
            definition.setHttpMethod(String.valueOf(httpMethod));
        }
    }

    private Map<String, String> parseHeaders(AgentConfig conf, RequestInput input) throws ScriptException {
        Map<String, String> result = normalizeStringMap(parseConfigPart(conf.getHeader(), conf, input));
        if (StringUtils.isBlank(result.get("Content-Type")) && StringUtils.isNotBlank(conf.getBody())) {
            result.put("Content-Type", "application/json; charset=utf-8");
        }
        return result;
    }

    private Map<String, Object> parseQuery(AgentConfig conf, RequestInput input) throws ScriptException {
        return normalizeObjectMap(parseConfigPart(conf.getQuery(), conf, input));
    }

    private String parseBody(AgentConfig conf, RequestInput input) throws ScriptException {
        if (StringUtils.isBlank(conf.getBody())) {
            return "";
        }
        return toRequestBody(parseConfigPart(conf.getBody(), conf, input));
    }

    private Map<String, String> parseFormDataParts(AgentConfig conf, RequestInput input) throws ScriptException {
        Map<String, String> result = new LinkedHashMap<>();
        if (StringUtils.isBlank(conf.getBody())) {
            return result;
        }
        Object parsed = parseConfigPart(conf.getBody(), conf, input);
        if (parsed instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (key != null && value != null) {
                    result.put(String.valueOf(key), String.valueOf(value));
                }
            });
        } else if (parsed instanceof JSONObject json) {
            json.forEach((key, value) -> {
                if (key != null && value != null) {
                    result.put(key, String.valueOf(value));
                }
            });
        }
        return result;

    }

    private Object parseConfigPart(String text, AgentConfig conf, RequestInput input) throws ScriptException {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        JSONObject params = buildParamContext(conf, input);
        try {
            Object parsed = JSON.parse(text);
            return resolveReferences(parsed, params);
        } catch (Exception ignore) {
            Object data = jsEngine.tryJs(text, JsonUtils.toJsonString(conf), params.toJSONString());
            if (data == null) {
                throw new ScriptException("script run error");
            }
            return data;
        }
    }

    private Object resolveReferences(Object value, JSONObject params) {
        if (value instanceof JSONObject jsonObject) {
            JSONObject result = new JSONObject(true);
            jsonObject.forEach((key, item) -> result.put(key, resolveReferences(item, params)));
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            JSONObject result = new JSONObject(true);
            map.forEach((key, item) -> {
                if (key != null) {
                    result.put(String.valueOf(key), resolveReferences(item, params));
                }
            });
            return result;
        }
        if (value instanceof JSONArray array) {
            JSONArray result = new JSONArray();
            array.forEach(item -> result.add(resolveReferences(item, params)));
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            JSONArray result = new JSONArray();
            iterable.forEach(item -> result.add(resolveReferences(item, params)));
            return result;
        }
        if (value instanceof String text) {
            return resolveTextReference(text, params);
        }
        return value;
    }

    private Object resolveTextReference(String text, JSONObject params) {
        Matcher wholeMatcher = PARAM_REF_PATTERN.matcher(text);
        if (wholeMatcher.matches()) {
            Object value = getJsonPathValue(params, firstNonBlank(wholeMatcher.group(1), wholeMatcher.group(2)));
            return value == null ? "" : value;
        }
        Matcher matcher = PARAM_REF_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Object value = getJsonPathValue(params, firstNonBlank(matcher.group(1), matcher.group(2)));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Object getJsonPathValue(JSONObject params, String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        Object value = params;
        for (String part : path.split("\\.")) {
            if (!(value instanceof JSONObject jsonObject)) {
                return null;
            }
            value = jsonObject.get(part);
        }
        return value;
    }

    private static JSONObject buildConfigJson(AgentConfig conf) {
        JSONObject result = (JSONObject)JSON.toJSON(conf);
        result.put("configId", conf.getId());
        result.put("agentId", conf.getId());
        result.put("agentName", conf.getName());
        result.put("queryConfig", conf.getQuery());
        result.put("bodyConfig", conf.getBody());
        return result;
    }

    private static JSONObject toJsonObject(Map<?, ?> source) {
        JSONObject result = new JSONObject(true);
        source.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private static boolean containsAnyRequestPart(JSONObject valueJson) {
        return valueJson.containsKey("header") || valueJson.containsKey("query") || valueJson.containsKey("body")
            || valueJson.containsKey("httpMethod") || valueJson.containsKey("method");
    }

    private static Map<String, Object> normalizeObjectMap(Object value) {
        if (value == null) {
            return new HashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (key != null) {
                    result.put(String.valueOf(key), item);
                }
            });
            return result;
        }
        throw new IllegalArgumentException("request param must be json object");
    }

    private static Map<String, String> normalizeStringMap(Object value) {
        Map<String, Object> rawMap = normalizeObjectMap(value);
        Map<String, String> result = new LinkedHashMap<>();
        rawMap.forEach((key, item) -> {
            if (item != null) {
                result.put(key, String.valueOf(item));
            }
        });
        return result;
    }

    private static String toRequestBody(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        return JsonUtils.toJsonString(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    @Getter
    @Setter
    @ToString
    public static class RequestInput {

        private Long agent;
        private String content;
        private String userName;
        private String userID;
        private String departmentCode;
        private String departmentId;
        private String departmentName;
        private String approver;
        private String sessionId;
        private String attachement;
        // 推送 IM 智能体所需的特殊字段，仅 askType=2/3 透传场景使用
        private JSONObject imExtra;

        public static RequestInput from(AskAgentCO askAgentCO) {
            RequestInput input = new RequestInput();
            input.setAgent(askAgentCO.getAgent());
            input.setContent(askAgentCO.getContent());
            input.setUserName(askAgentCO.getUserName());
            input.setUserID(askAgentCO.getUserID());
            input.setDepartmentCode(askAgentCO.getDepartmentCode());
            input.setDepartmentId(askAgentCO.getDepartmentId());
            input.setDepartmentName(askAgentCO.getDepartmentName());
            input.setApprover(askAgentCO.getApprover());
            input.setSessionId(askAgentCO.getSessionId());
            input.setImExtra(askAgentCO.getImExtra());
            return input;
        }

        public static RequestInput from(AgentRecord agentRecord) {
            RequestInput input = new RequestInput();
            input.setContent(agentRecord.getQueryContent());
            input.setUserName(agentRecord.getUserName());
            input.setUserID(agentRecord.getIdentityCardNumber());
            input.setDepartmentCode(agentRecord.getDepartmentCode());
            input.setDepartmentId(agentRecord.getDepartmentId());
            input.setDepartmentName(agentRecord.getDepartmentName());
            input.setAttachement(agentRecord.getAttachement());
            return input;
        }

        public JSONObject toJson() {
            JSONObject result = (JSONObject)JSON.toJSON(this);
            result.put("query", content);
            result.put("userId", userID);
            return result;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class AskRequestDefinition {

        private String httpMethod;
        private Map<String, String> headers = new HashMap<>();
        private Map<String, Object> query = new HashMap<>();
        private String body = "";

        private Integer bodyType;
        private Map<String, String> formDataParts = new LinkedHashMap<>();
        private Integer endFlag;

        public static AskRequestDefinition legacy(String httpMethod, String body) {
            AskRequestDefinition definition = new AskRequestDefinition();
            definition.setHttpMethod(StringUtils.defaultIfBlank(httpMethod, "POST"));
            definition.setBody(body);
            return definition;
        }
    }
}