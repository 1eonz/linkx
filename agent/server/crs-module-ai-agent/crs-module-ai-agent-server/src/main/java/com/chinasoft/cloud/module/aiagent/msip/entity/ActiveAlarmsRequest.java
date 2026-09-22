package com.chinasoft.cloud.module.aiagent.msip.entity;

import com.chinasoft.cloud.module.aiagent.msip.enums.AlarmTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Slf4j
public class ActiveAlarmsRequest {
    private String alarmId;
    private String location = "LinkX";     //这里alarmId和location定义为唯一索引
    // ip
    private String src;
    private String originSystemTag = "LinkX";
    private String migFlag;
    private Map<String, Object> params;   //这里添加参数 对应模板里的占位项


    public ActiveAlarmsRequest() {
        super();
    }

    public ActiveAlarmsRequest(AlarmTemplate alarmTemplate) {
        this();
        this.setAlarmId(alarmTemplate.getAlarmId());
        this.setSrc(System.getenv("SERVER_IP"));
    }

    public ActiveAlarmsRequest(AlarmTemplate alarmTemplate, Object... params) {
        this(alarmTemplate);
        boolean isNeedFormat = Objects.nonNull(params) && params.length > 0;
        if (isNeedFormat) {
            try {
                String template = alarmTemplate.getTemplate();
                Map<String, Object> map = extractPlaceholders(template, params);
                log.info("ActiveAlarms map: {}", map);
                this.setParams(map);
            } catch (Exception e) {
                log.error("ActiveAlarms format param error: {}", e.getMessage());
            }
        }
    }

    /**
     * 将模板中的{{占位符}}与传入参数封装成Map
     * @param template 模板字符串
     * @param params 可变参数，按顺序对应模板中的占位符
     * @return Map<占位符名称, 参数值>
     */
    public static Map<String, Object> extractPlaceholders(String template, Object... params) {
        Map<String, Object> result = new HashMap<>();

        // 正则匹配 {{xxx}}
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(template);

        int index = 0;
        while (matcher.find()) {
            // 获取占位符名称（去掉{{}}）
            String key = matcher.group(1);

            // 对应参数值
            Object value = index < params.length ? params[index] : null;

            result.put(key, value);
            index++;
        }

        return result;
    }
}
