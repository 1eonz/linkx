package com.tdtech.cloudcmd.icp.proxy.ws.protocal;

public enum CmdTypeEnum {
    REGISTER_EVENTS("registerEvents"), RESOURCE_NOTIFY("resourceNotify"), SMS_NOTIFY("msNotify"),
    GIS_NOTIFY("gisNotify"), COMMON_NOTIFY("commonNotify");

    private final String cmd;

    CmdTypeEnum(String cmd) {
        this.cmd = cmd;
    }

    public static CmdTypeEnum cmdOf(String cmd) {
        for (CmdTypeEnum ctenum : CmdTypeEnum.values()) {
            if (ctenum.cmd.equals(cmd)) {
                return ctenum;
            }
        }
        return null;
    }

    public String cmd() {
        return this.cmd;
    }
}
