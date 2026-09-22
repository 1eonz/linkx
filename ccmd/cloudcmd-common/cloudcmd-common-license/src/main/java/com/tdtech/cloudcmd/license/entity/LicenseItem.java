package com.tdtech.cloudcmd.license.entity;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LicenseItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ICC_NUM_ITEM_NAME = "eICCNum";
    public static final String CAPP_NUM_ITEM_NAME = "CAPPNum";
    public static final String IMAGE_DEV_ITEM_NAME = "ImageDevNum";
    public static final String VIDEO_DEV_ITEM_NAME = "VideoDevNum";
    public static final String ICS_BASE_ITEM_NAME = "eICSBS";
    public static final String VIDEO_CONFERENCING_NAME = "eICSVCF";
    public static final String BODY_CAM_NUM = "BodyCamNum";
    public static final String SUSPECT_TASK = "eICSCF";
    public static final String CONTROL_CENTER_FUNCTION = "eICSCDF";
    public static final String ALARM_NOTIFY = "eICSANF";
    public static final String MISSION_DISPATCH = "eICSTDF";
    public static final String NORTHBOUND_FUNCTION = "eICSNI";
    public static final String BODY_CAM_ONLINE_CONTROL = "eICSFeBC";
    public static final String AUTHENTICATION_CAPABILITY_FLAG = "eICSLEMF";
    public static final String ICS_SSF_NUM_NAME = "eICSSSF";
    public static final String ICS_VDF_NUM_NAME = "eICSVDF";


    private String licenseControlItem;

    private Integer resNum;
    @JsonDeserialize(using = ItemExpireTimeDeserializer.class)
    private Date expireTime;
    private String licenseType;

    public boolean isExpired(long currentTime) {
        return (expireTime.getTime()) < (currentTime - 24L * 60L * 60L * 1000L);
    }
}
