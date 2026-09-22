package com.tdtech.cloudcmd.web.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapperHolder {

    private static ObjectMapper objectMapper;

    private JsonMapperHolder() {
        throw new UnsupportedOperationException();
    }

    public static ObjectMapper getMapper() {
        return objectMapper;
    }

    public static class ObjectMapperHolderConfigurer {
        public ObjectMapperHolderConfigurer(ObjectMapper objectMapper) {
            JsonMapperHolder.objectMapper = objectMapper.copy();
        }
    }
}
