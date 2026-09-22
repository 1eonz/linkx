package com.tdtech.cloudcmd.auth.dto;

import javax.validation.constraints.NotNull;

import com.tdtech.cloudcmd.auth.entity.Application;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ValidatorContext {

    // INIT params
    @NotNull
    private ImUserDO user;
    @NotNull
    private Application application;

    private String warningMsg;
}
