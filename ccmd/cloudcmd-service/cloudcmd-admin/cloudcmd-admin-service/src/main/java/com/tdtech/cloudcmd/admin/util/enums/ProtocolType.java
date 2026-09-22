package com.tdtech.cloudcmd.admin.util.enums;

import java.util.Arrays;

/**
 * 协议类型枚举
 */
public enum ProtocolType {

    SSH("SSH", 1),

    RDP("RDP", 2),

    Telnet("Telnet", 3),

    VNC("VNC", 4),

    HTTP("HTTP", 5),

    Other("其他", 6);

    private String name;
    private Integer code;

    ProtocolType(String name, Integer code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public Integer getCode() {
        return code;
    }

    public static ProtocolType codeOf(Integer code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static ProtocolType nameOf(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name.equals(name))
                .findFirst()
                .orElse(null);
    }
}
