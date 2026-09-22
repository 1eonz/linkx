package com.chinasoft.cloud.module.aiagent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
public enum BodyTypeEnum {

    RAW_JSON(1, "raw-json"),
    RAW_TEXT(2, "raw-text"),
    FORM_DATA(3, "form-data");

    private final Integer type;
    private final String name;

    public static BodyTypeEnum valueOf(Integer type) {
        if (type == null) {
            return RAW_JSON;
        }
        for (BodyTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return RAW_JSON;
    }

    public static BodyTypeEnum nameOf(String name) {
        if (StringUtils.isBlank(name)) {
            return RAW_JSON;
        }
        for (BodyTypeEnum e : values()) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        return RAW_JSON;
    }
}