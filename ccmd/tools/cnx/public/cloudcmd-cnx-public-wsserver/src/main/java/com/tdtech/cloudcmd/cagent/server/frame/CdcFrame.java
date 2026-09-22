package com.tdtech.cloudcmd.cagent.server.frame;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author : mWX556161
 * @date : 2020-05-13 15:28
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public final class CdcFrame {
    private CdcFrameHeader header;
    private Long privOrg;
    private String body;

    private int retryCount = 0;

    public CdcFrame(CdcFrameHeader header, String body) {
        this.header = header;
        this.body = body;
    }

    public CdcFrame(CdcFrameHeader header, String body, Long privOrg) {
        this.header = header;
        this.body = body;
        this.privOrg = privOrg;
    }
}