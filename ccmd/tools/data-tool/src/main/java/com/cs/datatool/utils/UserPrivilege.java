package com.cs.datatool.utils;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class UserPrivilege implements Serializable {

    private List<Long> privOrgs;
    private Long orgId;
    private Integer hasChildOrgPriv;// 0 true 1 false

}
