package com.tdtech.cloudcmd.linkx.third.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateFormatFieldInfo {

    private String fieldName;
    private String dateFormat;
    private DateDimension dimension;

    public enum DateDimension {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND
    }
}