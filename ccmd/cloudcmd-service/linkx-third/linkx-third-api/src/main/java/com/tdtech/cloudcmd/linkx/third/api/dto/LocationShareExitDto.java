package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LocationShareExitDto implements Serializable {

    private Long shareId;

    private Long userId;

    private Integer exitType;

    private String exitDesc;
}
