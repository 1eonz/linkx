package com.tdtech.cloudcmd.icp.proxy.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tdtech.cloudcmd.icp.proxy.ws.protocal.ResourceTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;

@Getter
@Setter
@Accessors(chain = true)
public class ResourceSubscriptionReq {

    private ResourceTypeEnum type;

    @JsonProperty("reslist")
    private Collection<Isdn> resList;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Isdn {
        private String isdn;
    }
}
