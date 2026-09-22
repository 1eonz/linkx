package com.tdtech.cloudcmd.icp.proxy.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;

@Getter
@Setter
@Accessors(chain = true)
public class GisSubscriptionReq {

    @JsonProperty("expiredtime")
    private String expiredTime;

    @JsonProperty("uelist")
    private Collection<Isdn> ueList;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Isdn {
        private String isdn;
    }
}
