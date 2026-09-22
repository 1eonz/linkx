package com.tdtech.cloudcmd.cagent.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo extends com.tdtech.cloudcmd.bean.UserInfo implements Comparable<UserInfo> {

    private String account;

    public static String compareTag(String clientId, Long userId) {
        return clientId + "--" + userId;
    }

    @Override
    public int compareTo(UserInfo o) {
        return compareTag().compareTo(o.compareTag());
    }

    public String compareTag() {
        return compareTag(super.getAppKey(), super.getUserId());
    }
}
