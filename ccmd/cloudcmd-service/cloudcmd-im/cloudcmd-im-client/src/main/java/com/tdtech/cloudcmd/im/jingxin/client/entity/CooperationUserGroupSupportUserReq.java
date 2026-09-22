package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CooperationUserGroupSupportUserReq {

    @NotNull
    private Long groupId;//协同岗群组ID
    @NotNull
    private Long cooperationUserId;//协同岗用户ID
    @NotNull
    private Long supportUserId;//协同岗群组支撑用户ID
    @NotNull
    private Integer opertype;//0-支撑1-解除支撑
    @NotNull
    private Long operTime;//操作时间（支撑或解除时间）
    @NotNull
    private String prompt;
    @NotNull
    private Integer isShowPrompt;
}
