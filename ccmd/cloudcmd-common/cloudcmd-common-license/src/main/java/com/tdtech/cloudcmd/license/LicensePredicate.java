package com.tdtech.cloudcmd.license;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class LicensePredicate {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    // private LicenseClient licenseClient;

    /**
     * 判断License状态
     */
    public String predicateLicenseStatus() {
        // StatusEnum statusEnum = licenseClient.currentStatus();
        // if (statusEnum == null) {
        // throw new LicenseException("LICENSE_PREDICATE_LICENSE_UNKNOWN", "");
        // }
        // switch (statusEnum) {
        // case NOT_ACTIVATED: {
        // throw new LicenseException("LICENSE_PREDICATE_LICENSE_NOT_ACTIVATED", "");
        // }
        // case DISABLED: {
        // throw new LicenseException("LICENSE_PREDICATE_LICENSE_DISABLED", "");
        // }
        // case ACTIVATED_EXPIRED: {
        // logSample(() -> log.debug("status:{}", statusEnum));
        // throw new LicenseException("LICENSE_PREDICATE_LICENSE_EXPIRE", "");
        // }
        // case DISABLED_IN_TRIAL: {
        // return "LICENSE_PREDICATE_DISABLED_IN_TRIAL";
        // }
        // case ACTIVATED_TO_BE_EXPIRED: {
        // return "LICENSE_PREDICATE_ACTIVATED_TO_BE_EXPIRED";
        // }
        // default: {
        // logSample(() -> log.debug("status:{}", statusEnum));
        // return "";
        // }
        // }
        return "";
    }

    /**
     * 判断基础能力
     */
    public void predicateBaseAbilities() {
        // predicateNumber(1, LicenseItem.ICS_BASE_ITEM_NAME, "LICENSE_PREDICATE_BASE_ABILITIES");
    }

    /**
     * 判断ICC开户
     */
    public void predicateIccNum(int number) {
        // predicateNumber(number, LicenseItem.ICC_NUM_ITEM_NAME, "LICENSE_PREDICATE_ICC_NUM");
    }

    /**
     * 判断ICC开户
     */
    public int getIccNum() {
        // var licenseItem = find(LicenseItem.ICC_NUM_ITEM_NAME);
        // return licenseItem.map(LicenseItem::getResNum).orElse(0);
        return 999999;
    }

    /**
     * 判断CAPP开户
     */
    public void predicateCappNum(int number) {
        // predicateNumber(number, LicenseItem.CAPP_NUM_ITEM_NAME, "LICENSE_PREDICATE_CAPP_NUM");
    }

    /**
     * 判断CAPP开户
     */
    public int getCappNum() {
        // var licenseItem = find( LicenseItem.CAPP_NUM_ITEM_NAME);
        // return licenseItem.map(LicenseItem::getResNum).orElse(0);
        return 999999;
    }

    /**
     * 判断图片布控
     */
    public void predicateImageDevNum(int number) {
        // predicateNumber(number, LicenseItem.IMAGE_DEV_ITEM_NAME, "LICENSE_PREDICATE_IMAGE_DEV_NUM");
    }

    /**
     * 判断视频布控
     */
    public void predicateVideoDevNum(int number) {
        // predicateNumber(number, LicenseItem.VIDEO_DEV_ITEM_NAME, "LICENSE_PREDICATE_VIDEO_DEV_NUM");
    }

    /**
     * 执法记录仪接入数
     */
    public void predicateBodyCamNum(int number) {
        // predicateNumber(number, LicenseItem.BODY_CAM_NUM, "LICENSE_PREDICATE_BODY_CAM_NUM");
    }

    public int getBodyCamNum() {
        // var licenseItem = find(LicenseItem.BODY_CAM_NUM);
        // return licenseItem.map(LicenseItem::getResNum).orElse(0);
        return 999999;
    }

    /**
     * 布控功能
     */
    public void predicateSuspectTask() {
        // var imageItem = find(LicenseItem.IMAGE_DEV_ITEM_NAME);
        // var videoItem = find(LicenseItem.VIDEO_DEV_ITEM_NAME);
        // if (imageItem.isEmpty() || videoItem.isEmpty()
        // || imageItem.get().getResNum() + videoItem.get().getResNum() == 0) {
        // throw new LicenseException("LICENSE_PREDICATE_SUSPECT_TASK",
        // "IMAGE_DEV_ITEM_NAME + VIDEO_DEV_ITEM_NAME = 0");
        // }
        // predicateNumber(1, LicenseItem.SUSPECT_TASK, "LICENSE_PREDICATE_SUSPECT_TASK");
    }

    /**
     * 布控功能
     */
    public Integer getSuspectTask() {
        // Optional<LicenseItem> find = find(LicenseItem.SUSPECT_TASK);
        // return find.map(LicenseItem::getResNum).orElse(0);
        return 1;
    }

    /**
     * 布控功能
     */
    public Integer getBaseItemName() {
        // Optional<LicenseItem> find = find(LicenseItem.ICS_BASE_ITEM_NAME);
        // return find.map(LicenseItem::getResNum).orElse(0);
        return 1;
    }

    /**
     * 指挥调度功能
     */
    public void predicateControlCenterFunction() {
        // predicateNumber(1, LicenseItem.CONTROL_CENTER_FUNCTION, "LICENSE_PREDICATE_CONTROL_CENTER_FUNCTION");
    }

    /**
     * 指挥调度功能
     */
    public Integer getControlCenterFunction() {
        // Optional<LicenseItem> find = find(LicenseItem.CONTROL_CENTER_FUNCTION);
        // return find.map(LicenseItem::getResNum).orElse(0);
        return 1;
    }

    /**
     * 预警通知功能
     */
    public void predicateAlarmNotify() {
        // predicateNumber(1, LicenseItem.ALARM_NOTIFY, "LICENSE_PREDICATE_ALARM_NOTIFY");
    }

    /**
     * 预警通知功能
     */
    public Integer getAlarmNotify() {
        // Optional<LicenseItem> find = find(LicenseItem.ALARM_NOTIFY);
        // return find.map(LicenseItem::getResNum).orElse(0);
        return 1;
    }

    /**
     * 任务处置功能
     */
    public void predicateMissionDispatch() {
        // predicateNumber(1, LicenseItem.MISSION_DISPATCH, "LICENSE_PREDICATE_MISSION_DISPATCH");
    }

    /**
     * 任务处置功能
     */
    public Integer getMissionDispatch() {
        // Optional<LicenseItem> find = find(LicenseItem.MISSION_DISPATCH);
        // return find.map(LicenseItem::getResNum).orElse(0);
        return 1;
    }

    /**
     * 北向接口功能
     */
    public void predicateNorthBoundFunction() {
        // predicateNumber(1, LicenseItem.NORTHBOUND_FUNCTION, "LICENSE_PREDICATE_NORTHBOUND_FUNCTION");
    }

    /**
     * 执法记录仪在线调度(EBC)
     */
    public Integer getEbc() {
        // Optional<LicenseItem> ebc = find(LicenseItem.BODY_CAM_ONLINE_CONTROL);
        // return ebc.map(LicenseItem::getResNum).orElse(0);
        return 1;
    }

    /**
     * 执法记录仪在线调度(EBC)
     */
    public void predicateEBC() {
        // Optional<LicenseItem> ebc = find(LicenseItem.BODY_CAM_ONLINE_CONTROL);
        // if (ebc.isEmpty() || ebc.get().getResNum() == 0) {
        // throw new LicenseException("LICENSE_PREDICATE_EBC", "ebc check failed");
        // }

    }

    /**
     * ICC-视频会商(VCF)
     */
    public void predicateVCF() {
        // predicateEBC();
        // Optional<LicenseItem> vcf = find(LicenseItem.VIDEO_CONFERENCING_NAME);
        // if (vcf.isEmpty() || vcf.get().getResNum() == 0) {
        // throw new LicenseException("LICENSE_PREDICATE_VIDEO_CONFERENCING", "Video conferencing is not available");
        // }
    }

    /**
     * 动态获取ICP—CONRTROL值
     */
    public String getIcpControl() {
        // Optional<LicenseItem> ebc = find(LicenseItem.BODY_CAM_ONLINE_CONTROL);
        // if (ebc.isEmpty() || ebc.get().getResNum() == 0) {
        // return "0";
        // }
        // Optional<LicenseItem> capp = find(LicenseItem.CAPP_NUM_ITEM_NAME);
        // if (capp.isEmpty() || capp.get().getResNum() == 0) {
        // return "2";
        // }
        return "0";
    }

    /**
     * 动态获取视频会商-Controller
     */
    public String getVCF() {
        // Optional<LicenseItem> find = find(LicenseItem.VIDEO_CONFERENCING_NAME);
        // return String.valueOf(find.map(LicenseItem::getResNum).orElse(0));
        return "1";
    }

    public Integer getAuthenticationFlag() {
        // Optional<LicenseItem> licenseItem = find(LicenseItem.AUTHENTICATION_CAPABILITY_FLAG);
        // return licenseItem.map(LicenseItem::getResNum).orElse(0);
        return 1;
    }
}
