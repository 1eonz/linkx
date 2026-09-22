package com.tdtech.cloudcmd.linkx.third.utils;

import com.tdtech.cloudcmd.linkx.third.api.dto.KeyValue;
import com.tdtech.cloudcmd.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * keyvalue工具类
 */
public class KeyValueUtils {

    /**
     * keyValue转map
     *
     * @param keyValueList keyValue列表
     * @return map
     */
    public static Map<String, String> keyvalueToMap(List<KeyValue> keyValueList) {
        if (CollectionUtils.isEmpty(keyValueList)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        keyValueList.forEach(keyValue -> {
            result.put((String) keyValue.getKey(), (String) keyValue.getValue());
        });
        return result;
    }
}