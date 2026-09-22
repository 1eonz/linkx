package com.tdtech.cloudcmd.icp.proxy.ws.protocal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum PWICodeEnum {

    /**
     * 4011： 用户登录成功 4012： 用户登出成功 4013： 用户未授权 4014： 用户不存在 4015： 用户暂时不可用 4016： 资源冲突 4017： 未鉴权 4018： License受限 4019： 密码错误
     * 4100： 用户已登录 101： 用户名或密码错误 103： 认证错误次数超限，账户临时被锁定 104： 首次登录强制修改密码 201： 用户名不合法 203： 密码不合法 206： 设备被人工锁定 .....
     */
    LOGIN_SUCCESS("4011", "用户登录成功"), LOGOUT_SUCCESS("4012", "用户登出成功"), USER_NOTAUTH("4013", "用户未授权"),
    USER_NOTEXIST("4014", "用户不存在"), USER_UNAVAILABLE("4015", "用户暂时不可用"), RESOURCE_CONFLICT("4016", "资源冲突"),
    NO_AUTHENTICATION("4017", "未鉴权"), LICENSE_RESTRICTION("4018", "License受限"), PWD_ERROR("4019", "密码错误"),
    USER_LOGINED("4100", "用户已登录"), USER_ERROR("101", "用户名或密码错误"), USER_LOCKED("103", "认证错误次数超限，账户临时被锁定"),
    FORCE_PWD_CHANGE("104", "首次登录强制修改密码"), USER_WRONGFUL("201", "用户名不合法"), PWD_WRONGFUL("203", "密码不合法"),
    EQUIPMENT_LOCKED("206", "设备被人工锁定"),

    NOT_REGISTERED("3998", "未登录"), REGISTERING_CONFLICT("3997", "登录冲突"), REGISTERED_ERROR("80016", "登录失败"),

    SUCCESS("0", "操作成功"), FAILURE("1", "操作失败"), AUTH_FAIL("10000", "鉴权失败"), PARAM_EXCEPTION("10001", "参数错误"),
    SDKSERVER_EXCEPTION("10002", "SDK服务异常"), REQ_TIMEOUT("10003", "请求超时"), SDKSERVER_BUSY("10004", "SDK服务忙"),
    OPT_WRONG("-40001", "操作不被接受，操作状态不正确，缺少必要的前提"), NO_RESOURSE("-40002", "操作不被接受，因为没有该资源"),
    NO_OPT_ABLILITY("-40003", "操作不被接受，因为对应资源不支持该操作能力"), EXCEED_APP_ABILITY("-40004", "操作不被接受，因为超出APP处理能力"),
    NO_AUTH("-40005", "操作不被接受，因为用户在对应资源上没有权限做该操作"), CONCURRENCY_LIMIT("-40006", "操作不被接受, 业务并发受限"),
    INVALID_IP("-40007", "操作不被接受, IP地址不合法"), INVALID_IPADDRESS("-40008", "操作不被接受, IP地址不合法"),
    OPT_SUCCESS("-40009", "操作执行成功"),

    RELEASE_ERROR("-40010", "执行组呼释放错误"), NO_SUBSCRIBE("-40011", "操作不被接受,组没有被订阅"),
    MEDIA_FAIL("-40012", "媒体面失败，检查MIC是否插入"), NO_AVAILABLEPORT("-40013", "端口分配失败，无可用端口供分配"),

    CALL_TRANSFERRING("-40093", "该路呼叫正在被转接"), NO_OWNERSHIP("-40094", "找不到归属地（用户在ZONE号段外）"),
    CHAIN_BREAK("-40095", "找到了用户的归属地 但与归属地MDC之间断链"), RSP_TIMEOUT("-40096", "服务器端响应超时"),
    RSP_ERRO("-40097", "服务器端返回错误, 操作被调度机拒绝"), UNKNOWN_REASON("-40098", "操作不被接受, 原因不明"),
    NOT_IMPLEMENTED("-40099", "操作未实现， 可能在未来版本实现"),

    OSIP_INNER_ERROR("-40100", "OSIP信令处理内部错误"), MS2_INNER_ERROR("-40101", "MS2媒体处理内部错误"),
    ZMQ_INNER_ERROR("-40102", "ZMQ命令处理内部错误"), TIMER_INNER_ERROR("-40103", "定时器处理内部错误"), PARAM_ERROR("-40104", "参数错误"),
    EXCEED_LICENSELIMIT("-40105", "LICENSE超出限制"), ALREADY_EXISTS("-40106", "已经存在，重复分发"), NO_USER("-40107", "该用户不存在"),
    NO_REGISTER("-40108", "该用户没有注册"), TIMER_FAIL("-40109", "定时器调度失败"),

    TRANSFER_FAIL("-40110", "调度机转接失败"), EXCEED_MAXLIMIT("-40111", "用户数量超出最大限制"),
    GROUPID_ERROR("-40112", "动态组创建时使用了错误的动态组号码"), MEMBER_OUTOF_RANGE("-40113", "动态组的成员超出范围"),
    CREATE_GROUP_TIMEOUT("-40114", "动态组创建超时"), MEMBER_EXCEED_LIMIT("-40115", "动态组中组成员超出范围限制"),
    MEMBER_NO_REGISTER("-40116", "动态组中有成员没有注册"), VERSION_MISMATCH("-40117", "版本不匹配"),
    GROUP_EXISTED("-40118", "动态组已经存在"), USER_NOTIN_GROUP("-40119", "没有任何有效的用户在动态组内，请检测是否包含外部组或者外部用户"),

    INTERNET_ERROR("-40120", "在创建动态组的时候网络返回错误"), TARGET_ONLINE("-40121", "目标正在通话中，拒绝调度台抢话"),
    EXCEED_SYSTEMLIMIT("-40122", "该视频源转发的数量已经超过系统限制"), VERSION_NOSUPPORT("-40124", "版本不支持"),
    NO_DOWNLOAD("-40125", "不允许下载"), NUMBER_ERROR("-40126", "号码错误"),

    NUMBER_EXCEED_LIMIT("-40131", "调度员超过最大创建派接数"), NO_SUPPORT("-40132", "紧急呼叫不支持强拆"),
    NO_SUPPORT_OPT("-40133", "不支持强拆操作"), INVALID_LOCALIP("-40134", "本地IP地址无效"),
    GROUP_CREATING("-40137", "后台服务无响应或动态组还在建立中，请稍后查看"), UDC_ERROR("-40138", "动态组操作UDC错误"),
    INVALID_NUMBER("-40139", "动态组操作无效的索引号码"),

    UDC_FAIL("-40140", "动态组操作UDC同步失败"), UDC_TIMEOUT("-40141", "动态组操作UDC超时"),
    UDC_INNER_ERROR("-40142", "动态组OR修改密码操作UDC内部错误"), UDC_INNER_ERROR2("-40143", "动态组OR修改密码操作UDC内部错误"),
    NUMBER_GETERROR("-40144", "派接组号码获取失败"), NUMBER_GETERROR2("-40145", "临时组号码获取失败"),
    MODIFY_PWD_ERRROR("-40146", "修改密码失败：旧密码验证失败"), MODIFY_PWD_ERRROR2("-40147", "修改密码失败：新密码不合法"),
    MODIFY_PWD_ERRROR3("-40148", "修改密码失败：用户号无效"), MODIFY_PWD_ERRROR4("-40149", "修改密码失败：未知错误"),

    VPN_NO_SUPPORT("-40151", "视频分发或转接失败：vpn不支持"), ONLY_GATEWAY_USER("-40153", "临时用户当前只支持网关用户"),
    TRANSCODING_BUSY("-40154", "转码繁忙，请尝试不转码发送"), NO_PERMISSION("-40155", "调度机取消转接未允许"),
    TRANSFER_SUCCESS("-40156", "调度机已完成取消转接"), TRANSFER_FAILURE("-40157", "调度机取消转接失败"), NO_POWER("-40158", "无权限"),
    USER_NOT_SUPPORTED("-40159", "用户不支持"),

    MEMBER_OVERRUN("-40162", "派接组用户成员超限"), CONFIG_TIMEOUT("-40163", "GIS权限配置超时"), SESSION_EXISTED("-40164", "会话存在"),
    KEEP_CALLING("-40166", "操作不被接受，因为正在请求保持呼叫中"),

    MEETING_NOT_EXIST("-40200", "会议不存在"), ALREADY_LOGIN("-40201", "已经登录"), NOT_JOIN_MEETING("-40202", "未加入会议"),
    NO_CHAIRMAN("-40203", "不是会议主席"), CREATING_MEETING("-40204", "正在创建会议"),
    NO_RETCODE("-40205", "收到Message响应，没有RetCode");

    private final String code;
    private final String msg;

    PWICodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @JsonCreator
    public static PWICodeEnum codeOf(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (PWICodeEnum pwiCodeEnum : PWICodeEnum.values()) {
            if (pwiCodeEnum.getCode().equals(code)) {
                return pwiCodeEnum;
            }
        }
        return null;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
