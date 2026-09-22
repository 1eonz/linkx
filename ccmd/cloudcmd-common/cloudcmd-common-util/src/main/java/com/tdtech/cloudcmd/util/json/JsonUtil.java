package com.tdtech.cloudcmd.util.json;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.tdtech.cloudcmd.util.DateFormatUtil;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        objectMapper.registerModule(javaTimeModule);

        objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
        objectMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setTimeZone(TimeZone.getDefault());
    }

    public static ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * 将 POJO 对象转为 JSON 字符串
     */
    @SneakyThrows
    public static <T> String toJsonStr(T pojo) {
        if (pojo == null) {
            return "";
        }
        return objectMapper.writeValueAsString(pojo);
    }

    public static <T> T convert(Object source, Class<T> targetType) {
        return objectMapper.convertValue(source, targetType);
    }

    public static <T> T convert(Object source, TypeReference<T> targetType) {
        return objectMapper.convertValue(source, targetType);
    }

    @SneakyThrows
    public static JsonObject parseJson(byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return null;
        }
        return objectMapper.readValue(jsonBytes, JsonObject.class);
    }

    @SneakyThrows
    public static JsonNode parseTree(byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return null;
        }
        return objectMapper.readTree(jsonBytes);
    }

    /**
     * 将 JSON 字符串转为 POJO 对象
     */
    @SneakyThrows
    public static <T> T parseJson(byte[] jsonBytes, Class<T> type) {
        Objects.requireNonNull(type);
        if (jsonBytes == null || jsonBytes.length == 0) {
            return null;
        }
        return objectMapper.readValue(jsonBytes, type);
    }

    @SneakyThrows
    public static JsonObject parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readValue(json, JsonObject.class);
    }

    @SneakyThrows
    public static JsonNode parseTree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readTree(json);
    }

    /**
     * 将 JSON 字符串转为 POJO 对象
     */
    @SneakyThrows
    public static <T> T parseJson(String jsonStr, Class<T> type) {
        Objects.requireNonNull(type);
        if (jsonStr == null || jsonStr.isBlank()) {
            return null;
        }
        return objectMapper.readValue(jsonStr, type);
    }

    /**
     * 将 JSON 字符串转为 POJO 对象
     */
    @SneakyThrows
    public static <T> T parseJson(String jsonStr, TypeReference<T> type) {
        Objects.requireNonNull(type);
        if (jsonStr == null || jsonStr.isBlank()) {
            return null;
        }
        return objectMapper.readValue(jsonStr, type);
    }

    /**
     * 将 JSON 字符串转为 POJO 对象
     */
    @SneakyThrows
    public static <T> T parseJson(byte[] bytes, TypeReference<T> type) {
        Objects.requireNonNull(type);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return objectMapper.readValue(bytes, type);
    }

    /**
     * json数组转List
     * <p>
     * List<UserBean> jsonToUserBeans = JacksonUtil.readValue(listToJson, new TypeReference<List<UserBean>>() {
     *
     */
    @SneakyThrows
    public static JsonArray parseArrayJson(byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return null;
        }
        return objectMapper.readValue(jsonBytes, JsonArray.class);
    }

    @SneakyThrows
    public static JsonArray parseArrayJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return objectMapper.readValue(json, JsonArray.class);
    }

    @SneakyThrows
    public static <T> List<T> parseArrayJson(String jsonStr, Class<T> elementType) {
        Objects.requireNonNull(elementType);
        if (jsonStr == null || jsonStr.isBlank()) {
            return null;
        }
        JavaType javaType = getCollectionType(elementType);
        return objectMapper.readValue(jsonStr, javaType); // 这里不需要强制转换
    }

    @SneakyThrows
    public static <T> List<T> parseArrayJson(InputStream inputStream, Class<T> elementType) {
        Objects.requireNonNull(elementType);
        if (inputStream == null) {
            return null;
        }
        JavaType javaType = getCollectionType(elementType);
        return objectMapper.readValue(inputStream, javaType); // 这里不需要强制转换
    }

    private static JavaType getCollectionType(Class<?>... elementClasses) {
        return objectMapper.getTypeFactory().constructParametricType(LinkedList.class, elementClasses);
    }

}
