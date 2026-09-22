package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LabelDetailVO extends Label{

    private Map<Long, LabelCO.Binding> orgCollaborations;

    private Map<Long, LabelCO.BindingUser> orgUsers;

}
