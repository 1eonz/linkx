package com.tdtech.cloudcmd.im.jingxin.client;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NamedLogger {

    String getName();

    default Logger getLogger(Class<?> cls) {
        return LoggerFactory.getLogger(cls);
    }

    static Object[] concatenateArrays(Object name, Object[] params) {
        if(params.length==0){
            return new Object[]{name};
        }
        var original = new Object[] {name};
        Object[] result = Arrays.copyOf(original, 1 + params.length);
        System.arraycopy(params, 0, result, original.length, params.length);
        return result;
    }

    default void log(String level, String pattern, Object... params) {
        Logger logger = getLogger(this.getClass());
        String formattedPattern = "[{}]: - " + pattern;

        switch (level) {
            case "debug":
                logger.debug(formattedPattern, concatenateArrays(getName(), params));
                break;
            case "info":
                logger.info(formattedPattern, concatenateArrays(getName(), params));
                break;
            case "warn":
                logger.warn(formattedPattern, concatenateArrays(getName(), params));
                break;
            case "error":
            default:
                logger.error(formattedPattern, concatenateArrays(getName(), params));
                break;
        }
    }
}
