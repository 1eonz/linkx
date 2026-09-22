package com.tdtech.cloudcmd.admin.util.response;

import com.tdtech.cloudcmd.i18n.I18nUtil;

/**
 * @author LWX623661
 * @date 2019-08-24 16:19
 * @description
 */
public enum AdminResultCode {

    /* 成功状态码 */
    SUCCESS(0, "AdminResultCode_SUCCESS"),
    /*失败状态码*/
    FAILURE(1, "AdminResultCode_FAILURE"),

    FILE_IS_NOT_EXIST(90001, "AdminResultCode_FILE_IS_NOT_EXIST"),

    /*账户绑定相互*/

    ACCOUNT_IS_USED(80001, I18nUtil.get("AdminResultCode_ACCOUNT_IS_USED")),
    ACCOUNT_RESOURCE_HAS_BIND_CCMD_ACCOUNT(80002,
        I18nUtil.get("AdminResultCode_ACCOUNT_RESOURCE_HAS_BIND_CCMD_ACCOUNT")),
    ACCOUNT_ROLE_HAS_BIND_CCMD_ACCOUNT(80002, I18nUtil.get("AdminResultCode_ACCOUNT_ROLE_HAS_BIND_CCMD_ACCOUNT")),
    ACCOUNT_MESSAGE_PUBLISH_SUB_FAIL(80003, I18nUtil.get("AdminResultCode_ACCOUNT_MESSAGE_PUBLISH_SUB_FAIL")),
    ACCOUNT_MESSAGE_PUBLISH_UNSUB_FAIL(80004, I18nUtil.get("AdminResultCode_ACCOUNT_MESSAGE_PUBLISH_UNSUB_FAIL")),
    ACCOUNT_CATEGORY_HAS_DATE(80005, I18nUtil.get("AdminResultCode_ACCOUNT_CATEGORY_HAS_DATE")),
    ACCOUNT_RESOURCE_HAS_BIND_ACCOUNT(8006, I18nUtil.get("AdminResultCode_ACCOUNT_RESOURCE_HAS_BIND_ACCOUNT")),
    ACCOUNT_ROLE_HAS_BIND_ACCOUNT(8007, I18nUtil.get("AdminResultCode_ACCOUNT_ROLE_HAS_BIND_ACCOUNT")),
    ACCOUNT_CHECK_SUB_ACCOUNT(8008, I18nUtil.get("AdminResultCode_ACCOUNT_CHECK_SUB_ACCOUNT")),
    ACCOUNT_CHECK_UNSUB_ACCOUNT(8009, I18nUtil.get("AdminResultCode_ACCOUNT_CHECK_UNSUB_ACCOUNT")),
    ACCOUNT_MESSAGE_PUBLISH_SUB_SUCCESS(80010, I18nUtil.get("AdminResultCode_ACCOUNT_MESSAGE_PUBLISH_SUB_SUCCESS")),
    ACCOUNT_MESSAGE_PUBLISH_UNSUB_SUCCESS(80011, I18nUtil.get("AdminResultCode_ACCOUNT_MESSAGE_PUBLISH_UNSUB_SUCCESS")),

    /* 参数错误：10001-19999 */
    PARAM_IS_INVALID(10001, I18nUtil.get("AdminResultCode_PARAM_IS_INVALID")),
    PARAM_IS_BLANK(10002, I18nUtil.get("AdminResultCode_PARAM_IS_BLANK")),
    PARAM_TYPE_BIND_ERROR(10003, I18nUtil.get("AdminResultCode_PARAM_TYPE_BIND_ERROR")),
    PARAM_NOT_COMPLETE(10004, I18nUtil.get("AdminResultCode_PARAM_NOT_COMPLETE")),
    PARAM_IS_DUPLICATION_NAME(5, I18nUtil.get("AdminResultCode_PARAM_IS_DUPLICATION_NAME")),

    /* 用户错误：20001-29999*/
    USER_NOT_LOGGED_IN(20001, I18nUtil.get("AdminResultCode_USER_NOT_LOGGED_IN")),
    USER_LOGIN_ERROR(20002, I18nUtil.get("AdminResultCode_USER_LOGIN_ERROR")),
    USER_ACCOUNT_FORBIDDEN(20003, I18nUtil.get("AdminResultCode_USER_ACCOUNT_FORBIDDEN")),
    USER_NOT_EXIST(20004, I18nUtil.get("AdminResultCode_USER_NOT_EXIST")),
    USER_HAS_EXISTED(20005, I18nUtil.get("AdminResultCode_USER_HAS_EXISTED")),

    /* 业务错误：30001-39999 */
    SPECIFIED_QUESTIONED_USER_NOT_EXIST(30001, I18nUtil.get("AdminResultCode_SPECIFIED_QUESTIONED_USER_NOT_EXIST")),

    /* 系统错误：40001-49999 */
    SYSTEM_INNER_ERROR(40001, I18nUtil.get("AdminResultCode_SYSTEM_INNER_ERROR")),

    /* 数据错误：50001-599999 */
    RESULE_DATA_NONE(50001, I18nUtil.get("AdminResultCode_RESULE_DATA_NONE")),
    DATA_IS_WRONG(50002, I18nUtil.get("AdminResultCode_DATA_IS_WRONG")),
    DATA_ALREADY_EXISTED(50003, I18nUtil.get("AdminResultCode_DATA_ALREADY_EXISTED")),
    DATA_UPDAE_FAILURE(5004, I18nUtil.get("AdminResultCode_DATA_UPDAE_FAILURE")),
    /* 接口错误：60001-69999 */
    INTERFACE_INNER_INVOKE_ERROR(60001, I18nUtil.get("AdminResultCode_INTERFACE_INNER_INVOKE_ERROR")),
    INTERFACE_OUTTER_INVOKE_ERROR(60002, I18nUtil.get("AdminResultCode_INTERFACE_OUTTER_INVOKE_ERROR")),
    INTERFACE_FORBID_VISIT(60003, I18nUtil.get("AdminResultCode_INTERFACE_FORBID_VISIT")),
    INTERFACE_ADDRESS_INVALID(60004, I18nUtil.get("AdminResultCode_INTERFACE_ADDRESS_INVALID")),
    INTERFACE_REQUEST_TIMEOUT(60005, I18nUtil.get("AdminResultCode_INTERFACE_REQUEST_TIMEOUT")),
    INTERFACE_EXCEED_LOAD(60006, I18nUtil.get("AdminResultCode_INTERFACE_EXCEED_LOAD")),
    /* 权限错误：70001-79999 */
    PERMISSION_NO_ACCESS(70001, I18nUtil.get("AdminResultCode_PERMISSION_NO_ACCESS"));

    private final Integer code;

    private final String message;

    AdminResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String getMessage(String name) {
        for (AdminResultCode item : AdminResultCode.values()) {
            if (item.name().equals(name)) {
                return item.message;
            }
        }
        return name;
    }

    public static Integer getCode(String name) {
        for (AdminResultCode item : AdminResultCode.values()) {
            if (item.name().equals(name)) {
                return item.code;
            }
        }
        return null;
    }

    public Integer code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }
}
