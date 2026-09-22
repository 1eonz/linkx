package com.tdtech.cloudcmd.admin.util;

import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tdtech.cloudcmd.admin.resource.entity.Range;

public class GlobalsUtils {

    private static final Map<String, Range> RANGE_MAP = new HashMap<>(RANGE_TYPES.size());
    private static final Map<String, List<Integer>> FIXVALUE_MAP = new HashMap<>(FIXVALUE_TYPES.size());

    static {
        //CAGENT_PORT, SDK_PORT, DEFAULT_ZOOM, MIN_ZOOM, MAX_ZOOM, AUTH_AUTO_UNLOCK_TIME,
        //AUTH_LOGIN_ERROR_NUM_LIMIT, AUTH_PWD_VALIDITY_PERIOD, AUTH_TOKEN_EXPIRE_IN, AUTH_REFRESHTOKEN_EXPIRE_IN,
        //CAPP_LOCATION_RESOURCE_CAN_SEE_DISTANCE, CAPP_TASK_CAN_SEE_CAMERA_DISTANCE, CAPP_TASK_CAN_OPERATOR_CAMERA_DISTANCE,
        //REFRESH_TIME, MAP_REFRESH_PERIOD
        RANGE_MAP.put(CAGENT_PORT, new Range(1, 65535));
        RANGE_MAP.put(SDK_PORT, new Range(1, 65535));
        RANGE_MAP.put(DEFAULT_ZOOM, new Range(0, 19));
        RANGE_MAP.put(MIN_ZOOM, new Range(0, 19));
        RANGE_MAP.put(MAX_ZOOM, new Range(0, 19));
        RANGE_MAP.put(AUTH_AUTO_UNLOCK_TIME, new Range(1, 60));
        RANGE_MAP.put(AUTH_LOGIN_ERROR_NUM_LIMIT, new Range(3, 10));
        RANGE_MAP.put(AUTH_PWD_VALIDITY_PERIOD, new Range(30, 180));
        RANGE_MAP.put(AUTH_TOKEN_EXPIRE_IN, new Range(1, 24));
        RANGE_MAP.put(AUTH_REFRESHTOKEN_EXPIRE_IN, new Range(30, 180));
        RANGE_MAP.put(CAPP_LOCATION_RESOURCE_CAN_SEE_DISTANCE, new Range(1000, 10000));
        RANGE_MAP.put(CAPP_TASK_CAN_SEE_CAMERA_DISTANCE, new Range(1000, 10000));
        RANGE_MAP.put(CAPP_TASK_CAN_OPERATOR_CAMERA_DISTANCE, new Range(1000, 10000));
        RANGE_MAP.put(REFRESH_TIME, new Range(3, 30));
        RANGE_MAP.put(MAP_REFRESH_PERIOD, new Range(10, 60));
        RANGE_MAP.put(STATION_NAME,new Range(1,28));
        RANGE_MAP.put(STATION_NAME_CHINESE,new Range(1,14));
        RANGE_MAP.put(MSIP_DISK_SAFE_SIZE,new Range(20,100));
        RANGE_MAP.put(DUTY_SCHEDULE_NOTICE_PERIOD,new Range(-1,4320));
        RANGE_MAP.put(DUTY_SCHEDULE_NOTICE_COUNT,new Range(-1,10));

        //AUTO_MISSION_STATUS, VIDEO_OFFER, EDGEGATEWAY_BREAKER, CAR_BREAKER,
        //CUSTOMIZED_LAYER, APPLICATION_BREAKER, CAR_TYPE, SUSPECT_IN, CAPABILITY_SWITCH, SUSPECTTASK_OFFLINE_SWITCH
        FIXVALUE_MAP.put(AUTO_MISSION_STATUS, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(VIDEO_OFFER, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(EDGEGATEWAY_BREAKER, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(CAR_BREAKER, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(CUSTOMIZED_LAYER, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(APPLICATION_BREAKER, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(CAR_TYPE, List.of(2));
        FIXVALUE_MAP.put(SUSPECT_IN, Arrays.asList(0, 1, 2));
        FIXVALUE_MAP.put(CAPABILITY_SWITCH, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(SUSPECTTASK_OFFLINE_SWITCH, Arrays.asList(0, 1));
        FIXVALUE_MAP.put(AI_SEPARATED_DEPLOY, Arrays.asList(0, 1));
    }

    public static boolean rangeCheck(String globalsName, String globalsValue) {
        int value = Integer.valueOf(globalsValue);
        if (RANGE_MAP.containsKey(globalsName) && typeCheck(globalsValue)) {
            Range range = RANGE_MAP.get(globalsName);
            return value >= range.getLow() && value <= range.getHigh();
        }
        return false;
    }
    public static boolean rangeCheckDouble(String globalsValue) {
        double value = Double.parseDouble(globalsValue);
        return value > 0 && value <= 5.0;
    }

    public static boolean fixValueCheck(String globalsName, String globalsValue) {
        int value = Integer.valueOf(globalsValue);
        return FIXVALUE_MAP.containsKey(globalsName) && FIXVALUE_MAP.get(globalsName).contains(value);
    }

    public static boolean fixBooleanValueCheck(String globalsValue) {
        //String value = globalsValue.toUpperCase(Locale.ENGLISH);
        return TRUE.equals(globalsValue) || FALSE.equals(globalsValue);
    }

    public static boolean typeCheck(String globalsValue){
        int value = Integer.valueOf(globalsValue);
        return globalsValue.equals(String.valueOf(value));
    }
}
