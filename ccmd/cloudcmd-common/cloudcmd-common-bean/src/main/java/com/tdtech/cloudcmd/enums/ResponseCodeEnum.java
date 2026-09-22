package com.tdtech.cloudcmd.enums;

/**
 * 
 * 
 * @author zhangweibo
 *
 */
public enum ResponseCodeEnum {
    SUCCESS(0, "ResponseCodeEnum_SUCCESS"),
    // Exception
    FAILURE(1, "ResponseCodeEnum_FAILURE"), // 无法确定错误级别

    // 系统通用错误.主要是参数验证等。
    // SystemException
    COMMON_ERROR_100(100, "ResponseCodeEnum_COMMON_ERROR_100"),
    // 以下为通用异常
    COMMON_ERROR_101(101, "ResponseCodeEnum_COMMON_ERROR_101"),
    COMMON_ERROR_102(102, "ResponseCodeEnum_COMMON_ERROR_102"),
    COMMON_ERROR_103(103, "ResponseCodeEnum_COMMON_ERROR_103"),

    COMMON_ERROR_104(104, "ResponseCodeEnum_COMMON_ERROR_104"),
    COMMON_ERROR_105(105, "ResponseCodeEnum_COMMON_ERROR_105"),
    COMMON_ERROR_106(106, "ResponseCodeEnum_COMMON_ERROR_106"),

    COMMON_ERROR_107(107, "ResponseCodeEnum_COMMON_ERROR_107"),
    COMMON_ERROR_108(108, "ResponseCodeEnum_COMMON_ERROR_108"),
    COMMON_ERROR_109(109, "ResponseCodeEnum_COMMON_ERROR_109"),

