package com.tdtech.cloudcmd.cagent.service.entity;

import javax.validation.constraints.NotNull;

import com.tdtech.cloudcmd.cagent.service.UserInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseChannel {

    @NotNull
    private UserInfo userInfo;

    @NotNull
    private Long id;

}
