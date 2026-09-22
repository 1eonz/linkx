package com.tdtech.cloudcmd.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
  public enum CarExceptionEnum {
    PLATE_NUMBER_DUPLICATE(2, "CAREXCEPTIONENUM_PLATE_NUMBER_DUPLICATE_ERROR"),
    CODE_DUPLICATE(3, "CAREXCEPTIONENUM_CODE_DUPLICATE_ERROR"),
    UNKOWN_EXCEPTION(4, "UNKOWN_EXCEPTION_ERROR"),
    MULTIPLE_COLLECTION(5, "MULTIPLE_COLLECTION_ERROR");
    private final int code;
    private final String msg;

    public static CarExceptionEnum valuesOf(int code) {
        CarExceptionEnum[] values = CarExceptionEnum.values();
        for (CarExceptionEnum value : values) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return null;
    }
}