    // 以下为登录异常
    COMMON_ERROR_110(110, "ResponseCodeEnum_COMMON_ERROR_110"),
    COMMON_ERROR_111(111, "ResponseCodeEnum_COMMON_ERROR_111"),
    COMMON_ERROR_112(112, "ResponseCodeEnum_COMMON_ERROR_112"),
    // 以下为修改密码异常
    COMMON_ERROR_113(113, "ResponseCodeEnum_COMMON_ERROR_113"),
    COMMON_ERROR_114(114, "ResponseCodeEnum_COMMON_ERROR_114"),
    COMMON_ERROR_115(115, "ResponseCodeEnum_COMMON_ERROR_115"),
    // 以下为调度台坐席ISDN异常
    COMMON_ERROR_116(116, "ResponseCodeEnum_COMMON_ERROR_116"),
    COMMON_ERROR_117(117, "ResponseCodeEnum_COMMON_ERROR_117"),
    // 以下为扩展登录异常
    COMMON_ERROR_118(118, "ResponseCodeEnum_COMMON_ERROR_118"),
    COMMON_ERROR_119(119, "ResponseCodeEnum_COMMON_ERROR_119"),
    COMMON_ERROR_120(120, "ResponseCodeEnum_COMMON_ERROR_120"),
    COMMON_ERROR_121(121, "ResponseCodeEnum_COMMON_ERROR_121"),
    // 以下为登出异常
    COMMON_ERROR_122(122, "ResponseCodeEnum_COMMON_ERROR_122"),
    COMMON_ERROR_123(123, "ResponseCodeEnum_COMMON_ERROR_123"),
    COMMON_ERROR_124(124, "ResponseCodeEnum_COMMON_ERROR_124"),
    COMMON_ERROR_125(125, "ResponseCodeEnum_COMMON_ERROR_125"),
    COMMON_ERROR_126(126, "ResponseCodeEnum_COMMON_ERROR_126"),
    COMMON_ERROR_127(127, "ResponseCodeEnum_COMMON_ERROR_127"),
    COMMON_ERROR_128(128, "ResponseCodeEnum_COMMON_ERROR_128"),
    COMMON_ERROR_129(129, "ResponseCodeEnum_COMMON_ERROR_129"),
    COMMON_ERROR_401(401, "ResponseCodeEnum_COMMON_ERROR_401"),
    COMMON_ERROR_130(130, "ResponseCodeEnum_COMMON_ERROR_130"),
    COMMON_ERROR_131(131, "ResponseCodeEnum_COMMON_ERROR_131"),
    COMMON_ERROR_132(132, "ResponseCodeEnum_COMMON_ERROR_132"),
    COMMON_ERROR_133(133, "ResponseCodeEnum_COMMON_ERROR_133"),
    COMMON_ERROR_134(134, "ResponseCodeEnum_COMMON_ERROR_134"),
    COMMON_ERROR_135(135, "ResponseCodeEnum_COMMON_ERROR_135"),
    COMMON_ERROR_136(136, "ResponseCodeEnum_COMMON_ERROR_136"),
    COMMON_ERROR_137(137, "ResponseCodeEnum_COMMON_ERROR_137"),
    COMMON_ERROR_140(140, "ResponseCodeEnum_COMMON_ERROR_140"),
    COMMON_ERROR_141(141, "ResponseCodeEnum_COMMON_ERROR_141"),
    COMMON_ERROR_142(142, "ResponseCodeEnum_COMMON_ERROR_142"),
    COMMON_ERROR_150(150, "ResponseCodeEnum_COMMON_ERROR_150"),
    COMMON_ERROR_151(151, "ResponseCodeEnum_COMMON_ERROR_151"),
    COMMON_ERROR_152(152, "ResponseCodeEnum_COMMON_ERROR_152"),
    COMMON_ERROR_153(153, "ResponseCodeEnum_COMMON_ERROR_153"),
    COMMON_ERROR_154(154, "ResponseCodeEnum_COMMON_ERROR_154"),
    COMMON_ERROR_155(155, "ResponseCodeEnum_COMMON_ERROR_155"),
    COMMON_ERROR_156(156, "ResponseCodeEnum_COMMON_ERROR_156"),
    COMMON_ERROR_157(157, "ResponseCodeEnum_COMMON_ERROR_157"),
    COMMON_ERROR_158(158, "ResponseCodeEnum_COMMON_ERROR_158"),
    COMMON_ERROR_159(159, "ResponseCodeEnum_COMMON_ERROR_159"),
    COMMON_ERROR_160(160, "ResponseCodeEnum_COMMON_ERROR_160"),
    //适配国际化
    COMMON_ERROR_161(161, "ResponseCodeEnum_COMMON_ERROR_161"),
    COMMON_ERROR_162(162, "ResponseCodeEnum_COMMON_ERROR_162"),
    COMMON_ERROR_163(163, "ResponseCodeEnum_COMMON_ERROR_163"),
    COMMON_ERROR_164(164, "ResponseCodeEnum_COMMON_ERROR_164"),
    COMMON_ERROR_165(165, "ResponseCodeEnum_COMMON_ERROR_165"),
    COMMON_ERROR_166(166, "ResponseCodeEnum_COMMON_ERROR_166"),
    COMMON_ERROR_167(167, "ResponseCodeEnum_COMMON_ERROR_167"),
    COMMON_ERROR_168(168, "ResponseCodeEnum_COMMON_ERROR_168"),
    COMMON_ERROR_169(169, "ResponseCodeEnum_COMMON_ERROR_169"),
    COMMON_ERROR_170(170, "ResponseCodeEnum_COMMON_ERROR_170"),
    COMMON_ERROR_171(171, "ResponseCodeEnum_COMMON_ERROR_171"),
    COMMON_ERROR_172(172, "ResponseCodeEnum_COMMON_ERROR_172"),
    COMMON_ERROR_173(173, "ResponseCodeEnum_COMMON_ERROR_173"),
    COMMON_ERROR_174(174, "ResponseCodeEnum_COMMON_ERROR_174"),
    COMMON_ERROR_175(175, "ResponseCodeEnum_COMMON_ERROR_175"),
    COMMON_ERROR_176(176, "ResponseCodeEnum_COMMON_ERROR_176"),
    COMMON_ERROR_177(177, "ResponseCodeEnum_COMMON_ERROR_177"),
    COMMON_ERROR_178(178, "ResponseCodeEnum_COMMON_ERROR_178"),
    CLICK_CLEARING_FAILED(179, "CLICK_CLEARING_FAILED"),
    COMMON_ERROR_180(180, "ResponseCodeEnum_COMMON_ERROR_180"),
    COMMON_ERROR_181(181, "ResponseCodeEnum_COMMON_ERROR_181"),
    COMMON_ERROR_182(182, "ResponseCodeEnum_COMMON_ERROR_182"), // 全局参数value不可以为空
    // 业务错误,定义在各个子项目里的ResponseBizCodeEnum。
    // AbstractBaseException
    // BIZ_ERROR_201(201,"业务错误"),

    ;// 定义结束符

    private int code;
    private String msg;

    private ResponseCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
