package com.tdtech.cloudcmd.cnd.privatezone.queue.auth;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfo(Long userId,
    //
    @JsonProperty("clientId") String appKey,
    //
    String deviceId,
    //
    String userName,
    //
    String executorCode,
    //
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTime,
    //
    String loginIp,
    //
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date userPeriod,
    //
    Long executorId,
    //
    boolean isAdmin,
    //
    Long organizationId, Integer hasChildOrgPriv) {
}
