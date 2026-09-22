package com.cs.datatool.utils.json;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.cs.datatool.utils.DateFormatUtil;
import com.cs.datatool.utils.JsonUtil;
import com.cs.datatool.utils.TypeUtils;
import com.fasterxml.jackson.core.type.TypeReference;

public class JsonArray extends LinkedList<Object> implements List<Object>, Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    public JsonArray() {}

    public JsonArray(Collection<?> c) {
        super(c);
    }

    public JsonObject getJSONObject(Integer index) {
        Object value = super.get(index);

        if (value instanceof JsonObject) {
            return (JsonObject)value;
        }

        if (value instanceof Map) {
            return new JsonObject((Map)value);
        }

        if (value instanceof String) {
            return JsonUtil.parseJson((String)value);
        }

        throw new JsonException("index:" + index + " is not a json object");
    }

    public JsonArray getJSONArray(Integer index) {
        Object value = super.get(index);

        if (value instanceof JsonArray) {
            return (JsonArray)value;
        }

        if (value instanceof Collection) {
            return new JsonArray((Collection)value);
        }

        if (value instanceof String) {
            return JsonUtil.parseArrayJson((String)value);
        }

        throw new JsonException("index:" + index + " is not a json array");
    }

    public <T> T getObject(Integer index, Class<T> clazz) {
        var obj = getJSONObject(index);
        return obj.toJavaObject(clazz);
    }

    public <T> T getObject(Integer index, TypeReference<T> typeReference) {
        var obj = getJSONObject(index);
        return obj.toJavaObject(typeReference);
    }

    public Boolean getBoolean(Integer index) {
        Object value = super.get(index);

        if (value == null) {
            return null;
        }

        return TypeUtils.castToBoolean(value);
    }

    public byte[] getBytes(Integer index) {
        Object value = super.get(index);

        if (value == null) {
            return null;
        }

        return TypeUtils.castToBytes(value);
    }

    public Byte getByte(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToByte(value);
    }

    public Short getShort(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToShort(value);
    }

    public Integer getInteger(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToInt(value);
    }

    public Long getLong(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToLong(value);
    }

    public Float getFloat(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToFloat(value);
    }

    public Double getDouble(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToDouble(value);
    }

    public BigDecimal getBigDecimal(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToBigDecimal(value);
    }

    public BigInteger getBigInteger(Integer index) {
        Object value = super.get(index);

        return TypeUtils.castToBigInteger(value);
    }

    public String getString(Integer index) {
        Object value = super.get(index);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public Date getDate(Integer index) {
        Object value = super.get(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date)value;
        }
        if (value instanceof String) {
            return DateFormatUtil.parseDate((String)value, DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
        }
        throw new JsonException("cant parse date index:" + index + " value:" + value);
    }

    public Date getDate(Integer index, String pattern) {
        Object value = super.get(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date)value;
        }
        if (value instanceof String) {
            return DateFormatUtil.parseDate((String)value, pattern);
        }
        throw new JsonException("cant parse date index:" + index + " value:" + value);
    }

    @Override
    public JsonArray clone() {
        return new JsonArray(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return JsonUtil.toJsonStr(this);
    }
}
