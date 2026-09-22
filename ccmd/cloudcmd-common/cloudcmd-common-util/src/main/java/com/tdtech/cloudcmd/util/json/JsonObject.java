package com.tdtech.cloudcmd.util.json;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.util.DateFormatUtil;

public class JsonObject extends LinkedHashMap<String, Object> implements Map<String, Object>, Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    public JsonObject() {
        super();
    }

    public JsonObject(Map<String, ?> map) {
        super(map);
    }

    public JsonObject getJSONObject(String key) {
        Object value = super.get(key);

        if (value instanceof JsonObject) {
            return (JsonObject)value;
        }

        if (value instanceof Map) {
            return new JsonObject((Map)value);
        }

        if (value instanceof String) {
            return JsonUtil.parseJson((String)value);
        }

        throw new JsonException("key:" + key + " is not a json object");
    }

    public JsonArray getJSONArray(String key) {
        Object value = super.get(key);

        if (value instanceof JsonArray) {
            return (JsonArray)value;
        }

        if (value instanceof Collection) {
            return new JsonArray((Collection)value);
        }

        if (value instanceof String) {
            return JsonUtil.parseArrayJson((String)value);
        }

        throw new JsonException("key:" + key + " is not a json array");
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> clazz) {
        Object value = super.get(key);
        if (value instanceof String && clazz == String.class) {
            return (T)TypeUtils.castToString(value);
        } else if (value instanceof String) {
            var jsonObject = JsonUtil.parseJson((String)value);
            return JsonUtil.convert(jsonObject, clazz);
        } else {
            return JsonUtil.convert(value, clazz);
        }
    }

    public <T> T getObject(String key, TypeReference<T> typeReference) {
        Object value = super.get(key);
        if (value instanceof String) {
            var jsonObject = JsonUtil.parseJson((String)value);
            return JsonUtil.convert(jsonObject, typeReference);
        } else {
            return JsonUtil.convert(value, typeReference);
        }
    }

    public Boolean getBoolean(String key) {
        Object value = get(key);

        if (value == null) {
            return null;
        }

        return TypeUtils.castToBoolean(value);
    }

    public byte[] getBytes(String key) {
        Object value = get(key);

        if (value == null) {
            return null;
        }

        return TypeUtils.castToBytes(value);
    }

    public Byte getByte(String key) {
        Object value = get(key);

        return TypeUtils.castToByte(value);
    }

    public Short getShort(String key) {
        Object value = get(key);

        return TypeUtils.castToShort(value);
    }

    public Integer getInteger(String key) {
        Object value = get(key);

        return TypeUtils.castToInt(value);
    }

    public Long getLong(String key) {
        Object value = get(key);

        return TypeUtils.castToLong(value);
    }

    public Float getFloat(String key) {
        Object value = get(key);

        return TypeUtils.castToFloat(value);
    }

    public Double getDouble(String key) {
        Object value = get(key);

        return TypeUtils.castToDouble(value);
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = get(key);

        return TypeUtils.castToBigDecimal(value);
    }

    public BigInteger getBigInteger(String key) {
        Object value = get(key);

        return TypeUtils.castToBigInteger(value);
    }

    public String getString(String key) {
        Object value = get(key);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public Date getDate(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date)value;
        }
        if (value instanceof String) {
            return DateFormatUtil.parseDate((String)value, DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
        }
        throw new JsonException("cant parse date name:" + key + " value:" + value);
    }

    public Date getDate(String key, String pattern) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date)value;
        }
        if (value instanceof String) {
            return DateFormatUtil.parseDate((String)value, pattern);
        }
        throw new JsonException("cant parse date name:" + key + " value:" + value);
    }

    @Override
    public JsonObject clone() {
        return new JsonObject(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return JsonUtil.toJsonStr(this);
    }

    public <T> T toJavaObject(Class<T> clazz) {
        return JsonUtil.parseJson(toString(), clazz);
    }

    public <T> T toJavaObject(TypeReference<T> typeReference) {
        return JsonUtil.parseJson(toString(), typeReference);
    }
}
