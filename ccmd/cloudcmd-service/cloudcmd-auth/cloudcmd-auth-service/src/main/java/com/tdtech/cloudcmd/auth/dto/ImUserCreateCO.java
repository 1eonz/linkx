package com.tdtech.cloudcmd.auth.dto;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.tdtech.cloudcmd.auth.entity.ImUserDO;

import lombok.Data;

@Data
@Validated
public class ImUserCreateCO {

    @NotNull
    private Long roleId;
    @NotNull
    private List<ImUserDO> imUsers;

}
