package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

@Data
public class ServerVersion {

    private Jx jx;
    private Linkx linkx;
    private EAgent eAgent;

    @Data
    public static class Jx {
        private String appVerion;
        private String serviceVersion;
    }

    @Data
    public static class Linkx {

        private String serviceVersion;
    }

    @Data
    public static class EAgent {
        private String serviceVersion;
    }
}
